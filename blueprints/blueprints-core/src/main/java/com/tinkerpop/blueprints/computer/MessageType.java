package com.tinkerpop.blueprints.computer;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.query.GraphQuery;
import com.tinkerpop.blueprints.query.util.VertexQueryBuilder;

import java.util.function.BiFunction;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class MessageType {

    private final String label;

    public MessageType(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public static class Global extends MessageType {
        private final GraphQuery query;
        private final Iterable<Vertex> vertices;

        private Global(final String label, final GraphQuery query) {
            super(label);
            this.query = query;
            this.vertices = null;
        }

        private Global(final String label, final Iterable<Vertex> vertices) {
            super(label);
            this.query = null;
            this.vertices = vertices;
        }

        public static Global of(final String label, final GraphQuery query) {
            return new Global(label, query);
        }

        public static Global of(final String label, final Iterable<Vertex> vertices) {
            return new Global(label, vertices);
        }

        public Iterable<Vertex> vertices() {
            return null == this.query ? this.vertices : this.query.vertices();
        }
    }

    public static class Local<M1, M2> extends MessageType {
        public final VertexQueryBuilder query;
        public final BiFunction<M1, Edge, M2> edgeFunction;

        private Local(final String label, final VertexQueryBuilder query) {
            super(label);
            this.query = query;
            this.edgeFunction = (final M1 m, final Edge e) -> (M2) m;
        }

        private Local(final String label, final VertexQueryBuilder query, final BiFunction<M1, Edge, M2> edgeFunction) {
            super(label);
            this.query = query;
            this.edgeFunction = edgeFunction;
        }

        public static Local of(final String label, final VertexQueryBuilder query) {
            return new Local(label, query);
        }

        public static <M1, M2> Local of(final String label, final VertexQueryBuilder query, final BiFunction<M1, Edge, M2> edgeFunction) {
            return new Local<>(label, query, edgeFunction);
        }

        public Iterable<Edge> edges(final Vertex vertex) {
            return this.query.build(vertex).edges();
        }

        public Iterable<Vertex> vertices(final Vertex vertex) {
            return this.query.build(vertex).vertices();
        }

        public BiFunction<M1, Edge, M2> getEdgeFunction() {
            return this.edgeFunction;
        }

        public VertexQueryBuilder getQuery() {
            return this.query;
        }
    }
}