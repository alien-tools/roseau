package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A formatter for {@link BreakingChange} instances that produces a CSV output.
 */
public class CsvFormatter implements BreakingChangesFormatter {
	@Override
	public String format(API api, List<BreakingChange> changes) {
		return "element,oldPosition,newPosition,kind,nature" + System.lineSeparator() +
			changes.stream().map(bc -> "%s,%s,%s,%s,%s".formatted(
				bc.impactedSymbol().getQualifiedName(),
				formatLocation(bc.impactedSymbol().getLocation()),
				bc.newSymbol() != null ? formatLocation(bc.newSymbol().getLocation()) : "",
				bc.kind(),
				bc.kind().getNature())
			).collect(Collectors.joining(System.lineSeparator()));
	}

	private static String formatLocation(io.github.alien.roseau.api.model.SourceLocation l) {
		return l.file() + ":" + l.line() + ":" + l.column();
	}
}
