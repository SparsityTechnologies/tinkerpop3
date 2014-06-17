package com.tinkerpop.gremlin.process.graph.step.filter

import com.tinkerpop.gremlin.process.Traversal
import com.tinkerpop.gremlin.process.graph.step.filter.RandomTest
import com.tinkerpop.gremlin.structure.Vertex

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class GroovyRandomTestImpl extends RandomTest {

    public Traversal<Vertex, Vertex> get_g_V_randomX1X() {
        g.V.random(1.0f)
    }

    public Traversal<Vertex, Vertex> get_g_V_randomX0X() {
        g.V.random(0.0f)
    }
}
