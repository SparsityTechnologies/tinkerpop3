package com.tinkerpop.gremlin.sparksee;

import com.tinkerpop.gremlin.AbstractGraphProvider;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.sparksee.structure.SparkseeGraph;

import org.apache.commons.configuration.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
public class SparkseeGraphGraphProvider extends AbstractGraphProvider {

    @Override
    @SuppressWarnings("serial")
    public Map<String, Object> getBaseConfiguration(final String graphName, final Class<?> test, final String testMethodName) {
        return new HashMap<String, Object>() {{
            put("gremlin.graph", SparkseeGraph.class.getName());
            put("tinkerpop3.sparksee.directory", System.getProperty("java.io.tmpdir") + File.separator + "test.gdb");
        }};
    }

    @Override
    public void clear(final Graph g, final Configuration configuration) throws Exception {
        if (g != null)
            g.close();
    }
}
