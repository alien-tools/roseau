package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.diff.RoseauReport;

/**
 * A formatter that takes a {@link RoseauReport} as input and formats it.
 */
public interface BreakingChangesFormatter {
	/**
	 * Returns a string representation of the supplied report
	 *
	 * @param report the report to format
	 * @return the formatted list
	 */
	String format(RoseauReport report);
}
