package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;

/**
 * A formatter that takes a list of {@link BreakingChange} as input and produces as a string representation of them.
 */
public interface BreakingChangesFormatter {
	/**
	 * Returns a string representation of the supplied list of {@link BreakingChange}.
	 *
	 * @param changes the breaking changes to format
	 * @return the formatted list
	 */
	String format(List<BreakingChange> changes);
}
