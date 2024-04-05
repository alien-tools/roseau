package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;

import java.nio.file.Path;

public interface APIExtractor {
	API extractAPI(Path sources);
}
