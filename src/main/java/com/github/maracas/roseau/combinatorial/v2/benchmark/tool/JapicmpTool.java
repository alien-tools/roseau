package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.ToolResult;

import java.nio.file.Path;

public final class JapicmpTool extends AbstractTool {
	public JapicmpTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();
		// TODO: Implement Japicmp
		long executionTime = System.currentTimeMillis() - startTime;

		return new ToolResult(executionTime, false);
	}
}
