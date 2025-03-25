package io.github.alien.roseau.combinatorial.v2.benchmark.tool;

import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import org.revapi.*;
import org.revapi.base.CollectingReporter;
import org.revapi.base.FileArchive;
import org.revapi.java.JavaApiAnalyzer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RevapiTool extends AbstractTool {
	public RevapiTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();

		var revapi = Revapi.builder()
				.withAnalyzers(JavaApiAnalyzer.class)
				.withReporters(CollectingReporter.class)
				.build();

		var v1Archive = new FileArchive(v1Path.toFile());
		var v1Api = API.of(v1Archive).build();
		var v2Archive = new FileArchive(v2Path.toFile());
		var v2Api = API.of(v2Archive).build();

		var analysisContext = AnalysisContext.builder()
				.withOldAPI(v1Api)
				.withNewAPI(v2Api)
				.build();

		List<Difference> breakingChanges = new ArrayList<>();
		try (var results = revapi.analyze(analysisContext)) {
			var collectingReporters = results.getExtensions()
					.getReporters().keySet().stream()
					.filter(r -> r.getInstance() instanceof CollectingReporter)
					.map(r -> (CollectingReporter) r.getInstance())
					.toList();

			collectingReporters.forEach(reporter ->
					reporter.getReports().forEach(report ->
							report.getDifferences().forEach(difference -> {
								var isBinaryBreaking = difference.classification.get(CompatibilityType.BINARY).equals(DifferenceSeverity.BREAKING)
										|| difference.classification.get(CompatibilityType.BINARY).equals(DifferenceSeverity.POTENTIALLY_BREAKING);
								var isSourceBreaking = difference.classification.get(CompatibilityType.SOURCE).equals(DifferenceSeverity.BREAKING)
										|| difference.classification.get(CompatibilityType.SOURCE).equals(DifferenceSeverity.POTENTIALLY_BREAKING);

								if (isBinaryBreaking || isSourceBreaking) {
									breakingChanges.add(difference);
								}
							})
					)
			);
		} catch (Exception ignored) {}

		var isBinaryBreaking = breakingChanges.stream().anyMatch(diff ->
				diff.classification.get(CompatibilityType.BINARY).equals(DifferenceSeverity.BREAKING)
						|| diff.classification.get(CompatibilityType.BINARY).equals(DifferenceSeverity.POTENTIALLY_BREAKING)
		);
		var isSourceBreaking = breakingChanges.stream().anyMatch(diff ->
				diff.classification.get(CompatibilityType.SOURCE).equals(DifferenceSeverity.BREAKING)
						|| diff.classification.get(CompatibilityType.SOURCE).equals(DifferenceSeverity.POTENTIALLY_BREAKING)
		);

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Revapi", executionTime, isBinaryBreaking, isSourceBreaking);
	}
}
