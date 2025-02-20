package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.API;

import java.nio.file.Path;

public interface IncrementalAPIExtractor {
	API refreshAPI(Path sources, ChangedFilesProvider.ChangedFiles changedFiles, API oldApi);
}
