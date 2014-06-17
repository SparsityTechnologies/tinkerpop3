package com.tinkerpop.gremlin.giraph.structure.io.graphson;

import com.tinkerpop.gremlin.giraph.structure.io.kryo.VertexStreamIterator;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.io.kryo.KryoReader;
import com.tinkerpop.gremlin.structure.io.kryo.KryoWriter;
import com.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class VertexStreamIteratorTest {
    @Test
    public void testAll() throws Exception {
        Graph g = TinkerFactory.createClassic();

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final KryoWriter writer = KryoWriter.create().build();
            writer.writeVertices(os, g.V(), Direction.BOTH);

            final AtomicInteger called = new AtomicInteger(0);
            final KryoReader reader = KryoReader.create()
                    .setWorkingDirectory(File.separator + "tmp").build();

            VertexStreamIterator vsi = new VertexStreamIterator(new ByteArrayInputStream(os.toByteArray()), reader);

            boolean found = false;
            while (vsi.hasNext()) {
                Vertex v = vsi.next();

                //System.out.println("v = " + v);
                //System.out.println("\tin edges: " + count(v.in().toList()));
                //System.out.println("\tout edges: " + count(v.out().toList()));
                String name = v.<String>property("name").value();
                //System.out.println("name: " + name);
                if (name.equals("ripple")) {
                     found = true;
                    assertEquals(1, count(v.in().toList()));
                    assertEquals(0, count(v.out().toList()));
                }

                called.incrementAndGet();
            }
            assertTrue(found);

            assertEquals(count(g.V().toList()), called.get());
        }
    }

    private <T> long count(final Iterable<T> iter) {
        long count = 0;
        for (T anIter : iter) {
            count++;
        }

        return count;
    }
}
