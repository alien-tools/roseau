package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.ToolResult;
import org.revapi.API;
import org.revapi.AnalysisContext;
import org.revapi.Revapi;
import org.revapi.base.FileArchive;

import java.nio.file.Path;

public final class RevapiTool extends AbstractTool {
	public RevapiTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();

		var revapi = Revapi.builder()
				.withAllExtensionsFromThreadContextClassLoader()
				.build();

		var v1Archive = new FileArchive(v1Path.toFile());
		var v1Api = API.of(v1Archive).build();
		var v2Archive = new FileArchive(v2Path.toFile());
		var v2Api = API.of(v2Archive).build();

		var analysisContext = AnalysisContext.builder()
				.withOldAPI(v1Api)
				.withNewAPI(v2Api)
				.build();

		boolean isBreaking = false;
		try (var results = revapi.analyze(analysisContext)) {
			isBreaking = !results.isSuccess();
		} catch (Exception ignored) {
		}

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult(executionTime, isBreaking);
	}
}
