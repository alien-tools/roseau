package io.github.alien.roseau.extractors.jdt;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.IncrementalAPIExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
public class IncrementalJdtTypesExtractor extends JdtTypesExtractor implements IncrementalAPIExtractor {
	@Override
	public LibraryTypes refreshAPI(Path sources, ChangedFiles changedFiles, LibraryTypes previousApi) {
		Preconditions.checkArgument(Files.exists(Objects.requireNonNull(sources)), "Invalid sources: " + sources);
		Objects.requireNonNull(changedFiles);
		Objects.requireNonNull(previousApi);

		// If nothing's changed, just return the old one
		if (changedFiles.hasNoChanges()) {
			return previousApi;
		}

		// Collect types that should be discarded from the previous API
		Set<Path> discarded = Sets.union(changedFiles.deletedFiles(), changedFiles.updatedFiles());

		// Collect files to be parsed
		List<Path> filesToParse = new ArrayList<>(
			Sets.union(changedFiles.updatedFiles(), changedFiles.createdFiles()));

		// Parse, collect, and merge the updated files
		TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();
		List<TypeDecl> newTypeDecls = Stream.concat(
			// Previous unchanged types
			previousApi.getAllTypes().stream()
				.filter(t -> !discarded.contains(t.getLocation().file())),
			// New re-parsed types
			parseTypes(filesToParse, sources, List.of(), typeRefFactory).stream()
		).toList();

		return new LibraryTypes(newTypeDecls);
	}
}
