package io.github.alien.roseau.combinatorial.writer;

import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;

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
