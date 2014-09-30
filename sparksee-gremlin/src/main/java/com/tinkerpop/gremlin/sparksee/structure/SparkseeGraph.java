package com.tinkerpop.gremlin.sparksee.structure;

import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.util.DefaultTraversal;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Transaction;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.structure.util.StringFactory;

import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.security.InvalidParameterException;

/**
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_PERFORMANCE)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_COMPUTER)
public class SparkseeGraph implements Graph {
    
    protected static final int INVALID_TYPE = com.sparsity.sparksee.gdb.Type.InvalidType;
    
    private static final String DB_PARAMETER     = "tinkerpop3.sparksee.directory";
    private static final String CONFIG_DIRECTORY = "tinkerpop3.sparksee.config";
    
    /**
     * Database persistent file.
     */
    private File dbFile = null;
    private com.sparsity.sparksee.gdb.Sparksee sparksee = null;
    private com.sparsity.sparksee.gdb.Database db = null;
    private SparkseeTransaction transaction = null;
    
    private SparkseeGraph(final Configuration configuration) {

        final String fileName   = configuration.getString(DB_PARAMETER);
        final String configFile = configuration.getString(CONFIG_DIRECTORY, null);

        dbFile = new File(fileName);

        if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs()) {
            throw new InvalidParameterException(String.format("Unable to create directory %s.", dbFile.getParent()));
        }
        
        try {
            if (configFile != null) {
                com.sparsity.sparksee.gdb.SparkseeProperties.load(configFile);
            }

            sparksee = new com.sparsity.sparksee.gdb.Sparksee(new com.sparsity.sparksee.gdb.SparkseeConfig());
            if (!dbFile.exists()) {
                db = sparksee.create(dbFile.getPath(), dbFile.getName());
            } else {
                db = sparksee.open(dbFile.getPath(), false);
            }
            transaction = new SparkseeTransaction(this, db);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    /**
     * Open a new {@link SparkseeGraph} instance.
     *
     * @param configuration the configuration for the instance
     * @param <G>           the {@link com.tinkerpop.gremlin.structure.Graph} instance
     * @return a newly opened {@link com.tinkerpop.gremlin.structure.Graph}
     */
    @SuppressWarnings("unchecked")
    public static <G extends Graph> G open(final Configuration configuration) {
        if (configuration == null) {
            throw Graph.Exceptions.argumentCanNotBeNull("configuration");
        }
        if (!configuration.containsKey(DB_PARAMETER)) {
            throw new IllegalArgumentException(String.format("Sparksee configuration requires %s to be set", DB_PARAMETER));
        }
        
        return (G) new SparkseeGraph(configuration);
    }
    
    @Override
    public Vertex addVertex(final Object... keyValues) {
        ElementHelper.legalPropertyKeyValueArray(keyValues);
        if (ElementHelper.getIdValue(keyValues).isPresent()) {
            throw Vertex.Exceptions.userSuppliedIdsNotSupported();
        }
        
        this.tx().readWrite();
        
        final String label = ElementHelper.getLabelValue(keyValues).orElse(Vertex.DEFAULT_LABEL);
        com.sparsity.sparksee.gdb.Graph rawGraph = transaction.getRawGraph();
        int type = rawGraph.findType(label);
        if (type == INVALID_TYPE) {
            type = rawGraph.newNodeType(label);
        }
        assert type != INVALID_TYPE;
        
        long oid = rawGraph.newNode(type);
        final SparkseeVertex vertex = new SparkseeVertex(oid, label, this);
        ElementHelper.attachProperties(vertex, keyValues);
        return vertex;
    }

    @Override
    public Vertex v(final Object id) {
        if (id == null) {
            throw Graph.Exceptions.elementNotFound(Vertex.class, id);
        }
        
        this.tx().readWrite();
        try {
            final Long longId = Double.valueOf(id.toString()).longValue();
            com.sparsity.sparksee.gdb.Graph rawGraph = transaction.getRawGraph();
            final int type = rawGraph.getObjectType(longId);
            if (type == INVALID_TYPE) {
                throw Graph.Exceptions.elementNotFound(Vertex.class, id);
            }
            
            return new SparkseeVertex(longId, rawGraph.getType(type).getName(), this);
        } catch (Exception e) {
            throw Graph.Exceptions.elementNotFound(Vertex.class, id);
        }
    }

    @Override
    public Edge e(final Object id) {
        if (id == null) {
            throw Graph.Exceptions.elementNotFound(Vertex.class, id);
        }
        
        this.tx().readWrite();
        try {
            final Long longId = Double.valueOf(id.toString()).longValue();
            com.sparsity.sparksee.gdb.Graph rawGraph = transaction.getRawGraph();
            final int type = rawGraph.getObjectType(longId);
            if (type == INVALID_TYPE) {
                throw Graph.Exceptions.elementNotFound(Vertex.class, id);
            }
            
            return new SparkseeEdge(longId, rawGraph.getType(type).getName(), this);
        } catch (Exception e) {
            throw Graph.Exceptions.elementNotFound(Vertex.class, id);
        }
    }
    
    @Override
    public GraphTraversal<Vertex, Vertex> V() {
        return null;
    }

    @Override
    public GraphTraversal<Edge, Edge> E() {
        return null;
    }

    @Override
    public <S> GraphTraversal<S, S> of() {
        return null;
    }

    @Override
    public void close() {
        transaction.closeAll();
        db.close();
        sparksee.close();
    }

    @Override
    public Transaction tx() {
        return transaction;
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public GraphComputer compute(final Class... graphComputerClass) {
        throw Graph.Exceptions.graphComputerNotSupported();
    }
    
    @Override
    public Features features() {
        return new SparkseeFeatures.SparkseeGeneralFeatures();
    }

    @Override
    public Variables variables() {
        throw Graph.Exceptions.variablesNotSupported();
    }

    @Override
    public String toString() {
        return StringFactory.graphString(this, dbFile.getPath());
    }
}
