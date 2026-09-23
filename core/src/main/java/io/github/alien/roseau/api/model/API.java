package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
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

	/**
	 * Two APIs are equal only when diffing them is guaranteed to report no breaking change. An API is fully
	 * determined by the types it was extracted from and by the classpath they resolve against, so two APIs are equal
	 * when they share a module, every declaration they hold, exported or not, and the classpath they were resolved
	 * against. Source locations are not part of declarations, and so not part of this either.
	 * <p>
	 * Comparing what clients can observe instead is not enough: what the differ reports also depends on declarations
	 * clients never see, such as the package-private abstract methods a type inherits from an internal class, or the
	 * hierarchy of a thrown exception that comes from the classpath. Any such difference makes two APIs unequal,
	 * even when it ends up changing nothing, which merely costs a diff.
	 * <p>
	 * The classpath is compared as the list of files it resolves to, not by their content: two APIs resolved against
	 * the same paths are assumed to have seen the same types there.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof API other
			&& Objects.equals(libraryTypes.getModule(), other.libraryTypes.getModule())
			&& Objects.equals(exportedTypes.keySet(), other.exportedTypes.keySet())
			&& Iterables.elementsEqual(libraryTypes.getAllTypes(), other.libraryTypes.getAllTypes())
			&& Objects.equals(getLibrary().getClasspath(), other.getLibrary().getClasspath());
	}

	@Override
	public int hashCode() {
		// Deliberately coarse: equality compares every declaration, which is far too much to hash on every lookup.
		// Equal APIs export the same names under the same module, so this stays consistent.
		return Objects.hash(libraryTypes.getModule(), exportedTypes.keySet());
	}
}
