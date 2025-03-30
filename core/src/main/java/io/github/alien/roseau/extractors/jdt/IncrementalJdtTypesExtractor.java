package io.github.alien.roseau.extractors.jdt;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.IncrementalTypesExtractor;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A JDT-based incremental {@link LibraryTypes} extractor.
 * <br>
 * This implementation:
 * <ul>
 *   <li>Returns the previous API if no file has changed</li>
 *   <li>Discards deleted symbols</li>
 *   <li>Re-parses changed symbols</li>
 *   <li>Parses new files to extract new symbols</li>
 * </ul>
 */
public class IncrementalJdtTypesExtractor extends JdtTypesExtractor implements IncrementalTypesExtractor {
	@Override
	public LibraryTypes incrementalUpdate(LibraryTypes previousTypes, Library newVersion, ChangedFiles changedFiles) {
		Preconditions.checkNotNull(previousTypes);
		Preconditions.checkArgument(newVersion != null && newVersion.isSources());
		Preconditions.checkNotNull(changedFiles);

		// If nothing's changed, just return the old one
		if (changedFiles.hasNoChanges()) {
			return previousTypes;
		}

		Path oldRoot = previousTypes.getLibrary().getPath();
		Path newRoot = newVersion.getPath();

		// Collect types that should be discarded from the previous API
		Set<Path> discarded = Sets.union(resolve(oldRoot, changedFiles.deletedFiles()),
			resolve(oldRoot, changedFiles.updatedFiles()));

		// Collect files to be parsed
		List<Path> filesToParse = Sets.union(resolve(newRoot, changedFiles.updatedFiles()),
			resolve(newRoot, changedFiles.createdFiles())).stream().toList();

		// Parse, collect, and merge the updated files
		TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();
		List<TypeDecl> newTypeDecls = Stream.concat(
			// Previous unchanged types
			previousTypes.getAllTypes().stream()
				.filter(t -> !discarded.contains(t.getLocation().file())),
			// New re-parsed types
			parseTypes(newVersion, filesToParse, typeRefFactory).stream()
		).toList();

		return new LibraryTypes(newVersion, newTypeDecls);
	}

	private Set<Path> resolve(Path root, Set<Path> files) {
		return files.stream()
			.map(root::resolve)
			.collect(Collectors.toSet());
	}
}
