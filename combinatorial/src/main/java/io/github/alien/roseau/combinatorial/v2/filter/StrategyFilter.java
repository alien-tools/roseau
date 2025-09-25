package io.github.alien.roseau.combinatorial.v2.filter;

import java.util.HashSet;
import java.util.Set;

public sealed abstract class StrategyFilter permits OnlyFilter, PreviousFailuresFilter {
	protected Set<String> strategiesFilter = new HashSet<>();

	protected final boolean isEnabled;

	protected StrategyFilter(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	protected abstract void fillStrategiesFilter();

	public boolean ignores(String strategyName) {
		if (!isEnabled) return false;

		return strategiesFilter.contains(strategyName);
	}
}
