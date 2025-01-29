package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import java.nio.file.Path;

public final class RoseauTool extends AbstractTool {
	public RoseauTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public void detectBreakingChanges() {
		System.out.println("--------------------------------------");
		System.out.println("Detecting Breaking Changes with Roseau");
		System.out.println("\tV1 Path: " + v1Path);
		System.out.println("\tV2 Path: " + v2Path);
		System.out.println("--------------------------------------");
	}
}
