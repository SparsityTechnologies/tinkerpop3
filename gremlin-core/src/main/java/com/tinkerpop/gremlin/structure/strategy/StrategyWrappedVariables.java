package com.tinkerpop.gremlin.structure.strategy;

import com.tinkerpop.gremlin.structure.Graph;

import java.util.Map;
import java.util.Set;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class StrategyWrappedVariables implements StrategyWrapped, Graph.Variables {

    protected final StrategyWrappedGraph strategyWrappedGraph;
    private final Graph.Variables baseVariables;
    private final Strategy.Context<StrategyWrappedVariables> variableStrategyContext;

    public StrategyWrappedVariables(final Graph.Variables variables, final StrategyWrappedGraph strategyWrappedGraph) {
        if (variables instanceof StrategyWrapped) throw new IllegalArgumentException(
                String.format("The variables %s is already StrategyWrapped and must be a base Variables", variables));
        this.baseVariables = variables;
        this.strategyWrappedGraph = strategyWrappedGraph;
        this.variableStrategyContext = new Strategy.Context<>(strategyWrappedGraph.getBaseGraph(), this);
    }

    @Override
    public Set<String> keys() {
        return this.strategyWrappedGraph.strategy().compose(
                s -> s.getVariableKeysStrategy(variableStrategyContext),
                this.baseVariables::keys).get();
    }

    @Override
    public <R> R get(final String key) {
        return this.strategyWrappedGraph.strategy().compose(
                s -> s.<R>getVariableGetStrategy(variableStrategyContext),
                this.baseVariables::get).apply(key);
    }

    @Override
    public void set(final String key, final Object value) {
        this.strategyWrappedGraph.strategy().compose(
                s -> s.getVariableSetStrategy(variableStrategyContext),
                this.baseVariables::set).accept(key, value);
    }

    @Override
    public <R> R remove(final String key) {
        return this.strategyWrappedGraph.strategy().compose(
                s -> s.<R>getVariableGetStrategy(variableStrategyContext),
                this.baseVariables::remove).apply(key);
    }

    @Override
    public Map<String, Object> asMap() {
        return this.strategyWrappedGraph.strategy().compose(
                s -> s.getVariableAsMapStrategy(variableStrategyContext),
                this.baseVariables::asMap).get();
    }
}
