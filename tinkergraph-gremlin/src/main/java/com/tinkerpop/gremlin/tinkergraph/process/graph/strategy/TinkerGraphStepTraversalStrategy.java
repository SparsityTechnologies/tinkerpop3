package com.tinkerpop.gremlin.tinkergraph.process.graph.strategy;

import com.tinkerpop.gremlin.process.TraversalStrategy;
import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.step.filter.HasStep;
import com.tinkerpop.gremlin.process.graph.step.filter.IntervalStep;
import com.tinkerpop.gremlin.process.graph.step.filter.IdentityStep;
import com.tinkerpop.gremlin.process.util.EmptyStep;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.tinkergraph.process.graph.step.map.TinkerGraphStep;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TinkerGraphStepTraversalStrategy implements TraversalStrategy.FinalTraversalStrategy {

    public void apply(final Traversal traversal) {

        if (traversal.getSteps().get(0) instanceof TinkerGraphStep) {  // TODO: generalize for searching the whole traversal?
            final TinkerGraphStep tinkerGraphStep = (TinkerGraphStep) traversal.getSteps().get(0);
            Step currentStep = tinkerGraphStep.getNextStep();
            while (true) {
                if (currentStep == EmptyStep.instance() || TraversalHelper.isLabeled(currentStep)) break;

                if (currentStep instanceof HasStep) {
                    tinkerGraphStep.hasContainers.add(((HasStep) currentStep).hasContainer);
                    TraversalHelper.removeStep(currentStep, traversal);
                } else if (currentStep instanceof IntervalStep) {
                    tinkerGraphStep.hasContainers.add(((IntervalStep) currentStep).startContainer);
                    tinkerGraphStep.hasContainers.add(((IntervalStep) currentStep).endContainer);
                    TraversalHelper.removeStep(currentStep, traversal);
                } else if (currentStep instanceof IdentityStep) {
                    // do nothing
                } else {
                    break;
                }
                currentStep = currentStep.getNextStep();
            }
            tinkerGraphStep.generateTraverserIterator(false);
        }
    }
}
