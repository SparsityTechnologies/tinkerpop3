package com.tinkerpop.gremlin.process.graph.step.sideEffect;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.step.filter.FilterStep;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.graph.marker.UnBulkable;
import com.tinkerpop.gremlin.util.function.SFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GroupByStep<S, K, V, R> extends FilterStep<S> implements SideEffectCapable, Reversible, UnBulkable {

    public Map<K, Collection<V>> groupMap;
    public final Map<K, R> reduceMap;
    public final SFunction<S, K> keyFunction;
    public final SFunction<S, V> valueFunction;
    public final SFunction<Collection<V>, R> reduceFunction;

    public GroupByStep(final Traversal traversal, final Map<K, Collection<V>> groupMap, final SFunction<S, K> keyFunction, final SFunction<S, V> valueFunction, final SFunction<Collection<V>, R> reduceFunction) {
        super(traversal);
        this.groupMap = groupMap;
        this.reduceMap = new HashMap<>();
        this.traversal.memory().set(CAP_VARIABLE, this.groupMap);
        this.keyFunction = keyFunction;
        this.valueFunction = valueFunction == null ? s -> (V) s : valueFunction;
        this.reduceFunction = reduceFunction;
        this.setPredicate(traverser -> {
            doGroup(traverser.get(), this.groupMap, this.keyFunction, this.valueFunction);
            if (null != reduceFunction && !this.getPreviousStep().hasNext()) {
                doReduce(this.groupMap, this.reduceMap, this.reduceFunction);
                this.traversal.memory().set(CAP_VARIABLE, this.reduceMap);
            }
            return true;
        });
    }

    public GroupByStep(final Traversal traversal, final String variable, final SFunction<S, K> keyFunction, final SFunction<S, V> valueFunction, final SFunction<Collection<V>, R> reduceFunction) {
        this(traversal, traversal.memory().getOrCreate(variable, HashMap<K, Collection<V>>::new), keyFunction, valueFunction, reduceFunction);
    }

    private static <S, K, V> void doGroup(final S s, final Map<K, Collection<V>> groupMap, final SFunction<S, K> keyFunction, final SFunction<S, V> valueFunction) {
        final K key = keyFunction.apply(s);
        final V value = valueFunction.apply(s);
        Collection<V> values = groupMap.get(key);
        if (null == values) {
            values = new ArrayList<>();
            groupMap.put(key, values);
        }
        GroupByStep.addValue(value, values);
    }

    private static <K, V, R> void doReduce(final Map<K, Collection<V>> groupMap, final Map<K, R> reduceMap, final SFunction<Collection<V>, R> reduceFunction) {
        groupMap.forEach((k, vv) -> {
            reduceMap.put(k, (R) reduceFunction.apply(vv));
        });
    }

    public static void addValue(final Object value, final Collection values) {
        if (value instanceof Iterator) {
            while (((Iterator) value).hasNext()) {
                values.add(((Iterator) value).next());
            }
        } else {
            values.add(value);
        }
    }
}
