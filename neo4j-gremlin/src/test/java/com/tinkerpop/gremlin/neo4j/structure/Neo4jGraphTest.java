package com.tinkerpop.gremlin.neo4j.structure;

import com.tinkerpop.gremlin.neo4j.Neo4jGraphProvider;
import com.tinkerpop.gremlin.structure.util.GraphFactory;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * These are tests specific to Neo4j.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Neo4jGraphTest {

    private final Neo4jGraphProvider graphProvider = new Neo4jGraphProvider();

	@Test
	public void shouldOpenWithOverridenConfig() throws Exception {
		final Configuration conf = graphProvider.newGraphConfiguration("standard");
		graphProvider.clear(conf);

		final Neo4jGraph g = Neo4jGraph.open(conf);
		assertNotNull(g);

		graphProvider.clear(g, conf);
	}

    @Test
    public void shouldExecuteCypher() throws Exception {
        final Configuration conf = graphProvider.newGraphConfiguration("standard");
        graphProvider.clear(conf);

        final Neo4jGraph g = (Neo4jGraph) GraphFactory.open(conf);
        g.addVertex("name", "marko");
        g.tx().commit();

        final Iterator<Map<String,Object>> result = g.query("MATCH (a {name:\"marko\"}) RETURN a", null);
        assertNotNull(result);
        assertTrue(result.hasNext());

        graphProvider.clear(g, conf);
    }
}
