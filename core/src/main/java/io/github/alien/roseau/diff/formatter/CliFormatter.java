package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;

import java.util.List;

/**
 * A formatter of {@link RoseauReport} that produces a CLI-friendly report.
 */
public class CliFormatter implements BreakingChangesFormatter {
	private final Mode mode;

	private static final String BOLD = "\u001B[1m";
	private static final String DIM = "\u001B[2m";
	private static final String CYAN = "\u001B[36m";
	private static final String RESET = "\u001B[0m";

	private static final String KIND_ADDITION = "➕";
	private static final String KIND_DELETION = "✗";
	private static final String KIND_MUTATION = "⚠";

	private static final String COMPATIBLE = "✓";
	private static final String BREAKING = "✗";

	public enum Mode {
		PLAIN,
		ANSI
	}

	public CliFormatter(Mode mode) {
		this.mode = mode;
	}

	public CliFormatter() {
		boolean user = System.console() != null && System.getenv("CI") == null;
		this(user ? Mode.ANSI : Mode.PLAIN);
	}

	@Override
	public String format(RoseauReport report) {
		List<BreakingChange> changes = report.getBreakingChanges();
		if (changes.isEmpty()) {
			return "No breaking changes found.";
		}

		int binaryBreaking = report.getBinaryBreakingChanges().size();
		int sourceBreaking = report.getSourceBreakingChanges().size();

		StringBuilder sb = new StringBuilder();

		sb.append(bold("Breaking Changes Found: ")).append(changes.size());
		sb.append(" (").append(binaryBreaking).append(" binary-breaking, ");
		sb.append(sourceBreaking).append(" source-breaking)");
		sb.append(System.lineSeparator());

		report.getBreakingChanges().forEach(bc -> formatBreakingChange(bc, sb));

		return sb.toString();
	}

	private void formatBreakingChange(BreakingChange bc, StringBuilder sb) {
		String emoji = switch (bc.kind().getNature()) {
			case ADDITION -> KIND_ADDITION;
			case DELETION -> KIND_DELETION;
			case MUTATION -> KIND_MUTATION;
		};

		sb.append(emoji).append(" ").append(formatSymbol(bc)).append(" ").append(formatKind(bc));
		String details = formatDetails(bc);
		if (!details.isEmpty()) {
			sb.append(" ").append(cyan("[" + details + "]"));
		}
		sb.append(System.lineSeparator());
		sb.append("  ").append(formatCompatibility(bc));
		sb.append(System.lineSeparator());
		sb.append("  ").append(dim(formatLocation(bc)));
		sb.append(System.lineSeparator());
	}

	private static String formatKind(BreakingChange bc) {
		return bc.kind().name();
	}

	private static String formatCompatibility(BreakingChange bc) {
		String binary = bc.kind().isBinaryBreaking()
			? BREAKING + " binary-breaking"
			: COMPATIBLE + " binary-compatible";
		String source = bc.kind().isSourceBreaking()
			? BREAKING + " source-breaking"
			: COMPATIBLE + " source-compatible";
		return binary + " " + source;
	}

	private static String formatSymbol(BreakingChange bc) {
		return bc.isLocal()
			? bc.impactedSymbol().getQualifiedName()
			: "%s in %s".formatted(bc.impactedSymbol().getQualifiedName(), bc.impactedType().getQualifiedName());
	}

	private static String formatLocation(BreakingChange bc) {
		return bc.getLocation() == SourceLocation.NO_LOCATION
			? "No source location"
			: "→ %s:%d".formatted(bc.getLocation().file(), bc.getLocation().line());
	}

	private static String formatDetails(BreakingChange bc) {
		return switch (bc.details()) {
			case BreakingChangeDetails.None() -> "";
			case BreakingChangeDetails.MethodReturnTypeChanged(var oldType, var newType) ->
				"%s → %s".formatted(oldType, newType);
			case BreakingChangeDetails.FieldTypeChanged(var oldType, var newType) -> "%s → %s".formatted(oldType, newType);
			case BreakingChangeDetails.TypeNewAbstractMethod(var newMethod) -> newMethod.getSignature();
			case BreakingChangeDetails.TypeSupertypeRemoved(var type) -> type.getQualifiedName();
			case BreakingChangeDetails.AnnotationTargetRemoved(var target) -> target.name();
			case BreakingChangeDetails.AnnotationNewMethodWithoutDefault(var newMethod) -> newMethod.getSignature();
			case BreakingChangeDetails.TypeKindChanged(var oldType, var newType) ->
				"%s → %s".formatted(oldType.getSimpleName(), newType.getSimpleName());
			case BreakingChangeDetails.MethodNoLongerThrowsCheckedException(var exc) -> exc.getQualifiedName();
			case BreakingChangeDetails.MethodNowThrowsCheckedException(var exc) -> exc.getQualifiedName();
			case BreakingChangeDetails.FormalTypeParametersAdded(var ftp) -> ftp.name();
			case BreakingChangeDetails.FormalTypeParametersRemoved(var ftp) -> ftp.name();
			case BreakingChangeDetails.FormalTypeParametersChanged(var oldFtp, var newFtp) ->
				"%s → %s".formatted(oldFtp.name(), newFtp.name());
			case BreakingChangeDetails.MethodParameterGenericsChanged(var oldType, var newType) ->
				"%s → %s".formatted(oldType, newType);
		};
	}

	private String bold(String text) {
		return mode == Mode.ANSI ? (BOLD + text + RESET) : text;
	}

	private String dim(String text) {
		return mode == Mode.ANSI ? (DIM + text + RESET) : text;
	}

	private String cyan(String text) {
		return mode == Mode.ANSI ? (CYAN + text + RESET) : text;
	}
}
