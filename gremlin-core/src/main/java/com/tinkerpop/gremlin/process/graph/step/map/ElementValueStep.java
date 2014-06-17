package com.tinkerpop.gremlin.process.graph.step.map;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.util.SOptional;

import java.util.function.Supplier;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ElementValueStep<E> extends MapStep<Element, E> {

    public String key;
    public SOptional<E> defaultValue;
    public SOptional<Supplier<E>> defaultSupplier;

    public ElementValueStep(final Traversal traversal, final String key) {
        super(traversal);
        this.key = key;
        this.defaultValue = SOptional.empty();
        this.defaultSupplier = SOptional.empty();
        this.setFunction(traverser -> traverser.get().<E>property(key).orElse((E) NO_OBJECT));
    }

    public ElementValueStep(final Traversal traversal, final String key, final E defaultValue) {
        super(traversal);
        this.key = key;
        this.defaultValue = SOptional.of(defaultValue);
        this.defaultSupplier = SOptional.empty();
        this.setFunction(traverser -> traverser.get().<E>property(key).orElse(this.defaultValue.get()));
    }

    public ElementValueStep(final Traversal traversal, final String key, final Supplier<E> defaultSupplier) {
        super(traversal);
        this.key = key;
        this.defaultValue = SOptional.empty();
        this.defaultSupplier = SOptional.of(defaultSupplier);
        this.setFunction(traverser -> traverser.get().<E>property(key).orElse(this.defaultSupplier.get().get()));
    }
}
