package io.github.alien.roseau.extractors.incremental;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.extractors.TypesExtractor;

/**
 * A complement to {@link TypesExtractor} that allows incrementally updating an existing {@link LibraryTypes} based on
 * some {@link ChangedFiles}.
 */
public interface IncrementalTypesExtractor {
	/**
	 * Returns a fresh new instance of {@link LibraryTypes} that reuses the symbols declared in a previous instance and
	 * incorporates the changes pointed by {@code changedFiles}.
	 *
	 * @param previousTypes the previously extracted types to update
	 * @param newVersion    the new version of the library
	 * @param changedFiles  the files that have been updated since {@code previousTypes} was created
	 * @return a fresh instance incorporating new changes on top of unchanged types
	 */
	LibraryTypes incrementalUpdate(LibraryTypes previousTypes, Library newVersion, ChangedFiles changedFiles);
}
