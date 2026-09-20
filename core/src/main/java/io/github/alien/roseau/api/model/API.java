package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.analysis.ApiAnalyzer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A resolved API snapshot with analysis capabilities.
 */
public final class API {
	private final LibraryTypes libraryTypes;
	private final ApiAnalyzer analyzer;
	private final Map<String, TypeDecl> exportedTypes;
	private final List<TypeDecl> exportedTypesList;

	public API(LibraryTypes libraryTypes, ApiAnalyzer analyzer) {
		Preconditions.checkNotNull(libraryTypes);
		Preconditions.checkNotNull(analyzer);
		this.libraryTypes = libraryTypes;
		this.analyzer = analyzer;
		this.exportedTypes = libraryTypes.getAllTypes().stream()
			.filter(analyzer::isExported)
			.collect(ImmutableSortedMap.toImmutableSortedMap(
				Comparator.naturalOrder(),
				Symbol::getQualifiedName,
				Function.identity()
			));
		this.exportedTypesList = ImmutableList.copyOf(exportedTypes.values());
	}

	/**
	 * The types, exported or not, declared in the library.
	 *
	 * @return the library types
	 */
	public LibraryTypes getLibraryTypes() {
		return libraryTypes;
	}

	/**
	 * An {@link ApiAnalyzer} for this API.
	 *
	 * @return the analyzer
	 */
	public ApiAnalyzer analyzer() {
		return analyzer;
	}

	/**
	 * Type declarations that are exported by the API.
	 *
	 * @return the list of exported {@link TypeDecl}
	 */
	public List<TypeDecl> getExportedTypes() {
		return exportedTypesList;
	}

	/**
	 * Returns the exported type in the API with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return an {@link Optional} indicating whether the type was found
	 */
	public Optional<TypeDecl> findExportedType(String qualifiedName) {
		return Optional.ofNullable(exportedTypes.get(qualifiedName));
	}

	/**
	 * Returns the qualified names of the types that could not be resolved while analyzing this API. A non-empty
	 * result means the API model is incomplete and that verdicts may be inaccurate.
	 *
	 * @return the qualified names of the unresolved types
	 */
	public Set<String> getUnresolvedTypes() {
		return analyzer.resolver().getUnresolvedTypes();
	}

	/**
	 * The {@link Library} this API was extracted from.
	 *
	 * @return the library
	 */
	public Library getLibrary() {
		return libraryTypes.getLibrary();
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
