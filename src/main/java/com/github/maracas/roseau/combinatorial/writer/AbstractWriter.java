package com.github.maracas.roseau.combinatorial.writer;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

abstract class AbstractWriter {
	protected final Path outputDir;

	protected AbstractWriter(Path outputDir) {
		this.outputDir = outputDir;
	}

	public void createOutputHierarchy() {
		if (this.outputDir.toString().isBlank()) return;

		try {
			File outputDirFile = outputDir.toFile();
			if (outputDirFile.exists()) {
				try {
					FileUtils.cleanDirectory(outputDirFile);
					return;
				} catch (IOException e) {
					throw new WriterException("Error cleaning output directory: " + e.getMessage());
				}
			}

			if (!outputDirFile.mkdirs())
				throw new WriterException("Error creating output directory");
		} catch (SecurityException e) {
			throw new WriterException("Error creating output directory: " + e.getMessage());
		}
	}
}
