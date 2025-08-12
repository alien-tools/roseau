package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;

public record RoseauReport(
	Library v1,
	Library v2,
	List<BreakingChange> breakingChanges
) {
	public RoseauReport {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		Preconditions.checkNotNull(breakingChanges);
		breakingChanges = List.copyOf(breakingChanges);
	}
}
