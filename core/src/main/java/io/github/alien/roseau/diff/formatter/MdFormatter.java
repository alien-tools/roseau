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
		sb.append("## Roseau - Breaking Changes Report\n");

		if (report.getBreakingChanges().isEmpty()) {
			sb.append("No breaking changes detected.");
		} else {
			sb.append(report.getBreakingChanges().size()).append(" breaking changes detected!\n");

			sb.append("### Details\n");
			sb.append("| Element | Old Location | New Location | Kind | Nature |\n");
			sb.append("|---------|--------------|--------------|------|--------|\n");
			for (BreakingChange bc : report.getBreakingChanges()) {
				sb.append("| ")
					.append(bc.impactedSymbol().getQualifiedName()).append(" | ")
					.append(location(bc.impactedSymbol().getLocation())).append(" | ");

				if (bc.newSymbol() != null) {
					sb.append(location(bc.newSymbol().getLocation()));
				} else {
					sb.append("N/A");
				}

				sb.append(" | ")
					.append(bc.kind()).append(" | ")
					.append(bc.kind().getNature()).append(" |\n");
			}
		}

		return sb.toString();
	}

	private static String location(SourceLocation location) {
		return location == SourceLocation.NO_LOCATION
			? "No location"
			: "%s:%d".formatted(location.file(), location.line());
	}
}
