package com.github.maracas.roseau.combinatorial.writer;

import com.github.maracas.roseau.combinatorial.utils.ExplorerUtils;

import java.nio.file.Path;

abstract class AbstractWriter {
	protected final Path outputDir;

	protected AbstractWriter(Path outputDir) {
		this.outputDir = outputDir;
	}

	public boolean createOutputHierarchy() {
		return ExplorerUtils.cleanOrCreateDirectory(outputDir);
	}
}
