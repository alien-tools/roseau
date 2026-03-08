package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.analysis.CachingApiAnalyzer;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An API augments {@link LibraryTypes} with analysis capabilities and symbol export information.
 */
public final class API extends CachingApiAnalyzer {
	/**
	 * The types, exported or not, declared in the library.
	 */
	private final LibraryTypes libraryTypes;
	private final TypeResolver typeResolver;

	public API(LibraryTypes libraryTypes, TypeResolver typeResolver) {
		Preconditions.checkNotNull(libraryTypes);
		Preconditions.checkNotNull(typeResolver);
		this.libraryTypes = libraryTypes;
		this.typeResolver = typeResolver;
	}

	public LibraryTypes getLibraryTypes() {
		return libraryTypes;
	}

	@Override
	public TypeResolver resolver() {
		return typeResolver;
	}

	@Override
	public boolean isExported(TypeDecl type) {
		return isModuleExported(type) && super.isExported(type);
	}

	public boolean isModuleExported(TypeDecl type) {
		return libraryTypes.getModule().isExporting(type.getPackageName());
	}

	/**
	 * Type declarations that are exported by the API.
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
		return libraryTypes.findType(qualifiedName)
			.filter(this::isExported);
	}

	public Library getLibrary() {
		return libraryTypes.getLibrary();
	}

	@Override
	public String toString() {
		return getExportedTypes().stream()
			.map(TypeDecl::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
