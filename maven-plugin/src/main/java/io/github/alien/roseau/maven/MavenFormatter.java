package io.github.alien.roseau.maven;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

/**
 * A formatter of {@link RoseauReport} that feeds a Maven-friendly report into a {@link Log}
 */
public class MavenFormatter implements BreakingChangesFormatter {
	private final Log log;

	private static final String COMPATIBLE = "✓";
	private static final String BREAKING = "✗";

	public MavenFormatter(Log log) {
		this.log = log;
	}

	@Override
	public String format(RoseauReport report) {
		List<BreakingChange> changes = report.getBreakingChanges();
		if (changes.isEmpty()) {
			log.info("No breaking changes found.");
		}

		int binaryBreaking = report.getBinaryBreakingChanges().size();
		int sourceBreaking = report.getSourceBreakingChanges().size();

		log.warn(String.format("Breaking Changes found: %d (%d binary-breaking, %d source-breaking)",
			changes.size(), binaryBreaking, sourceBreaking));

		report.getBreakingChanges().forEach(bc -> formatBreakingChange(bc));

		return "";
	}

	private void formatBreakingChange(BreakingChange bc) {
		String details = formatDetails(bc);
		log.warn(String.format("%s %s%s", formatSymbol(bc), formatKind(bc),
			details.isEmpty() ? "" : "[" + details + "]"));
		log.warn("  " + formatCompatibility(bc));
		log.warn("  " + formatLocation(bc));
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
			: "%s in %s".formatted(bc.impactedSymbol().getQualifiedName(), bc.impactedSymbol().getQualifiedName());
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
}
