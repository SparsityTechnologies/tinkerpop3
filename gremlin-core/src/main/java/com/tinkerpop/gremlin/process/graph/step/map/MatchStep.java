package com.tinkerpop.gremlin.process.graph.step.map;

import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.util.AbstractStep;
import com.tinkerpop.gremlin.process.graph.marker.PathConsumer;
import com.tinkerpop.gremlin.process.util.SingleIterator;
import com.tinkerpop.gremlin.process.util.TraversalHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class MatchStep<S, E> extends AbstractStep<S, E> implements PathConsumer {

    private final Map<String, List<Traversal>> predicateTraversals = new HashMap<>();
    private final Map<String, List<Traversal>> internalTraversals = new HashMap<>();
    private Traversal endTraversal = null;
    private String endTraversalStartAs;
    private final String inAs;
    private final String outAs;

    public MatchStep(final Traversal traversal, final String inAs, final String outAs, final Traversal... traversals) {
        super(traversal);
        this.inAs = inAs;
        this.outAs = outAs;
        for (final Traversal tl : traversals) {
            final String start = TraversalHelper.getStart(tl).getAs();
            final String end = TraversalHelper.getEnd(tl).getAs();
            if (!TraversalHelper.isLabeled(start)) {
                throw new IllegalArgumentException("All match traversals must have their start pipe labeled");
            }
            if (!TraversalHelper.isLabeled(end)) {
                final List<Traversal> list = this.predicateTraversals.getOrDefault(start, new ArrayList<>());
                this.predicateTraversals.put(start, list);
                list.add(tl);
            } else {
                if (end.equals(this.outAs)) {
                    if (null != this.endTraversal)
                        throw new IllegalArgumentException("There can only be one outAs labeled end traversal");
                    this.endTraversal = tl;
                    this.endTraversalStartAs = TraversalHelper.getStart(tl).getAs();
                } else {
                    final List<Traversal> list = this.internalTraversals.getOrDefault(start, new ArrayList<>());
                    this.internalTraversals.put(start, list);
                    list.add(tl);
                }
            }
        }
        if (null == this.endTraversal) {
            throw new IllegalStateException("One of the match traversals must be an end traversal");
        }
    }

    protected Traverser<E> processNextStart() {
        while (true) {
            if (this.endTraversal.hasNext()) {
                final Traverser<E> traverser = (Traverser<E>) TraversalHelper.getEnd(this.endTraversal).next();
                if (doPredicates(this.outAs, traverser)) {
                    return traverser;
                }
            } else {
                final Traverser temp = this.starts.next();
                temp.getPath().renameLastStep(this.inAs); // TODO: is this cool? this is the only place path is needed! (make this not a PathConsumer)
                doMatch(this.inAs, temp);
            }
        }
    }

    private void doMatch(final String as, final Traverser traverser) {
        if (!doPredicates(as, traverser))
            return;

        if (as.equals(this.endTraversalStartAs)) {
            this.endTraversal.addStarts(new SingleIterator<>(traverser));
            return;
        }

        for (final Traversal traversal : this.internalTraversals.get(as)) {
            traversal.addStarts(new SingleIterator<>(traverser));
            final Step<?, ?> endStep = TraversalHelper.getEnd(traversal);
            while (endStep.hasNext()) {
                final Traverser temp = endStep.next();
                doMatch(endStep.getAs(), temp);
            }
        }
    }

    private boolean doPredicates(final String as, final Traverser traverser) {
        if (this.predicateTraversals.containsKey(as)) {
            for (final Traversal traversal : this.predicateTraversals.get(as)) {
                traversal.addStarts(new SingleIterator<>(traverser));
                if (!TraversalHelper.hasNextIteration(traversal))
                    return false;
            }
        }
        return true;
    }

}
