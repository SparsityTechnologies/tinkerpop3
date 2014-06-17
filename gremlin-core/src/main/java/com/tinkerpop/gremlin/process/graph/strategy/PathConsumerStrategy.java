package com.tinkerpop.gremlin.process.graph.strategy;

import com.tinkerpop.gremlin.process.TraversalStrategy;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.marker.TraverserSource;
import com.tinkerpop.gremlin.process.graph.marker.PathConsumer;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class PathConsumerStrategy implements TraversalStrategy.FinalTraversalStrategy {

    public void apply(final Traversal traversal) {
        final boolean trackPaths = PathConsumerStrategy.trackPaths(traversal);
        traversal.getSteps().forEach(step -> {
            if (step instanceof TraverserSource)
                ((TraverserSource) step).generateTraverserIterator(trackPaths);
        });
    }

    public static <S, E> boolean trackPaths(final Traversal<S, E> traversal) {
        return traversal.getSteps().stream()
                .filter(step -> step instanceof PathConsumer)
                .findFirst()
                .isPresent();
    }

    public static <S, E> void doPathTracking(final Traversal<S, E> traversal) {
        traversal.getSteps().forEach(step -> {
            if (step instanceof TraverserSource)
                ((TraverserSource) step).generateTraverserIterator(true);
        });
    }
}
