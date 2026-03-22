package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.stream.Collectors;

/**
 * A formatter of {@link RoseauReport} that produces a CSV output.
 */
public class CsvFormatter implements BreakingChangesFormatter {
	public static final String HEADER = "type;symbol;kind;nature;location;newSymbol;binaryBreaking;sourceBreaking";

	@Override
	public String format(RoseauReport report) {
		return HEADER + System.lineSeparator() +
			report.breakingChanges().stream().map(bc -> "%s;%s;%s;%s;%s;%s;%s;%s".formatted(
				bc.impactedType().getQualifiedName(),
				bc.impactedSymbol().getQualifiedName(),
				bc.kind(),
				bc.kind().getNature(),
				formatLocation(bc.getLocation()),
				bc.newSymbol() != null ? BreakingChange.printSymbol(bc.newSymbol()) : "",
				bc.kind().isBinaryBreaking(),
				bc.kind().isSourceBreaking())
			).collect(Collectors.joining(System.lineSeparator()));
	}

	private static String formatLocation(SourceLocation location) {
		if (location == SourceLocation.NO_LOCATION) {
			return "";
		}
		return location.line() != -1 ? location.toString() : location.file().toString();
	}
}
