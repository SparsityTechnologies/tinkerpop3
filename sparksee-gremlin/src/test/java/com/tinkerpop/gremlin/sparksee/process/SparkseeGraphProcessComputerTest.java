package com.tinkerpop.gremlin.sparksee.process;

import com.tinkerpop.gremlin.process.ProcessComputerSuite;
import com.tinkerpop.gremlin.sparksee.SparkseeGraphGraphProvider;
import com.tinkerpop.gremlin.sparksee.structure.SparkseeGraph;
import org.junit.runner.RunWith;

/**
 * Executes the Standard Gremlin Process Test Suite using Sparksee.
 *
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
@RunWith(ProcessComputerSuite.class)
@ProcessComputerSuite.GraphProviderClass(provider = SparkseeGraphGraphProvider.class, graph = SparkseeGraph.class)
public class SparkseeGraphProcessComputerTest {
}
