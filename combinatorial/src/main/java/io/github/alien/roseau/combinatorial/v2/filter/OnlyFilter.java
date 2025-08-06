package io.github.alien.roseau.combinatorial.v2.filter;

public final class OnlyFilter extends StrategyFilter {
	public OnlyFilter() {
		super(true);

		fillStrategiesFilter();
	}

	@Override
	protected void fillStrategiesFilter() {
		strategiesFilter.add("StrategyNameToKeep");
	}

	@Override
	public boolean ignores(String strategyName) {
		return !strategiesFilter.contains(strategyName);
	}
}
