package com.tinkerpop.gremlin.process.graph.step.filter;

import com.tinkerpop.gremlin.LoadGraphWith;
import com.tinkerpop.gremlin.process.AbstractGremlinProcessTest;
import com.tinkerpop.gremlin.process.T;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.util.StreamFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.tinkerpop.gremlin.LoadGraphWith.GraphData.CLASSIC;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class HasTest extends AbstractGremlinProcessTest {

    public abstract Traversal<Vertex, Vertex> get_g_v1_hasXkeyX(Object v1Id, String key);

    public abstract Traversal<Vertex, Vertex> get_g_v1_hasXname_markoX(Object v1Id);

    public abstract Traversal<Vertex, Vertex> get_g_V_hasXname_markoX();

    public abstract Traversal<Vertex, Vertex> get_g_V_hasXname_blahX();

    public abstract Traversal<Vertex, Vertex> get_g_V_hasXblahX();

    public abstract Traversal<Vertex, Vertex> get_g_v1_hasXage_gt_30X(Object v1Id);

    public abstract Traversal<Vertex, Vertex> get_g_v1_out_hasXid_2X(Object v1Id, Object v2Id);

    public abstract Traversal<Vertex, Vertex> get_g_V_hasXage_gt_30X();

    public abstract Traversal<Edge, Edge> get_g_e7_hasXlabelXknowsX(Object e7Id);

    public abstract Traversal<Edge, Edge> get_g_E_hasXlabelXknowsX();

    public abstract Traversal<Edge, Edge> get_g_E_hasXlabelXknows_createdX();

	public abstract Traversal<Vertex, Vertex> get_g_V_hasXname_equalspredicate_markoX();

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_v1_hasXkeyX() {
        Iterator<Vertex> traversal = get_g_v1_hasXkeyX(convertToVertexId("marko"), "name");
        System.out.println("Testing: " + traversal);
        assertEquals("marko", traversal.next().<String>value("name"));
        assertFalse(traversal.hasNext());
        traversal = get_g_v1_hasXkeyX(convertToVertexId("marko"), "circumference");
        System.out.println("Testing: " + traversal);
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_v1_hasXname_markoX() {
        Iterator<Vertex> traversal = get_g_v1_hasXname_markoX(convertToVertexId("marko"));
        System.out.println("Testing: " + traversal);
        assertEquals("marko", traversal.next().<String>value("name"));
        assertFalse(traversal.hasNext());
        traversal = get_g_v1_hasXname_markoX(convertToVertexId("vadas"));
        System.out.println("Testing: " + traversal);
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_V_hasXname_markoX() {
        final Iterator<Vertex> traversal = get_g_V_hasXname_markoX();
        System.out.println("Testing: " + traversal);
        assertEquals("marko", traversal.next().<String>value("name"));
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_V_hasXname_blahX() {
        final Iterator<Vertex> traversal = get_g_V_hasXname_blahX();
        System.out.println("Testing: " + traversal);
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_V_hasXage_gt_30X() {
        final Iterator<Vertex> traversal = get_g_V_hasXage_gt_30X();
        System.out.println("Testing: " + traversal);
        final List<Element> list = StreamFactory.stream(traversal).collect(Collectors.toList());
        assertEquals(2, list.size());
        for (final Element v : list) {
            assertTrue(v.<Integer>value("age") > 30);
        }
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_v1_hasXage_gt_30X() {
        Iterator<Vertex> traversal = get_g_v1_hasXage_gt_30X(convertToVertexId("marko"));
        System.out.println("Testing: " + traversal);
        assertFalse(traversal.hasNext());
        traversal = get_g_v1_hasXage_gt_30X(convertToVertexId("josh"));
        System.out.println("Testing: " + traversal);
        assertTrue(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_v1_out_hasXid_2X() {
        final Iterator<Vertex> traversal = get_g_v1_out_hasXid_2X(convertToVertexId("marko"), convertToVertexId("vadas"));
        System.out.println("Testing: " + traversal);
        assertTrue(traversal.hasNext());
        assertEquals(convertToVertexId("vadas"), traversal.next().id());
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_V_hasXblahX() {
        assumeTrue(graphMeetsTestRequirements());
        final Iterator<Vertex> traversal = get_g_V_hasXblahX();
        System.out.println("Testing: " + traversal);
        assertFalse(traversal.hasNext());
    }


    @Test
    @LoadGraphWith(CLASSIC)
    public void g_e7_hasXlabelXknowsX() {
        System.out.println(convertToEdgeId("marko", "knows", "vadas"));
        Iterator<Edge> traversal = get_g_e7_hasXlabelXknowsX(convertToEdgeId("marko", "knows", "vadas"));
        System.out.println("Testing: " + traversal);
        int counter = 0;
        while (traversal.hasNext()) {
            counter++;
            assertEquals("knows", traversal.next().label());
        }
        assertEquals(1, counter);
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_E_hasXlabelXknowsX() {
        final Iterator<Edge> traversal = get_g_E_hasXlabelXknowsX();
        System.out.println("Testing: " + traversal);
        int counter = 0;
        while (traversal.hasNext()) {
            counter++;
            assertEquals("knows", traversal.next().label());
        }
        assertEquals(2, counter);
    }

    @Test
    @LoadGraphWith(CLASSIC)
    public void g_E_hasXlabelXknows_createdX() {
        final Iterator<Edge> traversal = get_g_E_hasXlabelXknows_createdX();
        System.out.println("Testing: " + traversal);
        int counter = 0;
        while (traversal.hasNext()) {
            counter++;
            final String label = traversal.next().label();
            assertTrue(label.equals("knows") || label.equals("created"));
        }
        assertEquals(6, counter);
    }

	@Test
	@LoadGraphWith(CLASSIC)
	@Ignore
	public void g_V_hasXname_equalspredicate_markoX() {
		// todo: doesn't work in graph computer because of lambda

		final Iterator<Vertex> traversal = get_g_V_hasXname_equalspredicate_markoX();
		System.out.println("Testing: " + traversal);
		assertEquals("marko", traversal.next().<String>value("name"));
		assertFalse(traversal.hasNext());
	}

    public static class JavaHasTest extends HasTest {
        public JavaHasTest() {
            requiresGraphComputer = false;
        }

        public Traversal<Vertex, Vertex> get_g_v1_hasXkeyX(final Object v1Id, final String key) {
            return g.v(v1Id).has(key);
        }

        public Traversal<Vertex, Vertex> get_g_v1_hasXname_markoX(final Object v1Id) {
            return g.v(v1Id).has("name", "marko");
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXname_markoX() {
            return g.V().has("name", "marko");
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXname_blahX() {
            return g.V().has("name", "blah");
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXblahX() {
            return g.V().has("blah");
        }

        public Traversal<Vertex, Vertex> get_g_v1_hasXage_gt_30X(final Object v1Id) {
            return g.v(v1Id).has("age", T.gt, 30);
        }

        public Traversal<Vertex, Vertex> get_g_v1_out_hasXid_2X(final Object v1Id, final Object v2Id) {
            return g.v(v1Id).out().has(Element.ID, v2Id);
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXage_gt_30X() {
            return g.V().has("age", T.gt, 30);
        }

        public Traversal<Edge, Edge> get_g_e7_hasXlabelXknowsX(final Object e7Id) {
            return g.e(e7Id).has("label", "knows");
        }

        public Traversal<Edge, Edge> get_g_E_hasXlabelXknowsX() {
            return g.E().has("label", "knows");
        }

        public Traversal<Edge, Edge> get_g_E_hasXlabelXknows_createdX() {
            return g.E().has("label", T.in, Arrays.asList("knows", "created"));
        }

		public Traversal<Vertex, Vertex> get_g_V_hasXname_equalspredicate_markoX() {
			return g.V().has("name", (v1,v2) -> v1.equals(v2), "marko");
		}
    }

    public static class JavaComputerHasTest extends HasTest {
        public JavaComputerHasTest() {
            requiresGraphComputer = true;
        }

        public Traversal<Vertex, Vertex> get_g_v1_hasXkeyX(final Object v1Id, final String key) {
            return g.v(v1Id).<Vertex>has(key).submit(g.compute());
        }

        public Traversal<Vertex, Vertex> get_g_v1_hasXname_markoX(final Object v1Id) {
            return g.v(v1Id).<Vertex>has("name", "marko").submit(g.compute());
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXname_markoX() {
            return g.V().<Vertex>has("name", "marko").submit(g.compute());
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXname_blahX() {
            return g.V().<Vertex>has("name", "blah").submit(g.compute());
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXblahX() {
            return g.V().<Vertex>has("blah").submit(g.compute());
        }

        public Traversal<Vertex, Vertex> get_g_v1_hasXage_gt_30X(final Object v1Id) {
            return g.v(v1Id).<Vertex>has("age", T.gt, 30).submit(g.compute());
        }

        public Traversal<Vertex, Vertex> get_g_v1_out_hasXid_2X(final Object v1Id, final Object v2Id) {
            return g.v(v1Id).out().<Vertex>has(Element.ID, v2Id).submit(g.compute());
        }

        public Traversal<Vertex, Vertex> get_g_V_hasXage_gt_30X() {
            return g.V().<Vertex>has("age", T.gt, 30).submit(g.compute());
        }

        public Traversal<Edge, Edge> get_g_e7_hasXlabelXknowsX(final Object e7Id) {
            return g.e(e7Id).<Edge>has("label", "knows").submit(g.compute());
        }

        public Traversal<Edge, Edge> get_g_E_hasXlabelXknowsX() {
            return g.E().<Edge>has("label", "knows").submit(g.compute());
        }

        public Traversal<Edge, Edge> get_g_E_hasXlabelXknows_createdX() {
            return g.E().<Edge>has("label", T.in, Arrays.asList("knows", "created")).submit(g.compute());
        }

		public Traversal<Vertex, Vertex> get_g_V_hasXname_equalspredicate_markoX() {
			return g.V().<Vertex>has("age", (v1, v2) -> v1.equals(v2), 30).submit(g.compute());
		}
    }
}