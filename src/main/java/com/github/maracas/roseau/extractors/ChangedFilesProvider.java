package com.github.maracas.roseau.extractors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public interface ChangedFilesProvider {
	ChangedFiles getChangedFiles();

	record ChangedFiles(
		Set<Path> updatedFiles,
		Set<Path> deletedFiles,
		Set<Path> createdFiles
	)	{
		public static final ChangedFiles NO_CHANGES = new ChangedFiles(Set.of(), Set.of(), Set.of());

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
}
