package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.analysis.ApiAnalyzer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A resolved API snapshot with analysis capabilities.
 */
public final class API {
	/**
	 * The types, exported or not, declared in the library.
	 */
	private final LibraryTypes libraryTypes;
	private final ApiAnalyzer analyzer;
	private final Map<String, TypeDecl> exportedTypes;

	public API(LibraryTypes libraryTypes, ApiAnalyzer analyzer) {
		Preconditions.checkNotNull(libraryTypes);
		this.libraryTypes = libraryTypes;
		this.analyzer = Preconditions.checkNotNull(analyzer);
		this.exportedTypes = libraryTypes.getAllTypes().stream()
			.filter(analyzer::isExported)
			.collect(ImmutableSortedMap.toImmutableSortedMap(
				Comparator.naturalOrder(),
				Symbol::getQualifiedName,
				Function.identity()
			));
	}

	public LibraryTypes getLibraryTypes() {
		return libraryTypes;
	}

	public ApiAnalyzer analyzer() {
		return analyzer;
	}

	/**
	 * Type declarations that are exported by the API.
	 *
	 * @return The list of exported {@link TypeDecl}
	 */
	public List<TypeDecl> getExportedTypes() {
		return List.copyOf(exportedTypes.values());
	}

	/**
	 * Returns the exported type in the API with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 */
	public Optional<TypeDecl> findExportedType(String qualifiedName) {
		return Optional.ofNullable(exportedTypes.get(qualifiedName));
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

	@Override
	public boolean equals(Object obj) {
		// FIXME: This structural equality check isn't fully accurate.
		// If classpaths are different between the two APIs, the fully resolved API model might
		// be different and the API might not be equal.
		if (this == obj) {
			return true;
		}
		return obj instanceof API other
			&& Objects.equals(libraryTypes.getModule(), other.libraryTypes.getModule())
			&& Objects.equals(getExportedTypes(), other.getExportedTypes());
	}

	@Override
	public int hashCode() {
		return Objects.hash(libraryTypes.getModule(), getExportedTypes());
	}
}
