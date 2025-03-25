package io.github.alien.roseau.combinatorial.v2.benchmark.tool;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import io.github.alien.roseau.diff.APIDiff;

import java.nio.file.Path;

public final class RoseauTool extends AbstractTool {
	private API v1Api;
	private API v2Api;

	public RoseauTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	public void setApis(API v1Api, API v2Api) {
		this.v1Api = v1Api;
		this.v2Api = v2Api;
	}

	@Override
	public ToolResult detectBreakingChanges() {
		if (v1Api == null || v2Api == null) return null;

		long startTime = System.currentTimeMillis();

		APIDiff diff = new APIDiff(v1Api, v2Api);
		diff.diff();

		var breakingChanges = diff.getBreakingChanges();
		var isBinaryBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isBinaryBreaking());
		var isSourceBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isSourceBreaking());

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Roseau", executionTime, isBinaryBreaking, isSourceBreaking);
	}
}
