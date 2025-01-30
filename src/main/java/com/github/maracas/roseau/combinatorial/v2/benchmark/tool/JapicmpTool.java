package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.ToolResult;

import java.nio.file.Path;

public final class JapicmpTool extends AbstractTool {
	public JapicmpTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		System.out.println("--------------------------------------");
		System.out.println("Detecting Breaking Changes with Japicmp");
		System.out.println("\tV1 Path: " + v1Path);
		System.out.println("\tV2 Path: " + v2Path);
		System.out.println("--------------------------------------");

		return new ToolResult(0.0f, false);
	}
}
