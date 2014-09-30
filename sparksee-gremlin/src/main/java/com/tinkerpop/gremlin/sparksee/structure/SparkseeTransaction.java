package com.tinkerpop.gremlin.sparksee.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Transaction;

/**
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
public class SparkseeTransaction implements Transaction {
    
    private Consumer<Transaction> readWriteConsumer;
    private Consumer<Transaction> closeConsumer;
    private SparkseeGraph graph;
    private com.sparsity.sparksee.gdb.Database db = null;
    private ConcurrentHashMap<Long, Metadata> threadData = new ConcurrentHashMap<Long, Metadata>();

    private class Metadata {
        com.sparsity.sparksee.gdb.Session session = null;
        List<com.sparsity.sparksee.gdb.Objects> collection = new ArrayList<com.sparsity.sparksee.gdb.Objects>();
    };
    
    
    protected SparkseeTransaction(SparkseeGraph graph, com.sparsity.sparksee.gdb.Database db) {
        this.graph = graph;
        this.db = db;
        readWriteConsumer = READ_WRITE_BEHAVIOR.AUTO;
        closeConsumer = CLOSE_BEHAVIOR.COMMIT;
    }
    
    /**
     * Gets the Sparksee Session
     *
     * @return The Sparksee Session
     */
    protected com.sparsity.sparksee.gdb.Session getRawSession(boolean exception) {
        Long threadId = Thread.currentThread().getId();
        if (!threadData.containsKey(threadId)) {
            if (exception) {
                throw new IllegalStateException("Transaction has not been started");
            }
            return null;
        }
        return threadData.get(threadId).session;
    }
    
    /**
     * Gets the Sparksee raw graph.
     *
     * @return Sparksee raw graph.
     */
    protected com.sparsity.sparksee.gdb.Graph getRawGraph() {
        com.sparsity.sparksee.gdb.Session sess = getRawSession(false);
        if (sess == null) {
            throw new IllegalStateException("Transaction has not been started");
        }
        return sess.getGraph();
    }
    
    protected void closeAll() {
        for (Metadata md : threadData.values()) {
            for (com.sparsity.sparksee.gdb.Objects objs : md.collection) {
                objs.close();
            }
            md.session.close();
        }
        threadData.clear();
    }
    
    protected void add(com.sparsity.sparksee.gdb.Objects objs) {
        if (!isOpen()) {
            return;
        }
        Long threadId = Thread.currentThread().getId();
        Metadata metadata = threadData.get(threadId);
        metadata.collection.add(objs);
    }
    
    protected void remove(com.sparsity.sparksee.gdb.Objects objs) {
        if (!isOpen()) {
            return;
        }
        Long threadId = Thread.currentThread().getId();
        Metadata metadata = threadData.get(threadId);
        metadata.collection.remove(objs);
    }
    
    @Override
    public void open() {
        if (isOpen()) {
            throw Transaction.Exceptions.transactionAlreadyOpen();
        }
        
        Long threadId = Thread.currentThread().getId();
        Metadata metadata = new Metadata();
        metadata.session = db.newSession();
        threadData.put(threadId, metadata);
        threadData.get(threadId).session.beginUpdate();
    }
    
    @Override
    public void commit() {
        if (!isOpen()) {
            return;
        }
        
        Long threadId = Thread.currentThread().getId();
        Metadata metadata = threadData.get(threadId);
        metadata.session.commit();
        for (com.sparsity.sparksee.gdb.Objects objs : metadata.collection) {
            objs.close();
        }
        metadata.session.close();
        threadData.remove(threadId);
    }
    
    @Override
    public void rollback() {
        if (!isOpen()) {
            return;
        }

        Long threadId = Thread.currentThread().getId();
        Metadata metadata = threadData.get(threadId);
        metadata.session.rollback();
        for (com.sparsity.sparksee.gdb.Objects objs : metadata.collection) {
            objs.close();
        }
        metadata.session.close();
        threadData.remove(threadId);
    }

    @Override
    public <R> Workload<R> submit(final Function<Graph, R> work) {
        return new Workload<>(graph, work);
    }
    
    @Override
    public <G extends Graph> G create() {
        throw Transaction.Exceptions.threadedTransactionsNotSupported();
    }
    
    @Override
    public boolean isOpen() {
        Long threadId = Thread.currentThread().getId();
        return (threadData.containsKey(threadId));
    }
    
    @Override
    public void readWrite() {
        this.readWriteConsumer.accept(this);
    }
    
    @Override
    public void close() {
        this.closeConsumer.accept(this);
    }

    @Override
    public Transaction onReadWrite(final Consumer<Transaction> consumer) {
        this.readWriteConsumer = Optional.ofNullable(consumer).orElseThrow(Transaction.Exceptions::onReadWriteBehaviorCannotBeNull);
        return this;
    }
    
    @Override
    public Transaction onClose(final Consumer<Transaction> consumer) {
        this.closeConsumer = Optional.ofNullable(consumer).orElseThrow(Transaction.Exceptions::onCloseBehaviorCannotBeNull);
        return this;
    }
}
