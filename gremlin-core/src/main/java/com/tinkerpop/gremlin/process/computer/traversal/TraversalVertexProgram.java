package com.tinkerpop.gremlin.process.computer.traversal;

import com.tinkerpop.gremlin.process.PathTraverser;
import com.tinkerpop.gremlin.process.SimpleTraverser;
import com.tinkerpop.gremlin.process.Step;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.computer.MessageType;
import com.tinkerpop.gremlin.process.computer.Messenger;
import com.tinkerpop.gremlin.process.computer.VertexProgram;
import com.tinkerpop.gremlin.process.computer.util.VertexProgramHelper;
import com.tinkerpop.gremlin.process.graph.step.map.GraphStep;
import com.tinkerpop.gremlin.process.graph.strategy.PathConsumerStrategy;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.util.function.SSupplier;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TraversalVertexProgram<M extends TraversalMessage> implements VertexProgram<M> {

    private MessageType.Global global = MessageType.Global.of();

    public static final String TRAVERSAL_SUPPLIER = "gremlin.traversalSupplier";
    public static final String TRAVERSAL_SUPPLIER_CLASS = "gremlin.traversalSupplierClass";
    // TODO: public static final String TRAVERSAL_STRING = "gremlin.traversalString";

    private static final String TRACK_PATHS = "gremlin.traversalVertexProgram.trackPaths";
    private static final String VOTE_TO_HALT = "voteToHalt";
    public static final String TRAVERSER_TRACKER = Property.hidden("gremlin.traversalVertexProgram.traverserTracker");

    private SSupplier<Traversal> traversalSupplier;
    private boolean trackPaths = false;


    public TraversalVertexProgram() {
    }

    @Override
    public void initialize(final Configuration configuration) {
        try {
            this.trackPaths = configuration.getBoolean(TRACK_PATHS, false);
            if (configuration.containsKey(TRAVERSAL_SUPPLIER_CLASS)) {
                final Class<SSupplier<Traversal>> traversalSupplierClass =
                        (Class) Class.forName(configuration.getProperty(TRAVERSAL_SUPPLIER_CLASS).toString());
                this.traversalSupplier = traversalSupplierClass.getConstructor().newInstance();
            } else {
                this.traversalSupplier = VertexProgramHelper.deserializeSupplier(configuration, TRAVERSAL_SUPPLIER);
            }
            this.trackPaths = PathConsumerStrategy.trackPaths(this.traversalSupplier.get());
        } catch (final Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void setup(final GraphComputer.Globals globals) {
        globals.setIfAbsent(VOTE_TO_HALT, true);
    }

    @Override
    public void execute(final Vertex vertex, final Messenger<M> messenger, GraphComputer.Globals globals) {
        if (globals.isInitialIteration()) {
            executeFirstIteration(vertex, messenger, globals);
        } else {
            executeOtherIterations(vertex, messenger, globals);
        }
    }

    private void executeFirstIteration(final Vertex vertex, final Messenger<M> messenger, final GraphComputer.Globals globals) {
        final Traversal traversal = this.traversalSupplier.get();
        traversal.strategies().applyFinalOptimizers(traversal);
        final GraphStep startStep = (GraphStep) traversal.getSteps().get(0);   // TODO: make this generic to Traversal
        //startStep.clear();

        final String future = (traversal.getSteps().size() == 1) ? Traverser.NO_FUTURE : ((Step) traversal.getSteps().get(1)).getAs();
        // TODO: Was doing some HasContainer.testAll() stuff prior to the big change (necessary?)
        // TODO: Make this an optimizer.
        final AtomicBoolean voteToHalt = new AtomicBoolean(true);
        if (Vertex.class.isAssignableFrom(startStep.returnClass)) {
            final Traverser<Vertex> traverser = this.trackPaths ?
                    new PathTraverser<>(startStep.getAs(), vertex) :
                    new SimpleTraverser<>(vertex);
            traverser.setFuture(future);
            messenger.sendMessage(MessageType.Global.of(vertex), TraversalMessage.of(traverser));
            voteToHalt.set(false);
        } else if (Edge.class.isAssignableFrom(startStep.returnClass)) {
            vertex.outE().forEach(e -> {
                final Traverser<Edge> traverser = this.trackPaths ?
                        new PathTraverser<>(startStep.getAs(), e) :
                        new SimpleTraverser<>(e);
                traverser.setFuture(future);
                messenger.sendMessage(MessageType.Global.of(vertex), TraversalMessage.of(traverser));
                voteToHalt.set(false);
            });
        }
        globals.and(VOTE_TO_HALT, voteToHalt.get());
    }

    private void executeOtherIterations(final Vertex vertex, final Messenger<M> messenger, GraphComputer.Globals globals) {
        if (this.trackPaths) {
            final TraversalPaths tracker = new TraversalPaths(vertex);
            globals.and(VOTE_TO_HALT, TraversalPathMessage.execute(vertex, (Iterable) messenger.receiveMessages(this.global), messenger, tracker, this.traversalSupplier));
            vertex.property(TRAVERSER_TRACKER, tracker);
        } else {
            final TraversalCounters tracker = new TraversalCounters(vertex);
            globals.and(VOTE_TO_HALT, TraversalCounterMessage.execute(vertex, (Iterable) messenger.receiveMessages(this.global), messenger, tracker, this.traversalSupplier));
            vertex.property(TRAVERSER_TRACKER, tracker);
        }
    }

    @Override
    public boolean terminate(final GraphComputer.Globals globals) {
        final boolean voteToHalt = globals.get(VOTE_TO_HALT);
        if (voteToHalt) {
            return true;
        } else {
            globals.or(VOTE_TO_HALT, true);
            return false;
        }
    }

    @Override
    public Class<M> getMessageClass() {
        return (Class) (this.trackPaths ? TraversalPathMessage.class : TraversalCounterMessage.class);
    }

    @Override
    public Map<String, KeyType> getComputeKeys() {
        return VertexProgram.ofComputeKeys(TRAVERSER_TRACKER, KeyType.VARIABLE);
    }

    @Override
    public Set<String> getGlobalKeys() {
        final Set<String> keys = new HashSet<>();
        keys.add(VOTE_TO_HALT);
        return keys;
    }


    public String toString() {
        return "TraversalVertexProgram" + this.traversalSupplier.get().toString();
    }

    public SSupplier<Traversal> getTraversalSupplier() {
        return this.traversalSupplier;
    }

    //////////////

    public static Builder create() {
        return new Builder();
    }

    public static class Builder implements VertexProgram.Builder {
        private final Configuration configuration = new BaseConfiguration();

        public Builder() {
            this.configuration.setProperty(GraphComputer.VERTEX_PROGRAM, TraversalVertexProgram.class.getName());
        }

        public Builder traversal(final SSupplier<Traversal> traversalSupplier) {
            try {
                VertexProgramHelper.serializeSupplier(traversalSupplier, this.configuration, TRAVERSAL_SUPPLIER);
            } catch (final IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return this;
        }

        public Builder traversal(final Class<SSupplier<Traversal>> traversalSupplierClass) {
            this.configuration.setProperty(TRAVERSAL_SUPPLIER_CLASS, traversalSupplierClass);
            return this;
        }

        public Configuration getConfiguration() {
            return this.configuration;
        }
    }
}