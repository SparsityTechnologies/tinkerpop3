package com.tinkerpop.gremlin.giraph.process.computer;

import com.tinkerpop.gremlin.giraph.process.computer.util.ConfUtil;
import com.tinkerpop.gremlin.giraph.process.computer.util.MemoryAggregator;
import com.tinkerpop.gremlin.giraph.process.computer.util.RuleWritable;
import com.tinkerpop.gremlin.giraph.structure.GiraphVertex;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.computer.VertexProgram;
import org.apache.giraph.master.MasterCompute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GiraphGraphComputerGlobals extends MasterCompute implements GraphComputer.Globals {

    private final Logger LOGGER = LoggerFactory.getLogger(GiraphGraphComputerGlobals.class);
    private VertexProgram vertexProgram;
    private GiraphVertex giraphVertex;
    //private long runtime = System.currentTimeMillis();
    private Set<String> globalKeys;

    public GiraphGraphComputerGlobals() {
        this.giraphVertex = null;
        this.vertexProgram = null;
        this.initialize();
    }

    public GiraphGraphComputerGlobals(final GiraphVertex giraphVertex) {
        this.giraphVertex = giraphVertex;
        this.initialize();
    }

    public void initialize() {
        if (null == this.giraphVertex) {  // master compute node
            try {
                this.vertexProgram = VertexProgram.createVertexProgram(ConfUtil.makeApacheConfiguration(this.getConf()));
                this.globalKeys = new HashSet<String>(this.vertexProgram.getGlobalKeys());
                for (final String key : (Set<String>) this.vertexProgram.getGlobalKeys()) {
                    this.registerAggregator(key, MemoryAggregator.class); // TODO: Why does PersistentAggregator not work?
                }
                this.registerPersistentAggregator("runtime", MemoryAggregator.class);
                this.setIfAbsent("runtime", System.currentTimeMillis());
                this.vertexProgram.setup(this);
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
                // do nothing as Giraph has a hard time starting up with random exceptions until ZooKeeper comes online
            }
        } else {  // local vertex aggregator
            this.vertexProgram = VertexProgram.createVertexProgram(ConfUtil.makeApacheConfiguration(this.giraphVertex.getConf()));
            this.globalKeys = new HashSet<String>(this.vertexProgram.getGlobalKeys());
        }
    }

    public void compute() {
        if (!this.isInitialIteration()) {
            if (this.vertexProgram.terminate(this)) {
                this.haltComputation();
            }
        }
    }

    public int getIteration() {
        return null == this.giraphVertex ? (int) this.getSuperstep() : (int) this.giraphVertex.getSuperstep();
    }

    public long getRuntime() {
        return System.currentTimeMillis() - this.<Long>get("runtime");
    }

    public Set<String> keys() {
        return this.globalKeys;
    }

    public <R> R get(final String key) {
        final RuleWritable rule = (null == this.giraphVertex) ? this.getAggregatedValue(key) : this.giraphVertex.getAggregatedValue(key);
        return (R) rule.getObject();
    }

    public void set(final String key, Object value) {
        if (null == this.giraphVertex)
            this.setAggregatedValue(key, new RuleWritable(RuleWritable.Rule.SET, value));
        else
            this.giraphVertex.aggregate(key, new RuleWritable(RuleWritable.Rule.SET, value));
    }

    public void setIfAbsent(final String key, final Object value) {
        if (null == this.giraphVertex)
            this.setAggregatedValue(key, new RuleWritable(RuleWritable.Rule.SET_IF_ABSENT, value));
        else
            this.giraphVertex.aggregate(key, new RuleWritable(RuleWritable.Rule.SET_IF_ABSENT, value));
    }

    public boolean and(final String key, final boolean bool) {
        if (null == this.giraphVertex) {
            this.setAggregatedValue(key, new RuleWritable(RuleWritable.Rule.AND, ((RuleWritable) this.getAggregatedValue(key)).<Boolean>getObject() && bool));
            return ((RuleWritable) this.getAggregatedValue(key)).getObject();
        } else {
            this.giraphVertex.aggregate(key, new RuleWritable(RuleWritable.Rule.AND, bool));
            return ((RuleWritable) this.giraphVertex.getAggregatedValue(key)).getObject();
        }
    }

    public boolean or(final String key, final boolean bool) {
        if (null == this.giraphVertex) {
            this.setAggregatedValue(key, new RuleWritable(RuleWritable.Rule.OR, ((RuleWritable) this.getAggregatedValue(key)).<Boolean>getObject() || bool));
            return ((RuleWritable) this.getAggregatedValue(key)).getObject();
        } else {
            this.giraphVertex.aggregate(key, new RuleWritable(RuleWritable.Rule.OR, bool));
            return ((RuleWritable) this.giraphVertex.getAggregatedValue(key)).getObject();
        }
    }

    public long incr(final String key, final long delta) {
        if (null == this.giraphVertex) {
            this.setAggregatedValue(key, new RuleWritable(RuleWritable.Rule.INCR, ((RuleWritable) this.getAggregatedValue(key)).<Long>getObject() + delta));
            return ((RuleWritable) this.getAggregatedValue(key)).getObject();
        } else {
            this.giraphVertex.aggregate(key, new RuleWritable(RuleWritable.Rule.INCR, delta));
            return ((RuleWritable) this.giraphVertex.getAggregatedValue(key)).getObject();
        }
    }

    public void write(final DataOutput output) {
    }

    public void readFields(final DataInput input) {
    }
}
