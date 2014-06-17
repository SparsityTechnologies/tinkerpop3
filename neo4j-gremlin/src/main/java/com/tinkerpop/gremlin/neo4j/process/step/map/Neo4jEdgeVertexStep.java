package com.tinkerpop.gremlin.neo4j.process.step.map;

import com.tinkerpop.gremlin.neo4j.structure.Neo4jEdge;
import com.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import com.tinkerpop.gremlin.neo4j.structure.Neo4jVertex;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.step.map.EdgeVertexStep;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class Neo4jEdgeVertexStep extends EdgeVertexStep {
    public Neo4jEdgeVertexStep(final Traversal traversal, final Neo4jGraph graph, final Direction direction) {
        super(traversal, direction);
        this.setFunction(traverser -> {
            Relationship relationship = ((Relationship) ((Neo4jEdge) traverser.get()).getRawElement());
            final List<Vertex> vertices = new ArrayList<>();
            if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH))
                vertices.add(new Neo4jVertex(relationship.getStartNode(), graph));
            if (direction.equals(Direction.IN) || direction.equals(Direction.BOTH))
                vertices.add(new Neo4jVertex(relationship.getEndNode(), graph));
            return vertices.iterator();
        });
    }
}
