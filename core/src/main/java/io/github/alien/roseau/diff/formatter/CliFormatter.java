package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;

import java.util.stream.Collectors;

/**
 * A formatter of {@link RoseauReport} that produces a CSV output.
 */
public class CliFormatter implements BreakingChangesFormatter {
	private final boolean plain;

	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";

	public CliFormatter(boolean plain) {
		this.plain = plain;
	}

	@Override
	public String format(RoseauReport report) {
		return report.getBreakingChanges().stream()
			.map(this::formatBC)
			.collect(Collectors.joining(System.lineSeparator()));
	}

	private String formatBC(BreakingChange bc) {
		String details = formatDetails(bc.details());
		boolean isLocalSymbol =
			bc.impactedSymbol() instanceof TypeDecl ||
				(bc.impactedSymbol() instanceof TypeMemberDecl member &&
					member.getContainingType().getQualifiedName().equals(bc.impactedType().getQualifiedName()));

		if (plain) {
			return "%s %s%s%s%n\t%s".formatted(
				bc.kind(),
				bc.impactedSymbol().getQualifiedName(),
				isLocalSymbol ? "" : " in " + bc.impactedType().getQualifiedName(),
				details.isEmpty() ? "" : " [%s]".formatted(details),
				bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION
					? "No source location"
					: "%s:%d".formatted(bc.impactedSymbol().getLocation().file(), bc.impactedSymbol().getLocation().line()));
		} else {
			return "%s %s%s%s%n\t%s".formatted(
				RED_TEXT + BOLD + bc.kind() + RESET,
				UNDERLINE + bc.impactedSymbol().getQualifiedName() + RESET,
				isLocalSymbol ? "" : " in " + bc.impactedType().getQualifiedName(),
				details.isEmpty() ? "" : " [%s]".formatted(details),
				bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION
					? "No source location"
					: "%s:%d".formatted(bc.impactedSymbol().getLocation().file(), bc.impactedSymbol().getLocation().line()));
		}
	}

	private static String formatDetails(BreakingChangeDetails details) {
		return switch (details) {
			case BreakingChangeDetails.None() -> "";
			case BreakingChangeDetails.MethodReturnTypeChanged(var oldType, var newType) ->
				"%s -> %s".formatted(oldType, newType);
			case BreakingChangeDetails.FieldTypeChanged(var oldType, var newType) -> "%s -> %s".formatted(oldType, newType);
			case BreakingChangeDetails.MethodAddedToInterface(var newMethod) -> newMethod.getSignature();
			case BreakingChangeDetails.MethodAbstractAddedToClass(var newMethod) -> newMethod.getSignature();
			case BreakingChangeDetails.SuperTypeRemoved(var type) -> type.toString();
			case BreakingChangeDetails.AnnotationTargetRemoved(var target) -> target.name();
			case BreakingChangeDetails.AnnotationMethodAddedWithoutDefault(var newMethod) -> newMethod.getSignature();
			case BreakingChangeDetails.ClassTypeChanged(var oldType, var newType) ->
				"%s -> %s".formatted(oldType.getSimpleName(), newType.getSimpleName());
			case BreakingChangeDetails.MethodNoLongerThrowsCheckedException(var exc) -> exc.getQualifiedName();
			case BreakingChangeDetails.MethodNowThrowsCheckedException(var exc) -> exc.getQualifiedName();
			case BreakingChangeDetails.MethodFormalTypeParametersAdded(var ftp) -> ftp.name();
			case BreakingChangeDetails.MethodFormalTypeParametersRemoved(var ftp) -> ftp.name();
			case BreakingChangeDetails.TypeFormalTypeParametersAdded(var ftp) -> ftp.name();
			case BreakingChangeDetails.TypeFormalTypeParametersRemoved(var ftp) -> ftp.name();
			case BreakingChangeDetails.MethodFormalTypeParametersChanged(var oldFtp, var newFtp) ->
				"%s -> %s".formatted(oldFtp.name(), newFtp.name());
			case BreakingChangeDetails.TypeFormalTypeParametersChanged(var oldFtp, var newFtp) ->
				"%s -> %s".formatted(oldFtp.name(), newFtp.name());
			case BreakingChangeDetails.MethodParameterGenericsChanged(var oldType, var newType) ->
				"%s -> %s".formatted(oldType, newType);
		};
	}
}
