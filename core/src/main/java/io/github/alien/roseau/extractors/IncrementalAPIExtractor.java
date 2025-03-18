package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.API;

import java.nio.file.Path;

public interface IncrementalAPIExtractor {
	API refreshAPI(Path sources, ChangedFilesProvider.ChangedFiles changedFiles, API previousApi);
}
