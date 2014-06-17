package com.tinkerpop.gremlin.structure.strategy;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.TraversalStrategy;
import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.graph.step.map.EdgeVertexStep;
import com.tinkerpop.gremlin.process.graph.step.map.GraphStep;
import com.tinkerpop.gremlin.process.graph.step.map.MapStep;
import com.tinkerpop.gremlin.process.graph.step.map.VertexStep;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Force all {@link Vertex}, {@link Edge}, {@link Property}, and {@link Graph} objects in the {@link Traversal} to be wrapped
 * appropriately.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class StrategyWrappedTraversalStrategy implements TraversalStrategy.FinalTraversalStrategy {

    private final StrategyWrappedGraph graph;

    public StrategyWrappedTraversalStrategy(final StrategyWrappedGraph graph) {
        this.graph = graph;
    }

    public void apply(final Traversal traversal) {
        // MapStep after each GraphStep, VertexStep or EdgeVertexStep
        final List<Class> stepsToLookFor = Arrays.<Class>asList(GraphStep.class, VertexStep.class, EdgeVertexStep.class);
        final List<Integer> positions = new ArrayList<>();
        final List<?> traversalSteps = traversal.getSteps();
        for (int ix = 0; ix < traversalSteps.size(); ix++) {
            final int pos = ix;
            if (stepsToLookFor.stream().anyMatch(c -> c.isAssignableFrom(traversalSteps.get(pos).getClass()))) positions.add(ix);
        }

        Collections.reverse(positions);
        for (int pos : positions) {
            final MapStep<Object, Object> transformToStrategy = new MapStep<>(traversal);
            transformToStrategy.setFunction((Traverser<Object> t) -> {
                final Object o = t.get();

                // make sure we're not re-wrapping in strategy over and over again.
                if (o instanceof StrategyWrapped) return o;

                if (o instanceof Vertex)
                    return new StrategyWrappedVertex((Vertex) o, graph);
                else if (o instanceof Edge)
                    return new StrategyWrappedEdge((Edge) o, graph);
                else if (o instanceof Property)
                    return new StrategyWrappedProperty((Property) o, graph);
                else if (o instanceof Graph)
                    return new StrategyWrappedGraph((Graph) o); // todo why would a Graph be in here???
                else
                    return o;
            });

            TraversalHelper.insertStep(transformToStrategy, pos + 1, traversal);
        }
    }
}