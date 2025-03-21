package io.github.alien.roseau.extractors.incremental;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * An abstraction for representing updated, deleted, and created files between two versions of a library.
 *
 * @param updatedFiles the set of updated files
 * @param deletedFiles the set of deleted files
 * @param createdFiles the set of created files
 */
public record ChangedFiles(
	Set<Path> updatedFiles,
	Set<Path> deletedFiles,
	Set<Path> createdFiles
) {
	/**
	 * A convenience instance representing no changes in the source
	 */
	public static final ChangedFiles NO_CHANGES = new ChangedFiles(Set.of(), Set.of(), Set.of());

	/**
	 * Creates a new list of changed files.
	 *
	 * @throws IllegalArgumentException if any set is null or if a given file belongs to two sets
	 */
	public ChangedFiles {
		Preconditions.checkArgument(
			Sets.union(Objects.requireNonNull(updatedFiles),
				Sets.union(Objects.requireNonNull(deletedFiles), Objects.requireNonNull(createdFiles))).size() ==
				updatedFiles.size() + deletedFiles.size() + createdFiles.size(),
			"A file cannot be in two states");
	}

	public boolean hasNoChanges() {
		return equals(NO_CHANGES);
	}
}
