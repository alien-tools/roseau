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
	private final Set<ExtractionGap> extractionGaps;

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
		this.extractionGaps = apiExtractionGaps();
	}

	/*
	 * A gap matters if it can affect the exported API: its declaring type is exported, or is a supertype of an
	 * exported type since inherited members are part of the API. Gaps whose declaring type is absent from the model
	 * (lost type declarations) and file-level gaps are conservatively kept.
	 */
	private Set<ExtractionGap> apiExtractionGaps() {
		Set<ExtractionGap> gaps = libraryTypes.getExtractionGaps();
		if (gaps.isEmpty()) {
			return ImmutableSortedSet.of();
		}

		Set<String> apiTypes = gaps.stream().anyMatch(this::isInternalGap)
			? exportedTypesAndSuperTypes()
			: exportedTypes.keySet();
		return gaps.stream()
			.filter(gap -> !isInternalGap(gap) || apiTypes.contains(gap.declaringType()))
			.collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
	}

	private boolean isInternalGap(ExtractionGap gap) {
		return gap.declaringType() != null && !exportedTypes.containsKey(gap.declaringType()) &&
			libraryTypes.findType(gap.declaringType()).isPresent();
	}

	// Only walks the library's own types: the resolver records the types it cannot resolve
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
	 * of the {@link #getExtractionGaps() extraction gaps}. A non-empty result means the API model is incomplete and
	 * that verdicts may be inaccurate.
	 *
	 * @return the qualified names of the unresolved types
	 */
	public Set<String> getUnresolvedTypes() {
		Set<String> unresolved = analyzer.resolver().getUnresolvedTypes();
		if (extractionGaps.isEmpty()) {
			return unresolved;
		}
		return ImmutableSortedSet.<String>naturalOrder()
			.addAll(unresolved)
			.addAll(extractionGaps.stream().map(ExtractionGap::reference).filter(Objects::nonNull).iterator())
			.build();
	}

	/**
	 * Returns the declarations that could not be extracted faithfully and may affect the exported API: those of
	 * exported types and of their supertypes, and those whose declaring type is missing from the model. A non-empty
	 * result means the API model is incomplete and that verdicts may be inaccurate.
	 *
	 * @return the extraction gaps that may affect the API, in a deterministic order
	 * @see LibraryTypes#getExtractionGaps()
	 */
	public Set<ExtractionGap> getExtractionGaps() {
		return extractionGaps;
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
