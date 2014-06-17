package com.tinkerpop.gremlin.process.graph.step.map;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Property;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class PropertyElementStep<E> extends MapStep<Property<E>, Element> {

    public PropertyElementStep(final Traversal traversal) {
        super(traversal);
        this.setFunction(traverser -> traverser.get().getElement());
    }

    // TODO: reverse()
}
