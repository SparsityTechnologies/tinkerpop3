package com.tinkerpop.gremlin.process.graph.step.filter;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.util.HasContainer;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class HasStep extends FilterStep<Element> implements Reversible {

    public HasContainer hasContainer;

    public HasStep(final Traversal traversal, final HasContainer hasContainer) {
        super(traversal);
        this.hasContainer = hasContainer;
        this.setPredicate(traverser -> hasContainer.test(traverser.get()));
    }

    public String toString() {
        return TraversalHelper.makeStepString(this, this.hasContainer);
    }
}
