package io.github.alien.roseau.combinatorial.v2.benchmark.tool;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;

import java.nio.file.Path;

public final class RoseauTool extends AbstractTool {
	public RoseauTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();

		var v1Api = new AsmTypesExtractor().extractTypes(Library.of(v1Path));
		var v2Api = new AsmTypesExtractor().extractTypes(Library.of(v2Path));

		APIDiff diff = new APIDiff(v1Api.toAPI(), v2Api.toAPI());
		diff.diff();

		var breakingChanges = diff.getBreakingChanges();
		var isBinaryBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isBinaryBreaking());
		var isSourceBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isSourceBreaking());

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Roseau", executionTime, isBinaryBreaking, isSourceBreaking);
	}
}
