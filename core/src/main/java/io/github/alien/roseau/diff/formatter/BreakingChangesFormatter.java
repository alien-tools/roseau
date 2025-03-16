package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;

public interface BreakingChangesFormatter {
	String format(List<BreakingChange> changes);
}
