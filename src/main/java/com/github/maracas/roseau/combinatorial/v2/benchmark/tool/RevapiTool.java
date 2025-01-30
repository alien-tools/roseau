package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.ToolResult;

import java.nio.file.Path;

public final class RevapiTool extends AbstractTool {
	public RevapiTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		System.out.println("--------------------------------------");
		System.out.println("Detecting Breaking Changes with Revapi");

		long startTime = System.currentTimeMillis();
		// TODO: Implement Revapi
		long executionTime = System.currentTimeMillis() - startTime;

		System.out.println("--------------------------------------");

		return new ToolResult(executionTime, false);
	}
}
