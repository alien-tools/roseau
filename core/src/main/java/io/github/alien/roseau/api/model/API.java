package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.analysis.CachingAPIAnalyzer;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class API extends CachingAPIAnalyzer {
	private final LibraryTypes types;

	public API(LibraryTypes types, TypeResolver resolver) {
		super(resolver);
		this.types = types;
	}

	public LibraryTypes getTypes() {
		return types;
	}

	/**
	 * Type declaration that are exported by the API.
	 *
	 * @return The list of exported {@link TypeDecl}
	 */
	public List<TypeDecl> getExportedTypes() {
		return types.getAllTypes().stream()
			.filter(this::isExported)
			.toList();
	}

	/**
	 * Returns the class declarations exported by the API.
	 *
	 * @return The list of exported {@link ClassDecl}
	 */
	public List<ClassDecl> getExportedClasses() {
		return getExportedTypes().stream()
			.filter(ClassDecl.class::isInstance)
			.map(ClassDecl.class::cast)
			.toList();
	}

	/**
	 * Returns the interface declarations exported by the API.
	 *
	 * @return The list of exported {@link InterfaceDecl}
	 */
	public List<InterfaceDecl> getExportedInterfaces() {
		return getExportedTypes().stream()
			.filter(InterfaceDecl.class::isInstance)
			.map(InterfaceDecl.class::cast)
			.toList();
	}

	public Optional<TypeDecl> findType(String qualifiedName) {
		return types.findType(qualifiedName);
	}

	/**
	 * Returns the exported type in the API with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 */
	public Optional<TypeDecl> findExportedType(String qualifiedName) {
		return findType(qualifiedName).filter(this::isExported);
	}

	@Override
	public String toString() {
		return getExportedTypes().stream()
			.map(TypeDecl::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
