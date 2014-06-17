package com.tinkerpop.gremlin.structure.strategy;

import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.util.function.SConsumer;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class StrategyWrappedVertex extends StrategyWrappedElement implements Vertex, StrategyWrapped {
    private final Vertex baseVertex;
    private final Strategy.Context<StrategyWrappedVertex> strategyContext;

    public StrategyWrappedVertex(final Vertex baseVertex, final StrategyWrappedGraph strategyWrappedGraph) {
        super(baseVertex, strategyWrappedGraph);
        this.strategyContext = new Strategy.Context<>(strategyWrappedGraph.getBaseGraph(), this);
        this.baseVertex = baseVertex;
    }

    public Vertex getBaseVertex() {
        return this.baseVertex;
    }

    @Override
    public Edge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
        final Vertex baseInVertex = (inVertex instanceof StrategyWrappedVertex) ? ((StrategyWrappedVertex) inVertex).getBaseVertex() : inVertex;
        return new StrategyWrappedEdge(this.strategyWrappedGraph.strategy().compose(
                s -> s.getAddEdgeStrategy(strategyContext),
                this.baseVertex::addEdge)
                .apply(label, baseInVertex, keyValues), this.strategyWrappedGraph);
    }

    @Override
    public GraphTraversal<Vertex, Vertex> out(final int branchFactor, final String... labels) {
        return applyStrategy(this.baseVertex.out(branchFactor, labels));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> in(final int branchFactor, final String... labels) {
        return applyStrategy(this.baseVertex.in(branchFactor, labels));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> both(final int branchFactor, final String... labels) {
        return applyStrategy(this.baseVertex.both(branchFactor, labels));
    }

    @Override
    public GraphTraversal<Vertex, Edge> outE(final int branchFactor, final String... labels) {
        return applyStrategy(this.baseVertex.outE(branchFactor, labels));
    }

    @Override
    public GraphTraversal<Vertex, Edge> inE(final int branchFactor, final String... labels) {
        return applyStrategy(this.baseVertex.inE(branchFactor, labels));
    }

    @Override
    public GraphTraversal<Vertex, Edge> bothE(final int branchFactor, final String... labels) {
        return applyStrategy(this.baseVertex.bothE(branchFactor, labels));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> out(final String... labels) {
        return applyStrategy(this.baseVertex.out(labels));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> in(final String... labels) {
        return applyStrategy(this.baseVertex.in(labels));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> both(final String... labels) {
        return this.baseVertex.both(labels);
    }

    @Override
    public GraphTraversal<Vertex, Edge> outE(final String... labels) {
        return applyStrategy(this.baseVertex.outE(labels));
    }

    @Override
    public GraphTraversal<Vertex, Edge> inE(final String... labels) {
        return applyStrategy(this.baseVertex.inE(labels));
    }

    @Override
    public GraphTraversal<Vertex, Edge> bothE(final String... labels) {
        return applyStrategy(this.baseVertex.bothE(labels));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> start() {
        return applyStrategy(this.baseVertex.start());
    }

    @Override
    public GraphTraversal<Vertex, Vertex> as(final String as) {
        return applyStrategy(this.baseVertex.as(as));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> with(final Object... variableValues) {
        return applyStrategy(this.baseVertex.with(variableValues));
    }

    @Override
    public GraphTraversal<Vertex, Vertex> sideEffect(final SConsumer<Traverser<Vertex>> consumer) {
        return applyStrategy(this.baseVertex.sideEffect(consumer));
    }
}
