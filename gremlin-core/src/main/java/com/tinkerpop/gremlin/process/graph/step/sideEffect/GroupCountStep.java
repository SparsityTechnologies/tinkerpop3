package com.tinkerpop.gremlin.process.graph.step.sideEffect;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.step.filter.FilterStep;
import com.tinkerpop.gremlin.process.util.FunctionRing;
import com.tinkerpop.gremlin.process.util.MapHelper;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.graph.marker.UnBulkable;
import com.tinkerpop.gremlin.util.function.SFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GroupCountStep<S> extends FilterStep<S> implements SideEffectCapable, Reversible, UnBulkable {

    public final Map<Object, Long> groupCountMap;
    public FunctionRing<S, ?> functionRing;

    public GroupCountStep(final Traversal traversal, final Map<Object, Long> groupCountMap, final SFunction<S, ?>... preGroupFunctions) {
        super(traversal);
        this.functionRing = new FunctionRing<>(preGroupFunctions);
        this.groupCountMap = groupCountMap;
        this.traversal.memory().set(CAP_VARIABLE, this.groupCountMap);
        this.setPredicate(traverser -> {
            MapHelper.incr(this.groupCountMap, this.functionRing.next().apply(traverser.get()), 1l);
            return true;
        });
    }

    public GroupCountStep(final Traversal traversal, final String variable, final SFunction<S, ?>... preGroupFunctions) {
        this(traversal, traversal.memory().getOrCreate(variable, HashMap<Object, Long>::new), preGroupFunctions);
    }
}
