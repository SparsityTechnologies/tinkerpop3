package com.tinkerpop.gremlin.groovy;

import com.tinkerpop.gremlin.util.function.SSupplier;
import groovy.lang.Closure;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GSupplier<A> implements SSupplier<A> {

    private final Closure closure;

    public GSupplier(final Closure closure) {
        this.closure = closure;
    }

    public A get() {
        return (A) this.closure.call();
    }

    public static GSupplier[] make(final Closure... closures) {
        final GSupplier[] suppliers = new GSupplier[closures.length];
        for (int i = 0; i < closures.length; i++) {
            suppliers[i] = new GSupplier(closures[i]);
        }
        return suppliers;
    }
}
