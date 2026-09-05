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
			int total = report.getBreakingChanges().size();
			int binaryBreaking = report.getBinaryBreakingChanges().size();
			int sourceBreaking = report.getSourceBreakingChanges().size();
			sb.append(total).append(" breaking changes detected");
			sb.append(" (").append(binaryBreaking).append(" binary-breaking, ");
			sb.append(sourceBreaking).append(" source-breaking).\n\n");
			sb.append("| Type | Symbol | Kind | Nature | Location | New symbol | Binary | Source |\n");
			sb.append("|------|--------|------|--------|----------|------------|--------|--------|\n");

			for (BreakingChange bc : report.getBreakingChanges()) {
				sb.append("| ").append(escape(bc.impactedType().getQualifiedName())).append(" | ")
					.append(escape(bc.impactedSymbol().getQualifiedName())).append(" | ")
					.append(bc.kind()).append(" | ")
					.append(bc.kind().getNature()).append(" | ")
					.append(escape(formatLocation(bc.getLocation()))).append(" | ")
					.append(escape(bc.newSymbol() != null ? BreakingChange.printSymbol(bc.newSymbol()) : "")).append(" | ")
					.append(bc.kind().isBinaryBreaking()).append(" | ")
					.append(bc.kind().isSourceBreaking()).append(" |\n");
			}
		}
		return sb.toString();
	}

	private static String escape(String value) {
		StringBuilder escaped = new StringBuilder();
		for (char c : value.replace("\r\n", "\n").replace('\r', '\n').toCharArray()) {
			switch (c) {
				case '&' -> escaped.append("&amp;");
				case '<' -> escaped.append("&lt;");
				case '>' -> escaped.append("&gt;");
				case '\n' -> escaped.append("<br>");
				case '|', '\\', '`', '*', '_', '[', ']' -> escaped.append("&#").append((int) c).append(';');
				default -> escaped.append(c);
			}
		}
		return escaped.toString();
	}

	private static String formatLocation(SourceLocation location) {
		if (location == SourceLocation.NO_LOCATION) return "No location";
		return location.line() != -1 ? "%s:%d".formatted(location.file(), location.line()) : location.file().toString();
	}
}
