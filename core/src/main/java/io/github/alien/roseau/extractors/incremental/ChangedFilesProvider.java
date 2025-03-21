package io.github.alien.roseau.extractors.incremental;

/**
 * A provider for detecting and representing changes in a collection of files. Implementations of this interface
 * determine which source to watch and which files have been created, updated, or deleted.
 */
public interface ChangedFilesProvider {
	/**
	 * Retrieves the list of changed files.
	 *
	 * @return the changed files
	 */
	ChangedFiles getChangedFiles();

}
