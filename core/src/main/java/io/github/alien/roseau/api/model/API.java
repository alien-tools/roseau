package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.analysis.ApiAnalyzer;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
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
	private final Map<String, Set<String>> extractionUnresolvedTypes;

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
		this.extractionUnresolvedTypes = computeExtractionUnresolvedTypes();
	}

	// Inherited members are part of the API, so the supertypes of exported types matter too
	private Map<String, Set<String>> computeExtractionUnresolvedTypes() {
		Map<String, Set<String>> unresolved = libraryTypes.getUnresolvedTypes();
		if (unresolved.isEmpty()) {
			return unresolved;
		}

		Set<String> apiTypes = exportedTypesAndSuperTypes();
		return unresolved.entrySet().stream()
			.filter(entry -> apiTypes.contains(entry.getKey()))
			.collect(ImmutableSortedMap.toImmutableSortedMap(
				Comparator.naturalOrder(), Map.Entry::getKey, Map.Entry::getValue));
	}

	// Only walks the library's types: the resolver would record the classpath types it cannot resolve
	private Set<String> exportedTypesAndSuperTypes() {
		Set<String> types = new HashSet<>(exportedTypes.keySet());
		Deque<TypeDecl> toVisit = new ArrayDeque<>(exportedTypes.values());
		while (!toVisit.isEmpty()) {
			TypeDecl type = toVisit.pop();
			List<TypeReference<?>> superTypes = new ArrayList<>(type.getImplementedInterfaces());
			if (type instanceof ClassDecl cls && cls.getSuperClass() != null) {
				superTypes.add(cls.getSuperClass());
			}
			superTypes.stream()
				.map(TypeReference::getQualifiedName)
				.filter(types::add)
				.forEach(name -> libraryTypes.findType(name).ifPresent(toVisit::push));
		}
		return types;
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
	 * Returns the qualified names of the types that could not be resolved while analyzing this API, including those
	 * the extractor could not resolve in the API's declarations. A non-empty result means the API model is incomplete
	 * and that verdicts may be inaccurate.
	 *
	 * @return the qualified names of the unresolved types
	 * @see #getExtractionUnresolvedTypes()
	 */
	public Set<String> getUnresolvedTypes() {
		Set<String> unresolved = analyzer.resolver().getUnresolvedTypes();
		if (extractionUnresolvedTypes.isEmpty()) {
			return unresolved;
		}
		return ImmutableSortedSet.<String>naturalOrder()
			.addAll(unresolved)
			.addAll(extractionUnresolvedTypes.values().stream().flatMap(Set::stream).iterator())
			.build();
	}

	/**
	 * Returns the types the extractor could not resolve in the declarations of exported types and of their supertypes.
	 * These declarations are missing from the API model or are distorted.
	 *
	 * @return the qualified names of the unresolved types, indexed by the qualified name of their declaring type
	 * @see LibraryTypes#getUnresolvedTypes()
	 */
	public Map<String, Set<String>> getExtractionUnresolvedTypes() {
		return extractionUnresolvedTypes;
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
