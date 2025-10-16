package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;

public final class RoseauReport {
	private final API v1;
	private final API v2;
	private final List<BreakingChange> breakingChanges;

	public RoseauReport(API v1, API v2, List<BreakingChange> breakingChanges) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		Preconditions.checkNotNull(breakingChanges);
		this.v1 = v1;
		this.v2 = v2;
		this.breakingChanges = List.copyOf(breakingChanges);
	}

	public API v1() {
		return v1;
	}

	public API v2() {
		return v2;
	}

	public List<BreakingChange> getBreakingChanges() {
		return breakingChanges.stream()
			.filter(bc -> !v1.isExcluded(bc.impactedSymbol()))
			.toList();
	}

	public List<BreakingChange> getAllBreakingChanges() {
		return breakingChanges;
	}
}
