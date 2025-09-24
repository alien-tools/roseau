package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;

import java.util.stream.Collectors;

/**
 * A formatter of {@link RoseauReport} that produces a CSV output.
 */
public class CsvFormatter implements BreakingChangesFormatter {
	@Override
	public String format(API api, RoseauReport report) {
		return "element,oldPosition,newPosition,kind,nature" + System.lineSeparator() +
			report.breakingChanges().stream().map(bc -> "%s,%s,%s,%s,%s".formatted(
				bc.impactedSymbol().getQualifiedName(),
				bc.impactedSymbol().getLocation(),
				bc.newSymbol() != null ? bc.newSymbol().getLocation() : "",
				bc.kind(),
				bc.kind().getNature())
			).collect(Collectors.joining(System.lineSeparator()));
	}
}
