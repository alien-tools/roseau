package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;

import java.util.stream.Collectors;

/**
 * A formatter of {@link RoseauReport} that produces a CSV output.
 */
public class CsvFormatter implements BreakingChangesFormatter {
	public static final String HEADER = "type;symbol;kind;nature";

	@Override
	public String format(API api, RoseauReport report) {
		return HEADER + System.lineSeparator() +
			report.breakingChanges().stream().map(bc -> "%s;%s;%s;%s".formatted(
				bc.impactedType().getQualifiedName(),
				bc.impactedSymbol().getQualifiedName(),
				bc.kind(),
				bc.kind().getNature())
			).collect(Collectors.joining(System.lineSeparator()));
	}
}
