package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.analysis.CachingAPIAnalyzer;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An API augments {@link LibraryTypes} with analysis capabilities and symbol export information.
 */
public class API extends CachingAPIAnalyzer {
	private final LibraryTypes libraryTypes;

	public API(LibraryTypes libraryTypes, TypeResolver resolver) {
		super(resolver);
		Preconditions.checkNotNull(libraryTypes);
		this.libraryTypes = libraryTypes;
	}

	public LibraryTypes getLibraryTypes() {
		return libraryTypes;
	}

	/**
	 * Type declaration that are exported by the API.
	 *
	 * @return The list of exported {@link TypeDecl}
	 */
	public List<TypeDecl> getExportedTypes() {
		return libraryTypes.getAllTypes().stream()
			.filter(this::isExported)
			.toList();
	}

	/**
	 * Returns the exported type in the API with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 */
	public Optional<TypeDecl> findExportedType(String qualifiedName) {
		return getExportedTypes().stream()
			.filter(type -> type.getQualifiedName().equals(qualifiedName))
			.findFirst();
	}

	@Override
	public String toString() {
		return getExportedTypes().stream()
			.map(TypeDecl::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
