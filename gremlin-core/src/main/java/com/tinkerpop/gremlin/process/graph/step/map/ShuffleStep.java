package com.tinkerpop.gremlin.process.graph.step.map;

import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.util.UnTraverserIterator;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ShuffleStep<S> extends FlatMapStep<S, S> {

    public ShuffleStep(final Traversal traversal) {
        super(traversal);
        this.setFunction(traverser -> {
            final List<Traverser<S>> list = new ArrayList<>();
            list.add(traverser);
            list.addAll(StreamFactory.stream(getPreviousStep()).collect(Collectors.<Traverser<S>>toList()));
            Collections.shuffle(list);
            return new UnTraverserIterator<>(list.iterator());
        });
    }
}
