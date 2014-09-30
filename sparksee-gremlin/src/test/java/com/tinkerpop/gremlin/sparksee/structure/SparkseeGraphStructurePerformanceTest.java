package com.tinkerpop.gremlin.sparksee.structure;

import com.tinkerpop.gremlin.structure.StructurePerformanceSuite;
import com.tinkerpop.gremlin.structure.StructureStandardSuite;
import com.tinkerpop.gremlin.sparksee.SparkseeGraphGraphProvider;
import com.tinkerpop.gremlin.sparksee.structure.SparkseeGraph;


import org.junit.runner.RunWith;

/**
 * Executes the Gremlin Structure Performance Test Suite using Sparksee.
 *
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
@RunWith(StructurePerformanceSuite.class)
@StructureStandardSuite.GraphProviderClass(provider = SparkseeGraphGraphProvider.class, graph = SparkseeGraph.class)
public class SparkseeGraphStructurePerformanceTest {

}