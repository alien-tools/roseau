package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.diff.RoseauReport;

import java.util.stream.Collectors;

/**
 * A formatter of {@link RoseauReport} that produces a CSV output.
 */
public class CsvFormatter implements BreakingChangesFormatter {
	public static final String HEADER = "type;symbol;kind;nature;location";

	@Override
	public String format(RoseauReport report) {
		return HEADER + System.lineSeparator() +
			report.getBreakingChanges().stream().map(bc -> "%s;%s;%s;%s;%s".formatted(
				bc.impactedType().getQualifiedName(),
				bc.impactedSymbol().getQualifiedName(),
				bc.kind(),
				bc.kind().getNature(),
				bc.getLocation())
			).collect(Collectors.joining(System.lineSeparator()));
	}
}
