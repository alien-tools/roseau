package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;

import java.util.stream.Collectors;

/**
 * A formatter of {@link RoseauReport} that produces a CSV output.
 */
public class CliFormatter implements BreakingChangesFormatter {
	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";

	@Override
	public String format(RoseauReport report) {
		return report.breakingChanges().stream()
			.map(bc -> {
				String details = switch (bc.details()) {
					case BreakingChangeDetails.None d -> "";
					case BreakingChangeDetails.MethodReturnTypeChanged(var oldType, var newType) ->
						"old: %s, new: %s".formatted(oldType, newType);
					case BreakingChangeDetails.MethodAddedToInterface(var newMethod) -> "method: " + newMethod.getSignature();
					case BreakingChangeDetails.MethodAbstractAddedToClass(var newMethod) -> "method: " + newMethod.getSignature();
				};

				if (false) { // plaoin
					return String.format("%s %s%s%n\t%s:%s", bc.kind(), bc.impactedSymbol().getQualifiedName(),
						details.isEmpty() ? "" : " [%s]".formatted(details),
						bc.impactedSymbol().getLocation().file(), bc.impactedSymbol().getLocation().line());
				} else {
					return String.format("%s %s%s%n\t%s:%s",
						RED_TEXT + BOLD + bc.kind() + RESET,
						UNDERLINE + bc.impactedSymbol().getQualifiedName() + RESET,
						details.isEmpty() ? "" : " [%s]".formatted(details),
						bc.impactedSymbol().getLocation().file(), bc.impactedSymbol().getLocation().line());
				}
			}).collect(Collectors.joining(System.lineSeparator()));
	}
}
