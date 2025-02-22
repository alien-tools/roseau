package com.github.maracas.roseau.extractors;

import java.nio.file.Path;
import java.util.Set;

public interface ChangedFilesProvider {
	ChangedFiles getChangedFiles();

	record ChangedFiles(
		Set<Path> updatedFiles,
		Set<Path> deletedFiles,
		Set<Path> createdFiles
	)	{}
}
