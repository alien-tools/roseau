package io.github.alien.roseau.combinatorial.v2.benchmark.tool;

import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.extractors.asm.AsmAPIExtractor;

import java.nio.file.Path;

public final class RoseauTool extends AbstractTool {
	public RoseauTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();

		var v1Api = new AsmAPIExtractor().extractAPI(v1Path);
		var v2Api = new AsmAPIExtractor().extractAPI(v2Path);

		APIDiff diff = new APIDiff(v1Api, v2Api);
		diff.diff();

		var breakingChanges = diff.getBreakingChanges();
		var isBinaryBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isBinaryBreaking());
		var isSourceBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isSourceBreaking());

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Roseau", executionTime, isBinaryBreaking, isSourceBreaking);
	}
}
