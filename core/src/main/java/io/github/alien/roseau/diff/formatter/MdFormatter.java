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
		if (report.breakingChanges().isEmpty()) {
			sb.append("No breaking changes detected.");
		} else {
			int total = report.breakingChanges().size();
			int binaryBreaking = report.getBinaryBreakingChanges().size();
			int sourceBreaking = report.getSourceBreakingChanges().size();
			sb.append(total).append(" breaking changes detected");
			sb.append(" (").append(binaryBreaking).append(" binary-breaking, ");
			sb.append(sourceBreaking).append(" source-breaking).\n\n");
			sb.append("| Type | Symbol | Kind | Nature | Location | New symbol | Binary | Source |\n");
			sb.append("|------|--------|------|--------|----------|------------|--------|--------|\n");

			for (BreakingChange bc : report.breakingChanges()) {
				sb.append("| ").append(bc.impactedType().getQualifiedName()).append(" | ")
					.append(bc.impactedSymbol().getQualifiedName()).append(" | ")
					.append(bc.kind()).append(" | ")
					.append(bc.kind().getNature()).append(" | ")
					.append(formatLocation(bc.getLocation())).append(" | ")
					.append(bc.newSymbol() != null ? BreakingChange.printSymbol(bc.newSymbol()) : "").append(" | ")
					.append(bc.kind().isBinaryBreaking()).append(" | ")
					.append(bc.kind().isSourceBreaking()).append(" |\n");
			}
		}
		return sb.toString();
	}

	private static String formatLocation(SourceLocation location) {
		if (location == SourceLocation.NO_LOCATION) return "No location";
		return location.line() != -1 ? "%s:%d".formatted(location.file(), location.line()) : location.file().toString();
	}
}
