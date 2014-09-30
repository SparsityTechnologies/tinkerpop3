package com.tinkerpop.gremlin.sparksee.structure;

import com.sparsity.sparksee.gdb.EdgesDirection;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
public class SparkseeVertex extends SparkseeElement implements Vertex {

    protected static final int SCOPE = com.sparsity.sparksee.gdb.Type.NodesType;
    
    protected static final EdgesDirection SPARKSEE_IN   = EdgesDirection.Ingoing;
    protected static final EdgesDirection SPARKSEE_OUT  = EdgesDirection.Outgoing;
    protected static final EdgesDirection SPARKSEE_BOTH = EdgesDirection.Any;
    
    protected static Map<Direction, EdgesDirection> directionMapper = new HashMap<Direction, EdgesDirection>();
    static {
        directionMapper.put(Direction.IN,   SPARKSEE_IN);
        directionMapper.put(Direction.OUT,  SPARKSEE_OUT);
        directionMapper.put(Direction.BOTH, SPARKSEE_BOTH);
    }
    
    protected SparkseeVertex(final Long id, final String label, final SparkseeGraph graph) {
        super(id, label, graph);
    }
    
    protected Long getId() {
        return (Long) id;
    }
    
    @Override
    public Edge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
        if (label == null) {
            throw Edge.Exceptions.edgeLabelCanNotBeNull();
        }
        ElementHelper.legalPropertyKeyValueArray(keyValues);
        if (ElementHelper.getIdValue(keyValues).isPresent()) {
            throw Edge.Exceptions.userSuppliedIdsNotSupported();
        }
        
        graph.tx().readWrite();
        com.sparsity.sparksee.gdb.Graph rawGraph = ((SparkseeTransaction) graph.tx()).getRawGraph();
        int type = rawGraph.findType(label);
        if (type == SparkseeGraph.INVALID_TYPE) {
            type = rawGraph.newEdgeType(label, true, true);
        }
        assert type != SparkseeGraph.INVALID_TYPE;
        assert inVertex instanceof SparkseeVertex;
        
        long oid = rawGraph.newEdge(type, (Long) id, ((SparkseeVertex) inVertex).getId());
        return new SparkseeEdge(oid, label, graph);
    }
    
    @Override
    public void remove() {
        graph.tx().readWrite();
        try {
            ((SparkseeTransaction) graph.tx()).getRawGraph().drop((Long) id);
        } catch (Exception e) {
        }
    }
    
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterator<Vertex> vertices(final Direction direction, final int branchFactor, final String... labels) {
        graph.tx().readWrite();
        return new SparkseeHelper.SparkseeVertexIterator(graph, this, directionMapper.get(direction), branchFactor, labels);
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterator<Edge> edges(final Direction direction, final int branchFactor, final String... labels) {
        graph.tx().readWrite();
        return new SparkseeHelper.SparkseeEdgeIterator(graph, this, directionMapper.get(direction), branchFactor, labels);
    }
    
    @Override
    public String toString() {
        return StringFactory.vertexString(this);
    }
    
    //TODO: The traversal overrides
}
