package io.github.alien.roseau.extractors.incremental;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.extractors.APIExtractor;

import java.nio.file.Path;

/**
 * A complement to {@link APIExtractor} that allows "refreshing" an existing {@link API} based on some
 * {@link io.github.alien.roseau.extractors.incremental.ChangedFilesProvider.ChangedFiles}.
 */
public interface IncrementalAPIExtractor {
	/**
	 * Returns a fresh new instance of {@link API} that reuses the symbols declared in a previous API and incorporates the
	 * changes pointed by {@code changedFiles}.
	 *
	 * @param sources      the file or directory to use as source
	 * @param changedFiles the files that have been changed since {@code previousApi} was created
	 * @param previousApi  the previous API to refresh
	 * @return a fresh API instance incoporating the changes on top of deep copies of the previous API's symbols
	 */
	API refreshAPI(Path sources, ChangedFilesProvider.ChangedFiles changedFiles, API previousApi);
}
