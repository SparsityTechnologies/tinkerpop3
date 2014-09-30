package com.tinkerpop.gremlin.sparksee.groovy.plugin;

import java.util.HashSet;
import java.util.Set;

import com.tinkerpop.gremlin.groovy.plugin.AbstractGremlinPlugin;
import com.tinkerpop.gremlin.groovy.plugin.GremlinPlugin;
import com.tinkerpop.gremlin.groovy.plugin.PluginAcceptor;

/**
 * @author <a href="http://www.sparsity-technologies.com">Sparsity Technologies</a>
 */
public class SparkseeGremlinPlugin extends AbstractGremlinPlugin {
    private static final Set<String> IMPORTS = new HashSet<String>() {{
        add(IMPORT_SPACE + SparkseeGraph.class.getPackage().getName() + DOT_STAR);
    }};
    
    @Override
    public String getName() {
        return "sparksee";
    }
    
    @Override
    public void pluginTo(final PluginAcceptor pluginAcceptor) {
        pluginAcceptor.addImports(IMPORTS);
    }
}
