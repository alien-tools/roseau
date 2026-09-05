package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A formatter of {@link RoseauReport} that produces a CSV output.
 */
public class CsvFormatter implements BreakingChangesFormatter {
	public static final String HEADER = "type;symbol;kind;nature;location;newSymbol;binaryBreaking;sourceBreaking";

	@Override
	public String format(RoseauReport report) {
		return HEADER + System.lineSeparator() +
			report.getBreakingChanges().stream().map(bc -> Stream.of(
				bc.impactedType().getQualifiedName(),
				bc.impactedSymbol().getQualifiedName(),
				bc.kind().name(),
				bc.kind().getNature().name(),
				formatLocation(bc.getLocation()),
				bc.newSymbol() != null ? BreakingChange.printSymbol(bc.newSymbol()) : "",
				Boolean.toString(bc.kind().isBinaryBreaking()),
				Boolean.toString(bc.kind().isSourceBreaking()))
				.map(CsvFormatter::escape).collect(Collectors.joining(";"))
			).collect(Collectors.joining(System.lineSeparator()));
	}

	private static String escape(String value) {
		if (value.indexOf(';') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
			return '"' + value.replace("\"", "\"\"") + '"';
		}
		return value;
	}

	private static String formatLocation(SourceLocation location) {
		if (location == SourceLocation.NO_LOCATION) {
			return "";
		}
		return location.line() != -1 ? location.toString() : location.file().toString();
	}
}
