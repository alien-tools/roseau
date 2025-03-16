package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;
import java.util.stream.Collectors;

public class CsvFormatter implements BreakingChangesFormatter {
	@Override
	public String format(List<BreakingChange> changes) {
		StringBuilder sb = new StringBuilder();
		sb.append("element,oldPosition,newPosition,kind,nature").append(System.lineSeparator());
		sb.append(changes.stream().map(bc -> "%s,%s,%s,%s,%s".formatted(
			bc.impactedSymbol().getQualifiedName(),
			bc.impactedSymbol().getLocation(),
			bc.newSymbol() != null ? bc.newSymbol().getLocation() : "",
			bc.kind(),
			bc.kind().getNature())
		).collect(Collectors.joining(System.lineSeparator())));
		return sb.toString();
	}
}
