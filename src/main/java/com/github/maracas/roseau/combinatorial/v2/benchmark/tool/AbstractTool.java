package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import java.nio.file.Path;

public sealed abstract class AbstractTool permits JapicmpTool, RevapiTool, RoseauTool {
	protected final Path v1Path;
	protected final Path v2Path;

	public AbstractTool(Path v1Path, Path v2Path) {
		this.v1Path = v1Path;
		this.v2Path = v2Path;
	}

	public abstract void detectBreakingChanges();
}
