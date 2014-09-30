package com.tinkerpop.gremlin.sparksee.process;

import com.tinkerpop.gremlin.process.ProcessStandardSuite;
import com.tinkerpop.gremlin.sparksee.SparkseeGraphGraphProvider;
import com.tinkerpop.gremlin.sparksee.structure.SparkseeGraph;
import org.junit.runner.RunWith;

/**
 * Executes the Standard Gremlin Process Test Suite using Sparksee.
 *
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
@RunWith(ProcessStandardSuite.class)
@ProcessStandardSuite.GraphProviderClass(provider = SparkseeGraphGraphProvider.class, graph = SparkseeGraph.class)
public class SparkseeGraphProcessStandardTest {
}
