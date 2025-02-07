package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.v2.benchmark.result.ToolResult;
import com.github.maracas.roseau.diff.APIDiff;

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
		var isBreaking = !diff.getBreakingChanges().isEmpty();

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Roseau", executionTime, isBreaking);
	}
}
