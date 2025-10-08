package io.github.alien.roseau.diff.formatter;

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

		if (report.breakingChanges().isEmpty()) {
			sb.append("No breaking changes detected.");
		} else {
			sb.append(report.breakingChanges().size()).append(" breaking changes detected!\n");

			sb.append("### Details\n");
			sb.append("| Element | Old Location | New Location | Kind | Nature |\n");
			sb.append("|---------|--------------|--------------|------|--------|\n");
			for (BreakingChange bc : report.breakingChanges()) {
				sb.append("| ")
					.append(bc.impactedSymbol().getQualifiedName()).append(" | ")
					.append(bc.impactedSymbol().getLocation().file()).append(":").append(bc.impactedSymbol().getLocation().line()).append(" | ");

				if (bc.newSymbol() != null) {
					sb.append(bc.newSymbol().getLocation().file()).append(":").append(bc.newSymbol().getLocation().line());
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
}
