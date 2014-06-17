package com.tinkerpop.gremlin.server;

import com.tinkerpop.gremlin.driver.MessageSerializer;
import com.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import com.tinkerpop.gremlin.server.handler.GremlinBinaryRequestDecoder;
import com.tinkerpop.gremlin.server.handler.GremlinTextRequestDecoder;
import com.tinkerpop.gremlin.server.handler.GremlinResponseEncoder;
import com.tinkerpop.gremlin.server.handler.IteratorHandler;
import com.tinkerpop.gremlin.server.handler.OpExecutorHandler;
import com.tinkerpop.gremlin.server.handler.OpSelectorHandler;
import com.tinkerpop.gremlin.server.util.MetricManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

/**
 * Start and stop Gremlin Server.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GremlinServer {
    private static final Logger logger = LoggerFactory.getLogger(GremlinServer.class);
    private final Settings settings;
    private Optional<Graphs> graphs = Optional.empty();
    private Channel ch;

    private final Optional<CompletableFuture<Void>> serverReady;

    public GremlinServer(final Settings settings) {
        this(settings, null);
    }

    public GremlinServer(final Settings settings, final CompletableFuture<Void> serverReady) {
        this.serverReady = Optional.ofNullable(serverReady);
        this.settings = settings;
    }

    /**
     * Start Gremlin Server with {@link Settings} provided to the constructor.
     */
    public void run() throws Exception {
        final EventLoopGroup bossGroup = new NioEventLoopGroup(settings.threadPoolBoss);
        final EventLoopGroup workerGroup = new NioEventLoopGroup(settings.threadPoolWorker);
        try {
            final ServerBootstrap b = new ServerBootstrap();

            // when high value is reached then the channel becomes non-writeable and stays like that until the
            // low value is so that there is time to recover
            b.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, settings.writeBufferLowWaterMark);
            b.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, settings.writeBufferHighWaterMark);
            b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketServerInitializer(this.settings, workerGroup));

            ch = b.bind(settings.host, settings.port).sync().channel();
            logger.info("Gremlin Server configured with worker thread pool of {} and boss thread pool of {}",
                    settings.threadPoolWorker, settings.threadPoolBoss);
            logger.info("Websocket channel started at port {}.", settings.port);

            serverReady.ifPresent(future -> future.complete(null));

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Stop Gremlin Server and free the port.
     */
    public void stop() {
        ch.close();
    }

    public static void main(final String[] args) throws Exception {
        // add to vm options: -Dlog4j.configuration=file:config/log4j.properties
        printHeader();
        final String file;
        if (args.length > 0)
            file = args[0];
        else
            file = "config/gremlin-server.yaml";

        final Settings settings;
        try {
            settings = Settings.read(file);
        } catch (Exception ex) {
            logger.error("Configuration file at {} could not be found or parsed properly. [{}]", file, ex.getMessage());
            return;
        }

        logger.info("Configuring Gremlin Server from {}", file);
        settings.optionalMetrics().ifPresent(GremlinServer::configureMetrics);
        new GremlinServer(settings).run();
    }

    public static String getHeader() {
        final StringBuilder builder = new StringBuilder();
        builder.append("\r\n");
        builder.append("         \\,,,/\r\n");
        builder.append("         (o o)\r\n");
        builder.append("-----oOOo-(_)-oOOo-----\r\n");
        return builder.toString();
    }

    private static void configureMetrics(final Settings.ServerMetrics settings) {
        final MetricManager metrics = MetricManager.INSTANCE;
        settings.optionalConsoleReporter().ifPresent(config -> metrics.addConsoleReporter(config.interval));
        settings.optionalCsvReporter().ifPresent(config -> metrics.addCsvReporter(config.interval, config.fileName));
        settings.optionalJmxReporter().ifPresent(config -> metrics.addJmxReporter(config.domain, config.agentId));
        settings.optionalSlf4jReporter().ifPresent(config -> metrics.addSlf4jReporter(config.interval, config.loggerName));
        settings.optionalGangliaReporter().ifPresent(config -> {
            try {
                metrics.addGangliaReporter(config.host, config.port,
                        config.optionalAddressingMode(), config.ttl, config.protocol31, config.hostUUID, config.spoof, config.interval);
            } catch (IOException ioe) {
                logger.warn("Error configuring the Ganglia Reporter.", ioe);
            }
        });
        settings.optionalGraphiteReporter().ifPresent(config -> metrics.addGraphiteReporter(config.host, config.port, config.prefix, config.interval));
    }

    private static void printHeader() {
        logger.info(getHeader());
    }

    private class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
        private final Settings settings;
        private final GremlinExecutor gremlinExecutor;
        private final Optional<SSLEngine> sslEngine;

        private final Map<String, MessageSerializer> serializers = new HashMap<>();

        final EventExecutorGroup gremlinGroup;

        public WebSocketServerInitializer(final Settings settings, final ScheduledExecutorService scheduledExecutorService) {
            this.settings = settings;

            // instantiate and configure the serializers that gremlin server will use - could error out here
            // and fail the server startup
            configureSerializers();

            // initialize graphs from configuration
            if (!graphs.isPresent()) graphs = Optional.of(new Graphs(settings));

            final BasicThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern("gremlin-%d").build();
            this.gremlinGroup = new DefaultEventExecutorGroup(settings.gremlinPool, threadFactory);

			logger.info("Initialized Gremlin thread pool.  Threads in pool named with pattern gremlin-*");

			final GremlinExecutor.Builder gremlinExecutorBuilder = GremlinExecutor.create()
					.scriptEvaluationTimeout(settings.scriptEvaluationTimeout)
					.afterFailure((b,e) -> graphs.get().rollbackAll())
					.afterSuccess(b -> graphs.get().commitAll())
					.beforeEval(b -> graphs.get().rollbackAll())
					.afterTimeout(b -> graphs.get().rollbackAll())
					.use(settings.use)
					.globalBindings(graphs.get().getGraphsAsBindings())
					.executorService(gremlinGroup)
					.scheduledExecutorService(scheduledExecutorService);

			settings.scriptEngines.forEach((k,v) -> gremlinExecutorBuilder.addEngineSettings(k, v.imports, v.staticImports, v.scripts));
			this.gremlinExecutor = gremlinExecutorBuilder.build();

			logger.info("Initialized GremlinExecutor and configured ScriptEngines.");

			this.sslEngine = settings.optionalSsl().isPresent() && settings.ssl.enabled ? Optional.ofNullable(createSslEngine()) : Optional.empty();
        }

        @Override
        public void initChannel(final SocketChannel ch) throws Exception {
            final ChannelPipeline pipeline = ch.pipeline();

            sslEngine.ifPresent(ssl -> pipeline.addLast("ssl", new SslHandler(ssl)));

            if (logger.isDebugEnabled())
                pipeline.addLast(new LoggingHandler("log-io", LogLevel.DEBUG));

            logger.debug("HttpRequestDecoder settings - maxInitialLineLength={}, maxHeaderSize={}, maxChunkSize={}",
                    settings.maxInitialLineLength, settings.maxHeaderSize, settings.maxChunkSize);
            pipeline.addLast("http-request-decoder", new HttpRequestDecoder(settings.maxInitialLineLength, settings.maxHeaderSize, settings.maxChunkSize));

            if (logger.isDebugEnabled())
                pipeline.addLast(new LoggingHandler("log-decoder-aggregator", LogLevel.DEBUG));

            logger.debug("HttpObjectAggregator settings - maxContentLength={}, maxAccumulationBufferComponents={}",
                    settings.maxContentLength, settings.maxAccumulationBufferComponents);
            final HttpObjectAggregator aggregator = new HttpObjectAggregator(settings.maxContentLength);
            aggregator.setMaxCumulationBufferComponents(settings.maxAccumulationBufferComponents);
            pipeline.addLast("aggregator", aggregator);

            if (logger.isDebugEnabled())
                pipeline.addLast(new LoggingHandler("log-aggregator-encoder", LogLevel.DEBUG));

            pipeline.addLast("http-response-encoder", new HttpResponseEncoder());
            pipeline.addLast("request-handler", new WebSocketServerProtocolHandler("/gremlin"));

            if (logger.isDebugEnabled())
                pipeline.addLast(new LoggingHandler("log-aggregator-encoder", LogLevel.DEBUG));

            pipeline.addLast("response-encoder", new GremlinResponseEncoder());
            pipeline.addLast("request-text-decoder", new GremlinTextRequestDecoder());
            pipeline.addLast("request-binary-decoder", new GremlinBinaryRequestDecoder(serializers));

            if (logger.isDebugEnabled())
                pipeline.addLast(new LoggingHandler("log-aggregator-encoder", LogLevel.DEBUG));

            pipeline.addLast("op-selector", new OpSelectorHandler(settings, graphs.get(), gremlinExecutor));

            pipeline.addLast(gremlinGroup, "result-iterator-handler", new IteratorHandler(settings));
            pipeline.addLast(gremlinGroup, "op-executor", new OpExecutorHandler(settings, graphs.get(), gremlinExecutor));
        }

        private void configureSerializers() {
            this.settings.serializers.stream().map(config -> {
                try {
                    final Class clazz = Class.forName(config.className);
                    if (!MessageSerializer.class.isAssignableFrom(clazz)) {
                        logger.warn("The {} serialization class does not implement {} - it will not be available.", config.className, MessageSerializer.class.getCanonicalName());
                        return Optional.<MessageSerializer>empty();
                    }

                    final MessageSerializer serializer = (MessageSerializer) clazz.newInstance();
                    if (config.config != null)
                        serializer.configure(config.config);

                    return Optional.ofNullable(serializer);
                } catch (ClassNotFoundException cnfe) {
                    logger.warn("Could not find configured serializer class - {} - it will not be available", config.className);
                    return Optional.<MessageSerializer>empty();
                }  catch (Exception ex) {
                    logger.warn("Cound not instantiate configured serializer class - {} - it will not be available.", config.className);
                    return Optional.<MessageSerializer>empty();
                }
            }).filter(Optional::isPresent).map(o->o.get()).flatMap(serializer ->
                    Stream.of(serializer.mimeTypesSupported()).map(mimeType -> Pair.with(mimeType, serializer))
            ).forEach(pair -> {
                logger.info("Configured {} with {}", pair.getValue0(), pair.getValue1().getClass().getName());
                serializers.put(pair.getValue0().toString(), pair.<MessageSerializer>getValue1());
            });

            if (serializers.size() == 0) {
                logger.error("No serializers were successfully configured - server will not start.");
                throw new RuntimeException("Serialization configuration error.");
            }
        }

        private SSLEngine createSslEngine() {
            try {
                logger.info("SSL was enabled.  Initializing SSLEngine instance...");
                final SSLEngine engine = createSSLContext(settings).createSSLEngine();
                engine.setUseClientMode(false);
                logger.info("SSLEngine was properly configured and initialized.");
                return engine;
            } catch (Exception ex) {
                logger.warn("SSL could not be enabled.  Check the ssl section of the configuration file.", ex);
                return null;
            }
        }

        private SSLContext createSSLContext(final Settings settings) throws Exception {
            final Settings.SslSettings sslSettings = settings.ssl;

            TrustManager[] managers = null;
            if (sslSettings.trustStoreFile != null) {
                final KeyStore ts = KeyStore.getInstance(Optional.ofNullable(sslSettings.trustStoreFormat).orElseThrow(() -> new IllegalStateException("The trustStoreFormat is not set")));
                try (final InputStream trustStoreInputStream = new FileInputStream(Optional.ofNullable(sslSettings.trustStoreFile).orElseThrow(() -> new IllegalStateException("The trustStoreFile is not set")))) {
                    ts.load(trustStoreInputStream, sslSettings.trustStorePassword.toCharArray());
                }

                final String trustStoreAlgorithm = Optional.ofNullable(sslSettings.trustStoreAlgorithm).orElse(TrustManagerFactory.getDefaultAlgorithm());
                final TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustStoreAlgorithm);
                tmf.init(ts);
                managers = tmf.getTrustManagers();
            }

            final KeyStore ks = KeyStore.getInstance(Optional.ofNullable(sslSettings.keyStoreFormat).orElseThrow(() -> new IllegalStateException("The keyStoreFormat is not set")));
            try (final InputStream keyStoreInputStream = new FileInputStream(Optional.ofNullable(sslSettings.keyStoreFile).orElseThrow(() -> new IllegalStateException("The keyStoreFile is not set")))) {
                ks.load(keyStoreInputStream, sslSettings.keyStorePassword.toCharArray());
            }

            final String keyManagerAlgorithm = Optional.ofNullable(sslSettings.keyManagerAlgorithm).orElse(KeyManagerFactory.getDefaultAlgorithm());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);
            kmf.init(ks, Optional.ofNullable(sslSettings.keyManagerPassword).orElseThrow(() -> new IllegalStateException("The keyManagerPassword is not set")).toCharArray());

            final SSLContext serverContext = SSLContext.getInstance("TLS");
            serverContext.init(kmf.getKeyManagers(), managers, null);
            return serverContext;
        }
    }
}
