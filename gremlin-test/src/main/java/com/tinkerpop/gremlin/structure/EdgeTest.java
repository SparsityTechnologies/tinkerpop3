package com.tinkerpop.gremlin.structure;

import com.tinkerpop.gremlin.AbstractGremlinTest;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static com.tinkerpop.gremlin.structure.Graph.Features.PropertyFeatures.FEATURE_BOOLEAN_VALUES;
import static com.tinkerpop.gremlin.structure.Graph.Features.PropertyFeatures.FEATURE_DOUBLE_VALUES;
import static com.tinkerpop.gremlin.structure.Graph.Features.PropertyFeatures.FEATURE_FLOAT_VALUES;
import static com.tinkerpop.gremlin.structure.Graph.Features.PropertyFeatures.FEATURE_INTEGER_VALUES;
import static com.tinkerpop.gremlin.structure.Graph.Features.PropertyFeatures.FEATURE_LONG_VALUES;
import static com.tinkerpop.gremlin.structure.Graph.Features.PropertyFeatures.FEATURE_STRING_VALUES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class EdgeTest extends AbstractGremlinTest {
    @Test
    public void shouldHaveStandardStringRepresentation() {
        final Vertex v1 = g.addVertex();
        final Vertex v2 = g.addVertex();
        final Edge e = v1.addEdge("friends", v2);

        assertEquals(StringFactory.edgeString(e), e.toString());
    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.EdgePropertyFeatures.class, feature = FEATURE_STRING_VALUES)
    public void shouldAutotypeStringProperties() {
        final Graph graph = g;
        final Vertex v = graph.addVertex();
        final Edge e = v.addEdge("knows", v, "string", "marko");
        final String name = e.value("string");
        assertEquals(name, "marko");

    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.EdgePropertyFeatures.class, feature = FEATURE_INTEGER_VALUES)
    public void shouldAutotypIntegerProperties() {
        final Graph graph = g;
        final Vertex v = graph.addVertex();
        final Edge e = v.addEdge("knows", v, "integer", 33);
        final Integer age = e.value("integer");
        assertEquals(Integer.valueOf(33), age);
    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.EdgePropertyFeatures.class, feature = FEATURE_BOOLEAN_VALUES)
    public void shouldAutotypeBooleanProperties() {
        final Graph graph = g;
        final Vertex v = graph.addVertex();
        final Edge e = v.addEdge("knows", v, "boolean", true);
        final Boolean best = e.value("boolean");
        assertEquals(best, Boolean.valueOf(true));
    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.EdgePropertyFeatures.class, feature = FEATURE_DOUBLE_VALUES)
    public void shouldAutotypeDoubleProperties() {
        final Graph graph = g;
        final Vertex v = graph.addVertex();
        final Edge e = v.addEdge("knows", v, "double", 0.1d);
        final Double best = e.value("double");
        assertEquals(best, Double.valueOf(0.1d));
    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.EdgePropertyFeatures.class, feature = FEATURE_LONG_VALUES)
    public void shouldAutotypeLongProperties() {
        final Graph graph = g;
        final Vertex v = graph.addVertex();
        final Edge e = v.addEdge("knows", v, "long", 1l);
        final Long best = e.value("long");
        assertEquals(best, Long.valueOf(1l));
    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.EdgePropertyFeatures.class, feature = FEATURE_FLOAT_VALUES)
    public void shouldAutotypeFloatProperties() {
        final Graph graph = g;
        final Vertex v = graph.addVertex();
        final Edge e = v.addEdge("knows", v, "float", 0.1f);
        final Float best = e.value("float");
        assertEquals(best, Float.valueOf(0.1f));
    }

    @Test
    @FeatureRequirement(featureClass = Graph.Features.EdgePropertyFeatures.class, feature = FEATURE_STRING_VALUES)
    public void shouldGetPropertyKeysOnEdge() {
        final Vertex v = g.addVertex();
        final Edge e = v.addEdge("friend", v, "name", "marko", "location", "desert", "status", "dope");
        Set<String> keys = e.keys();
        assertEquals(3, keys.size());

        assertTrue(keys.contains("name"));
        assertTrue(keys.contains("location"));
        assertTrue(keys.contains("status"));

        final Map<String,Property> m = e.properties();
        assertEquals(3, m.size());
        assertEquals("name", m.get("name").key());
        assertEquals("location", m.get("location").key());
        assertEquals("status", m.get("status").key());
        assertEquals("marko", m.get("name").orElse(""));
        assertEquals("desert", m.get("location").orElse(""));
        assertEquals("dope", m.get("status").orElse(""));

        e.property("status").remove();

        keys = e.keys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("name"));
        assertTrue(keys.contains("location"));

        e.properties().values().stream().forEach(p->p.remove());

        keys = e.keys();
        assertEquals(0, keys.size());
    }

    @Test
    public void shouldNotGetConcurrentModificationException() {
        for (int i = 0; i < 25; i++) {
            final Vertex v = g.addVertex();
            v.addEdge("friend", v);
        }

        tryCommit(g, StructureStandardSuite.assertVertexEdgeCounts(25, 25));

        for(Edge e : g.E().toList()) {
            e.remove();
            tryCommit(g);
        }

        tryCommit(g, StructureStandardSuite.assertVertexEdgeCounts(25, 0));
    }

    @Test
    public void shouldReturnEmptyMapIfNoProperties() {
        final Vertex v = g.addVertex();
        final Map<String,Property> m = v.properties();
        assertNotNull(m);
        assertEquals(0, m.size());
    }
}
