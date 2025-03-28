package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.analysis.CachingAPIAnalyzer;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An API holds a set of {@link Symbol} and provides convenience methods to access {@link TypeDecl} declarations. APIs
 * are immutable and can be serialized/unserialized from/to JSON. To enable type resolution, an API holds
 * <strong>all</strong> the types it declares, including non-exported ones. {@link LibraryTypes} instances have limited
 * analysis
 * capabilities and must be transformed into {@link API} to enable type resolution and most analyses
 * ({@link #toAPI(TypeResolver)}).
 */
public class API extends CachingAPIAnalyzer {
	private final LibraryTypes libraryTypes;

	public API(LibraryTypes libraryTypes, TypeResolver resolver) {
		super(resolver);
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
