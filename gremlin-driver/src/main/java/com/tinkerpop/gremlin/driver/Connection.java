package com.tinkerpop.gremlin.driver;

import com.tinkerpop.gremlin.driver.exception.ConnectionException;
import com.tinkerpop.gremlin.driver.message.RequestMessage;
import com.tinkerpop.gremlin.driver.message.ResponseMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A single connection to a Gremlin Server instance.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class Connection {
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

    private final Channel channel;
    private final URI uri;
    private final ConcurrentMap<UUID, ResponseQueue> pending = new ConcurrentHashMap<>();
    private final Cluster cluster;
    private final ConnectionPool pool;

    public static final int MAX_IN_PROCESS = 4;
    public static final int MIN_IN_PROCESS = 1;

    public final AtomicInteger inFlight = new AtomicInteger(0);
    private volatile boolean isDead = false;
    private final int maxInProcess;

    private final AtomicReference<CompletableFuture<Void>> closeFuture = new AtomicReference<>();

    public Connection(final URI uri, final ConnectionPool pool, final Cluster cluster, final int maxInProcess)  {
        this.uri = uri;
        this.cluster = cluster;
        this.pool = pool;
        this.maxInProcess = maxInProcess;

        final Bootstrap b = this.cluster.getFactory().createBootstrap();
        final String protocol = uri.getScheme();
        if (!"ws".equals(protocol))
            throw new IllegalArgumentException("Unsupported protocol: " + protocol);

        final ClientPipelineInitializer initializer = new ClientPipelineInitializer();
        b.channel(NioSocketChannel.class).handler(initializer);

        // todo: blocking
        try {
            channel = b.connect(uri.getHost(), uri.getPort()).sync().channel();
            initializer.handler.handshakeFuture().sync();

            logger.info("Created new connection for {}", uri);
        } catch (InterruptedException ie) {
			logger.debug("Error opening connection on {}", uri);

			throw new RuntimeException(ie);
        }
    }

    /**
     * A connection can only have so many things in process happening on it at once, where "in process" refers to
     * the maximum number of in-process requests less the number of pending responses.
     */
    public int availableInProcess() {
        return maxInProcess - pending.size();
    }

    public boolean isDead() {
        return isDead;
    }

    public boolean isClosed() {
        return closeFuture.get() != null;
    }

    public CompletableFuture<Void> closeAsync() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        if (!closeFuture.compareAndSet(null, future))
            return closeFuture.get();

        // make sure all requests in the queue are fully processed before killing.  if they are then shutdown
        // can be immediate.  if not this method will signal the readCompleted future defined in the write()
        // operation to check if it can close.  in this way the connection no longer receives writes, but
        // can continue to read. If a request never comes back the future won't get fulfilled and the connection
        // will maintain a "pending" request, that won't quite ever go away.  The build up of such a dead requests
        // on a connection in the connection pool will force the pool to replace the connection for a fresh one
        if (pending.isEmpty()) {
            if (null == channel)
                future.complete(null);
            else
                shutdown(future);
        }

        return future;
    }

    public void close() {
        try {
            closeAsync().get();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public ChannelPromise write(final RequestMessage requestMessage, final CompletableFuture<ResultSet> future) {
        // once there is a completed write, then create a traverser for the result set and complete
        // the promise so that the client knows that that it can start checking for results.
        final Connection thisConnection = this;
        final ChannelPromise promise = channel.newPromise()
                .addListener(f -> {
					if (!f.isSuccess()) {
						logger.debug(String.format("Write on connection %s failed", thisConnection), f.cause());
						thisConnection.isDead = true;
						thisConnection.returnToPool();
						future.completeExceptionally(f.cause());
					} else {
						final LinkedBlockingQueue<ResponseMessage> responseQueue = new LinkedBlockingQueue<>();
						final CompletableFuture<Void> readCompleted = new CompletableFuture<>();
						readCompleted.thenAcceptAsync(v -> {
							thisConnection.returnToPool();
							if (isClosed() && pending.isEmpty())
								shutdown(closeFuture.get());
						});
						final ResponseQueue handler = new ResponseQueue(responseQueue, readCompleted);
						pending.put(requestMessage.getRequestId(), handler);
						final ResultSet resultSet = new ResultSet(handler, cluster.executor());
						future.complete(resultSet);
					}
                });
        channel.writeAndFlush(requestMessage, promise);
        return promise;
    }

    public void returnToPool() {
        try {
            if (pool != null) pool.returnConnection(this);
        } catch (ConnectionException ce) {
            logger.debug("Returned {} connection to {} but an error occurred - {}", this, pool, ce.getMessage());
        }
    }

    private void shutdown(final CompletableFuture<Void> future) {
        // todo: await client side close confirmation?
        channel.writeAndFlush(new CloseWebSocketFrame());
        final ChannelPromise promise = channel.newPromise();
        promise.addListener(f -> {
            if (f.cause() != null)
                future.completeExceptionally(f.cause());
            else
                future.complete(null);
        });

        channel.close(promise);
    }

    @Override
    public String toString() {
		return String.format("Connection{isDead=%s, inFlight=%s, pending=%s}", isDead, inFlight, pending.size());
    }

    class ClientPipelineInitializer extends ChannelInitializer<SocketChannel> {

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
        final Handler.WebSocketClientHandler handler;

        public ClientPipelineInitializer() {
            handler = new Handler.WebSocketClientHandler(
                          WebSocketClientHandshakerFactory.newHandshaker(
                              Connection.this.uri, WebSocketVersion.V13, null, false, HttpHeaders.EMPTY_HEADERS, 1280000));
        }

        @Override
        protected void initChannel(final SocketChannel socketChannel) throws Exception {
            final ChannelPipeline pipeline = socketChannel.pipeline();
            pipeline.addLast("http-codec", new HttpClientCodec());
            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
            pipeline.addLast("ws-handler", handler);
            pipeline.addLast("gremlin-encoder", new Handler.GremlinRequestEncoder(true, cluster.getSerializer()));
            pipeline.addLast("gremlin-decoder", new Handler.GremlinResponseDecoder(pending, cluster.getSerializer()));
        }
    }


}
