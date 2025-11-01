package io.github.alien.roseau.extractors.jdt;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.IncrementalTypesExtractor;

import java.nio.file.Path;
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
 *   <li>Reparses changed symbols</li>
 *   <li>Parses new files to extract new symbols</li>
 * </ul>
 */
public final class IncrementalJdtTypesExtractor implements IncrementalTypesExtractor {
	private final JdtTypesExtractor extractor;

	public IncrementalJdtTypesExtractor(JdtTypesExtractor extractor) {
		this.extractor = Preconditions.checkNotNull(extractor);
	}

	@Override
	public LibraryTypes incrementalUpdate(LibraryTypes previousTypes, Library newVersion, ChangedFiles changedFiles) {
		Preconditions.checkNotNull(previousTypes);
		Preconditions.checkNotNull(newVersion);
		Preconditions.checkNotNull(changedFiles);

		// If nothing's changed, just return the old one
		if (changedFiles.hasNoChanges()) {
			return previousTypes;
		}

		// Collect types that should be discarded from the previous API
		Set<Path> discarded = Sets.union(changedFiles.deletedFiles(),
			changedFiles.updatedFiles());

		// Collect files to be parsed
		Set<Path> filesToParse = Sets.union(changedFiles.updatedFiles(), changedFiles.createdFiles()).stream()
			.map(newVersion.getLocation()::resolve)
			.collect(Collectors.toSet());

		Set<TypeDecl> unchanged = previousTypes.getAllTypes().stream()
			.filter(t -> !discarded.contains(t.getLocation().file()))
			.collect(Collectors.toSet());

		// Parse, collect, and merge the updated files
		Set<TypeDecl> newTypeDecls = Stream.concat(
			unchanged.stream(),
			extractor.parseTypes(newVersion, filesToParse).types().stream()
		).collect(Collectors.toSet());

		// FIXME: the module declaration might have changed between the two versions
		return new LibraryTypes(newVersion, previousTypes.getModule(), newTypeDecls);
	}
}
