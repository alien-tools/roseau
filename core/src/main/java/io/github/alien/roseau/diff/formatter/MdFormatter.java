package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;

/**
 * A formatter of {@link RoseauReport} that produces a Markdown output.
 */
public class MdFormatter implements BreakingChangesFormatter {
	/**
	 * Formats the list of breaking changes in Markdown format
	 */
	@Override
	public String format(RoseauReport report) {
		StringBuilder sb = new StringBuilder();
		sb.append("## Breaking Changes Report\n");
		if (report.getBreakingChanges().isEmpty()) {
			sb.append("No breaking changes detected.");
		} else {
			sb.append(report.getBreakingChanges().size()).append(" breaking changes detected.\n\n");
			sb.append("| Type | Symbol | Kind | Nature | Location |\n");
			sb.append("|------|--------|------|--------|----------|\n");

			for (BreakingChange bc : report.getBreakingChanges()) {
				sb.append("| ").append(bc.impactedType().getQualifiedName()).append(" | ")
					.append(bc.impactedSymbol().getQualifiedName()).append(" | ")
					.append(bc.kind()).append(" | ")
					.append(bc.kind().getNature()).append(" | ")
					.append(formatLocation(bc.getLocation())).append(" |\n");
			}
		}
		return sb.toString();
	}

	private static String formatLocation(SourceLocation location) {
		return location == SourceLocation.NO_LOCATION
			? "No location"
			: "%s:%d".formatted(location.file(), location.line());
	}
}
