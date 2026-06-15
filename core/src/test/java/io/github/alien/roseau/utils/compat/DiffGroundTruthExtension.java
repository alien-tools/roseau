package io.github.alien.roseau.utils.compat;

import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.AssertionFailedError;

import java.util.List;
import java.util.stream.Collectors;

public final class DiffGroundTruthExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
	@Override
	public void beforeTestExecution(ExtensionContext context) {
		DiffGroundTruthRecorder.clear();
	}

	@Override
	public void afterTestExecution(ExtensionContext context) {
		DiffGroundTruthMode mode = DiffGroundTruthMode.current();
		List<DiffCompatibilityCase> cases = DiffGroundTruthRecorder.takeCases();
		if (!mode.isEnabled() || context.getExecutionException().isPresent()) {
			return;
		}

		Client client = context.getRequiredTestMethod().getAnnotation(Client.class);
		if (client == null || !client.verify()) {
			return;
		}

		List<String> mismatches = cases.stream()
			.map(compatibilityCase -> compare(compatibilityCase, client))
			.filter(report -> !report.isBlank())
			.toList();
		if (mismatches.isEmpty()) {
			return;
		}

		String report = "Java compatibility ground truth mismatch in %s:%n%s"
			.formatted(context.getDisplayName(), String.join(System.lineSeparator(), mismatches));
		if (mode == DiffGroundTruthMode.FAIL) {
			throw new AssertionFailedError(report);
		}
		System.err.println(report);
	}

	private static String compare(DiffCompatibilityCase compatibilityCase, Client client) {
		JavaCompatibilityOracle.JavaCompatibilityResult result = JavaCompatibilityOracle.check(compatibilityCase, client);

		boolean setupFailed = result.v1Compilation().failed()
			|| result.v2Compilation().failed()
			|| result.clientV1Compilation().failed();
		boolean sourceMismatch = result.setupSucceeded()
			&& compatibilityCase.roseauSourceBreaking() != result.observedSourceBreaking();
		boolean binaryBaselineFailed = result.setupSucceeded() && result.clientV1Execution().failed();
		boolean binaryMismatch = result.clientV1Execution().succeeded()
			&& compatibilityCase.roseauBinaryBreaking() != result.observedBinaryBreaking();

		if (!setupFailed && !sourceMismatch && !binaryBaselineFailed && !binaryMismatch) {
			return "";
		}

		StringBuilder report = new StringBuilder();
		report.append(" - expected from javac/JVM: ").append(expectedVerdict(result)).append(System.lineSeparator());
		report.append(" - found by Roseau: ").append(verdict(compatibilityCase.roseauSourceBreaking(),
			compatibilityCase.roseauBinaryBreaking())).append(System.lineSeparator());
		report.append(" - Roseau breaking changes: ").append(roseauBreakingChanges(compatibilityCase)).append(System.lineSeparator());

		appendCompilationDetail(report, "v1 library compilation", result.v1Compilation());
		appendCompilationDetail(report, "v2 library compilation", result.v2Compilation());
		appendCompilationDetail(report, "client compilation against v1", result.clientV1Compilation());
		if (!result.setupSucceeded()) {
			return report.toString();
		}

		appendCompilationDetail(report, "client compilation against v2", result.clientV2Compilation());
		appendExecutionDetail(report, "client execution against v1", result.clientV1Execution());
		appendExecutionDetail(report, "client execution against v2", result.clientV2Execution());

		return report.toString();
	}

	private static String expectedVerdict(JavaCompatibilityOracle.JavaCompatibilityResult result) {
		if (!result.setupSucceeded()) {
			return "unavailable (v1, v2, or client-v1 compilation failed)";
		}
		if (result.clientV1Execution().failed()) {
			return "source=%s, binary=unavailable (client compiled against v1 does not run against v1)"
				.formatted(result.observedSourceBreaking() ? "breaking" : "compatible");
		}
		return verdict(result.observedSourceBreaking(), result.observedBinaryBreaking());
	}

	private static String verdict(boolean sourceBreaking, boolean binaryBreaking) {
		return switch ((sourceBreaking ? 2 : 0) + (binaryBreaking ? 1 : 0)) {
			case 0 -> "compatible";
			case 1 -> "binary-breaking";
			case 2 -> "source-breaking";
			case 3 -> "source-and-binary-breaking";
			default -> throw new IllegalStateException("Unexpected compatibility verdict");
		};
	}

	private static String roseauBreakingChanges(DiffCompatibilityCase compatibilityCase) {
		if (compatibilityCase.roseauBreakingChanges().isEmpty()) {
			return "none";
		}
		return compatibilityCase.roseauBreakingChanges().stream()
			.map(bc -> "%s %s -> %s".formatted(bc.kind(), bc.impactedType().getQualifiedName(),
				bc.impactedSymbol().getQualifiedName()))
			.collect(Collectors.joining("; "));
	}

	private static void appendCompilationDetail(StringBuilder report, String step,
	                                            JavaCompatibilityOracle.CompilationResult result) {
		report.append(" - ").append(step).append(": ");
		if (!result.ran()) {
			report.append("skipped");
		} else {
			report.append(result.succeeded() ? "succeeded" : "failed");
		}
		String diagnostics = diagnostics(result.diagnostics());
		if (!diagnostics.isBlank()) {
			report.append(" (").append(diagnostics).append(")");
		}
		report.append(System.lineSeparator());
	}

	private static void appendExecutionDetail(StringBuilder report, String step,
	                                          JavaCompatibilityOracle.ExecutionResult result) {
		report.append(" - ").append(step).append(": ");
		if (!result.ran()) {
			report.append("skipped");
		} else {
			report.append(result.succeeded() ? "succeeded" : "failed")
				.append(" (exit code ").append(result.exitCode()).append(")");
		}
		String output = output(result);
		if (!output.isBlank()) {
			report.append(" (").append(output).append(")");
		}
		report.append(System.lineSeparator());
	}

	private static String diagnostics(List<String> diagnostics) {
		return diagnostics.stream()
			.map(DiffGroundTruthExtension::oneLine)
			.collect(Collectors.joining("; "));
	}

	private static String output(JavaCompatibilityOracle.ExecutionResult result) {
		if (!result.ran()) {
			return oneLine(result.output());
		}
		if (result.succeeded()) {
			return "";
		}
		String output = oneLine(result.output());
		return output.isBlank() ? "exit code " + result.exitCode() : output;
	}

	private static String oneLine(String value) {
		String line = value.replace(System.lineSeparator(), " ").trim();
		return line.length() <= 500 ? line : line.substring(0, 497) + "...";
	}
}
