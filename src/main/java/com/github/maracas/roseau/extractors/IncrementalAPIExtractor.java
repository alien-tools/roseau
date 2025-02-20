package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.API;

import java.nio.file.Path;
import java.util.Set;

public interface IncrementalAPIExtractor {

    /**
     *
     * @param sources Root path of the sources
     * @param deleted Deleted java files
     * @param updated Updated java file
     * @param created Created java files
     * @param oldApi Previous API version
     * @return an updated API
     */
    API refreshAPI(Path sources, Set<Path> deleted, Set<Path> updated, Set<Path> created, API oldApi);

}
