package com.tinkerpop.gremlin.tinkergraph.structure;

import com.tinkerpop.gremlin.process.util.MultiIterator;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TinkerHelper {

    protected static long getNextId(final TinkerGraph graph) {
        return Stream.generate(() -> (++graph.currentId)).filter(id -> !graph.vertices.containsKey(id) && !graph.edges.containsKey(id)).findFirst().get();
    }

    protected static Edge addEdge(final TinkerGraph graph, final TinkerVertex outVertex, final TinkerVertex inVertex, final String label, final Object... keyValues) {
        if (null == label)
            throw Edge.Exceptions.edgeLabelCanNotBeNull();
        ElementHelper.legalPropertyKeyValueArray(keyValues);

        Object idValue = ElementHelper.getIdValue(keyValues).orElse(null);

        final Edge edge;
        if (null != idValue) {
            if (graph.edges.containsKey(idValue))
                throw Graph.Exceptions.edgeWithIdAlreadyExist(idValue);
        } else {
            idValue = TinkerHelper.getNextId(graph);
        }

        edge = new TinkerEdge(idValue, outVertex, label, inVertex, graph);
        ElementHelper.attachProperties(edge, keyValues);
        graph.edges.put(edge.id(), edge);
        TinkerHelper.addOutEdge(outVertex, label, edge);
        TinkerHelper.addInEdge(inVertex, label, edge);
        return edge;

    }

    protected static void addOutEdge(final TinkerVertex vertex, final String label, final Edge edge) {
        Set<Edge> edges = vertex.outEdges.get(label);
        if (null == edges) {
            edges = new HashSet<>();
            vertex.outEdges.put(label, edges);
        }
        edges.add(edge);
    }

    protected static void addInEdge(final TinkerVertex vertex, final String label, final Edge edge) {
        Set<Edge> edges = vertex.inEdges.get(label);
        if (null == edges) {
            edges = new HashSet<>();
            vertex.inEdges.put(label, edges);
        }
        edges.add(edge);
    }

    public static void dropView(final TinkerGraph graph) {
        graph.useGraphView = false;
        graph.graphView = null;
    }

    public static Collection<Vertex> getVertices(final TinkerGraph graph) {
        return graph.vertices.values();
    }

    public static Collection<Edge> getEdges(final TinkerGraph graph) {
        return graph.edges.values();
    }

    public static List<TinkerVertex> queryVertexIndex(final TinkerGraph graph, final String key, final Object value) {
        return graph.vertexIndex.get(key, value);
    }

    public static List<TinkerEdge> queryEdgeIndex(final TinkerGraph graph, final String key, final Object value) {
        return graph.edgeIndex.get(key, value);
    }

    public static Iterator<TinkerEdge> getEdges(final TinkerVertex vertex, final Direction direction, final String... labels) {
        final MultiIterator<Edge> edges = new MultiIterator<>();
        if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH)) {
            if (labels.length > 0) {
                for (final String label : labels) {
                    edges.addIterator(vertex.outEdges.getOrDefault(label, Collections.emptySet()).iterator());
                }
            } else {
                for (final Set<Edge> set : vertex.outEdges.values()) {
                    edges.addIterator(set.iterator());
                }
            }
        }
        if (direction.equals(Direction.IN) || direction.equals(Direction.BOTH)) {
            if (labels.length > 0) {
                for (final String label : labels) {
                    edges.addIterator(vertex.inEdges.getOrDefault(label, Collections.emptySet()).iterator());
                }
            } else {
                for (final Set<Edge> set : vertex.inEdges.values()) {
                    edges.addIterator(set.iterator());
                }
            }
        }
        return (Iterator) edges;
    }

    public static Iterator<TinkerVertex> getVertices(final TinkerVertex vertex, final Direction direction, final String... labels) {
        if (direction != Direction.BOTH) {
            if (direction.equals(Direction.OUT))
                return (Iterator) StreamFactory.stream(TinkerHelper.getEdges(vertex, direction, labels)).map(e -> e.inV().next()).iterator();
            else
                return (Iterator) StreamFactory.stream(TinkerHelper.getEdges(vertex, direction, labels)).map(e -> e.outV().next()).iterator();

        } else {
            final MultiIterator<TinkerVertex> vertices = new MultiIterator<>();
            vertices.addIterator((Iterator) StreamFactory.stream(TinkerHelper.getEdges(vertex, Direction.OUT, labels)).map(e -> e.inV().next()).iterator());
            vertices.addIterator((Iterator) StreamFactory.stream(TinkerHelper.getEdges(vertex, Direction.IN, labels)).map(e -> e.outV().next()).iterator());
            return vertices;
        }
    }

    public static Iterator<TinkerVertex> getVertices(final TinkerEdge edge, final Direction direction) {
        final List<TinkerVertex> vertices = new ArrayList<>();
        if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH))
            vertices.add((TinkerVertex) edge.outVertex);
        if (direction.equals(Direction.IN) | direction.equals(Direction.BOTH))
            vertices.add((TinkerVertex) edge.inVertex);
        return vertices.iterator();
    }
}
