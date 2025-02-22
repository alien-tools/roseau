package com.github.maracas.roseau.extractors.jdt;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.CachedTypeReferenceFactory;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import com.github.maracas.roseau.extractors.ChangedFilesProvider;
import com.github.maracas.roseau.extractors.IncrementalAPIExtractor;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class IncrementalJdtAPIExtractor extends JdtAPIExtractor implements IncrementalAPIExtractor {
	@Override
	public API refreshAPI(Path sources, ChangedFilesProvider.ChangedFiles changedFiles, API previousApi) {
		Preconditions.checkArgument(Files.exists(Objects.requireNonNull(sources)), "Invalid sources: " + sources);
		Objects.requireNonNull(changedFiles);
		Objects.requireNonNull(previousApi);

		// If nothing's changed, just return the old one
		if (changedFiles.hasNoChanges()) {
			return previousApi;
		}

		// Collect types that should be discarded from the previous API
		Set<Path> discarded = Sets.union(changedFiles.deletedFiles(), changedFiles.updatedFiles());

		// Copying unchanged types to the new API
		List<TypeDecl> typeDecls = new ArrayList<>(previousApi.getAllTypes()
			.filter(t -> !discarded.contains(t.getLocation().file()))
			.map(TypeDecl::deepCopy)
			.toList());

		// Collect files to be parsed
		List<Path> filesToParse = new ArrayList<>(
			Sets.union(changedFiles.updatedFiles(), changedFiles.createdFiles()));

		// Parse, collect, and merge the updated files
		TypeReferenceFactory typeRefFactory = new CachedTypeReferenceFactory();
		typeDecls.addAll(parseTypes(filesToParse, sources, typeRefFactory));
		return new API(typeDecls, typeRefFactory);
	}
}
