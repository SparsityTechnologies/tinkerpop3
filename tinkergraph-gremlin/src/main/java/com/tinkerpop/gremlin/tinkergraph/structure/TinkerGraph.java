package com.tinkerpop.gremlin.tinkergraph.structure;

import com.tinkerpop.gremlin.process.TraversalEngine;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.graph.DefaultGraphTraversal;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Transaction;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import com.tinkerpop.gremlin.tinkergraph.process.graph.step.map.TinkerGraphStep;
import com.tinkerpop.gremlin.tinkergraph.process.graph.strategy.ClearTraverserSourceStrategy;
import com.tinkerpop.gremlin.tinkergraph.process.graph.strategy.TinkerGraphStepTraversalStrategy;
import org.apache.commons.configuration.Configuration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An in-memory, reference implementation of the property graph interfaces provided by Blueprints.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class TinkerGraph implements Graph, Serializable {

    protected Long currentId = -1l;
    protected Map<Object, Vertex> vertices = new HashMap<>();
    protected Map<Object, Edge> edges = new HashMap<>();
    protected TinkerGraphVariables variables = new TinkerGraphVariables();
    protected TinkerGraphView graphView = null;
    protected boolean useGraphView = false;

    protected TinkerIndex<TinkerVertex> vertexIndex = new TinkerIndex<>(this, TinkerVertex.class);
    protected TinkerIndex<TinkerEdge> edgeIndex = new TinkerIndex<>(this, TinkerEdge.class);

    /**
     * An empty private constructor that initializes {@link TinkerGraph} with no {@link com.tinkerpop.gremlin.structure.strategy.GraphStrategy}.  Primarily
     * used for purposes of serialization issues.
     */
    private TinkerGraph() {
    }

    /**
     * Open a new {@link TinkerGraph} instance.
     * <p/>
     * <b>Reference Implementation Help:</b> If a {@link com.tinkerpop.gremlin.structure.Graph } implementation does not require a
     * {@link org.apache.commons.configuration.Configuration} (or perhaps has a default configuration) it can choose to implement a zero argument
     * open() method. This is an optional constructor method for TinkerGraph. It is not enforced by the Blueprints
     * Test Suite.
     */
    public static TinkerGraph open() {
        return open(null);
    }

    /**
     * Open a new {@link TinkerGraph} instance.
     * <p/>
     * <b>Reference Implementation Help:</b> This method is the one use by the
     * {@link com.tinkerpop.gremlin.structure.util.GraphFactory} to instantiate
     * {@link com.tinkerpop.gremlin.structure.Graph} instances.  This method must be overridden for the Blueprint Test
     * Suite to pass. Implementers have latitude in terms of how exceptions are handled within this method.  Such
     * exceptions will be considered implementation specific by the test suite as all test generate graph instances
     * by way of {@link com.tinkerpop.gremlin.structure.util.GraphFactory}. As such, the exceptions get generalized
     * behind that facade and since {@link com.tinkerpop.gremlin.structure.util.GraphFactory} is the preferred method
     * to opening graphs it will be consistent at that level.
     *
     * @param configuration the configuration for the instance
     * @param <G>           the {@link com.tinkerpop.gremlin.structure.Graph} instance
     * @return a newly opened {@link com.tinkerpop.gremlin.structure.Graph}
     */
    public static <G extends Graph> G open(final Configuration configuration) {
        return (G) new TinkerGraph();
    }

    ////////////// STRUCTURE API METHODS //////////////////

    public Vertex v(final Object id) {
        if (null == id) throw Graph.Exceptions.elementNotFound();
        final Vertex vertex = this.vertices.get(id);
        if (null == vertex)
            throw Graph.Exceptions.elementNotFound();
        else
            return vertex;
    }

    public Edge e(final Object id) {
        if (null == id) throw Graph.Exceptions.elementNotFound();
        final Edge edge = this.edges.get(id);
        if (null == edge)
            throw Graph.Exceptions.elementNotFound();
        else
            return edge;
    }

    public GraphTraversal<Vertex, Vertex> V() {
        final GraphTraversal traversal = new DefaultGraphTraversal<Object, Vertex>() {
            public GraphTraversal<Object, Vertex> submit(final TraversalEngine engine) {
                if (engine instanceof GraphComputer) {
                    this.strategies().unregister(TinkerGraphStepTraversalStrategy.class);
                    //TODO: this.strategies().register(new ClearTraverserSourceStrategy());
                }
                return super.submit(engine);
            }
        };
        traversal.memory().set(Property.hidden("g"), this);    // TODO: is this good?
        traversal.strategies().register(new TinkerGraphStepTraversalStrategy());
        traversal.addStep(new TinkerGraphStep(traversal, Vertex.class, this));
        return traversal;
    }

    public GraphTraversal<Edge, Edge> E() {
        final GraphTraversal traversal = new DefaultGraphTraversal<Object, Edge>() {
            public GraphTraversal<Object, Edge> submit(final TraversalEngine engine) {
                if (engine instanceof GraphComputer) {
                    this.strategies().unregister(TinkerGraphStepTraversalStrategy.class);
                    // TODO: this.strategies().register(new ClearTraverserSourceStrategy());
                }
                return super.submit(engine);
            }
        };
        traversal.strategies().register(new TinkerGraphStepTraversalStrategy());
        traversal.addStep(new TinkerGraphStep(traversal, Edge.class, this));
        return traversal;
    }

    public Vertex addVertex(final Object... keyValues) {
        ElementHelper.legalPropertyKeyValueArray(keyValues);
        Object idValue = ElementHelper.getIdValue(keyValues).orElse(null);
        final String label = ElementHelper.getLabelValue(keyValues).orElse(Vertex.DEFAULT_LABEL);

        if (null != idValue) {
            if (this.vertices.containsKey(idValue))
                throw Exceptions.vertexWithIdAlreadyExists(idValue);
        } else {
            idValue = TinkerHelper.getNextId(this);
        }

        final Vertex vertex = new TinkerVertex(idValue, label, this);
        this.vertices.put(vertex.id(), vertex);
        ElementHelper.attachProperties(vertex, keyValues);
        return vertex;
    }

    public <C extends GraphComputer> C compute(final Class<C>... graphComputerClass) {
        if (graphComputerClass.length > 1)
            throw Graph.Exceptions.onlyOneOrNoGraphComputerClass();

        if (graphComputerClass.length == 0) {
            return (C) new TinkerGraphComputer(this);
        } else {
            // TODO: non-default implementation call
            return (C) new TinkerGraphComputer(this);
        }
    }


    public <V extends Variables> V variables() {
        return (V) this.variables;
    }

    public String toString() {
        return StringFactory.graphString(this, "vertices:" + this.vertices.size() + " edges:" + this.edges.size());
    }

    public void clear() {
        this.vertices.clear();
        this.edges.clear();
        this.variables = new TinkerGraphVariables();
        this.currentId = 0l;
        this.vertexIndex = new TinkerIndex<>(this, TinkerVertex.class);
        this.edgeIndex = new TinkerIndex<>(this, TinkerEdge.class);
    }

    public void close() {

    }

    public Transaction tx() {
        throw Exceptions.transactionsNotSupported();
    }


    public Features getFeatures() {
        return new TinkerGraphFeatures();
    }

    public static class TinkerGraphFeatures implements Features {
        @Override
        public GraphFeatures graph() {
            return new TinkerGraphGraphFeatures();
        }

        @Override
        public String toString() {
            return StringFactory.featureString(this);
        }
    }

    public static class TinkerGraphGraphFeatures implements Features.GraphFeatures {
        @Override
        public boolean supportsTransactions() {
            return false;
        }

        @Override
        public boolean supportsPersistence() {
            return false;
        }

        @Override
        public boolean supportsThreadedTransactions() {
            return false;
        }
    }

    ///////////// GRAPH SPECIFIC INDEXING METHODS ///////////////

    public <E extends Element> void createIndex(final String key, final Class<E> elementClass) {
        if (Vertex.class.isAssignableFrom(elementClass)) {
            this.vertexIndex.createKeyIndex(key);
        } else if (Edge.class.isAssignableFrom(elementClass)) {
            this.edgeIndex.createKeyIndex(key);
        } else {
            throw new IllegalArgumentException("Class is not indexable: " + elementClass);
        }
    }

    public <E extends Element> void dropIndex(final String key, final Class<E> elementClass) {
        if (Vertex.class.isAssignableFrom(elementClass)) {
            this.vertexIndex.dropKeyIndex(key);
        } else if (Edge.class.isAssignableFrom(elementClass)) {
            this.edgeIndex.dropKeyIndex(key);
        } else {
            throw new IllegalArgumentException("Class is not indexable: " + elementClass);
        }
    }

    public <E extends Element> Set<String> getIndexedKeys(final Class<E> elementClass) {
        if (Vertex.class.isAssignableFrom(elementClass)) {
            return this.vertexIndex.getIndexedKeys();
        } else if (Edge.class.isAssignableFrom(elementClass)) {
            return this.edgeIndex.getIndexedKeys();
        } else {
            throw new IllegalArgumentException("Class is not indexable: " + elementClass);
        }
    }
}
