package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.ToolResult;

import java.nio.file.Path;

public final class RevapiTool extends AbstractTool {
	public RevapiTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();
		// TODO: Implement Revapi
		long executionTime = System.currentTimeMillis() - startTime;

		return new ToolResult(executionTime, false);
	}
}
