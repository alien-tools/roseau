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

	/**
	 * Two APIs are equal when they expose the same thing to clients: the same module, the same exported types, and,
	 * for each of them, the same surface.
	 * <p>
	 * What a type <em>declares</em> is only part of that surface. The members and supertypes it inherits are just as
	 * visible, and they can come from a type this library does not export, or from the classpath — so a type can gain
	 * or lose members without its own declaration changing at all. Its declarations are compared too, and in full:
	 * package-private members look invisible but decide whether clients can write a concrete subclass.
	 * <p>
	 * A classpath is deliberately not part of this. Changing one is only a <em>potential</em> API change: whether it is
	 * an actual one shows up in the surface it resolves to, which is what is compared here.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof API other
			&& Objects.equals(libraryTypes.getModule(), other.libraryTypes.getModule())
			&& Objects.equals(exportedTypes.keySet(), other.exportedTypes.keySet())
			&& exportedTypes.values().stream().allMatch(type -> exposesTheSameAs(type, other));
	}

	/**
	 * Whether {@code type} exposes to clients exactly what the type of the same name exposes in {@code other}.
	 */
	private boolean exposesTheSameAs(TypeDecl type, API other) {
		TypeDecl counterpart = other.exportedTypes.get(type.getQualifiedName());
		return type.equals(counterpart)
			// Supertypes are compared transitively: clients can cast to any of them, and one can appear or disappear
			// several levels up, without the type itself declaring anything different
			&& Objects.equals(analyzer.getAllSuperTypes(type), other.analyzer.getAllSuperTypes(counterpart))
			&& Objects.equals(analyzer.getExportedMethodsByErasure(type),
				other.analyzer.getExportedMethodsByErasure(counterpart))
			&& Objects.equals(analyzer.getExportedFieldsByName(type),
				other.analyzer.getExportedFieldsByName(counterpart))
			&& exposesTheSameConstructors(type, counterpart, other);
	}

	private boolean exposesTheSameConstructors(TypeDecl type, TypeDecl counterpart, API other) {
		if (!(type instanceof ClassDecl cls) || !(counterpart instanceof ClassDecl otherCls)) {
			return true;
		}
		return Objects.equals(analyzer.getExportedConstructors(cls), other.analyzer.getExportedConstructors(otherCls));
	}

	@Override
	public int hashCode() {
		// Deliberately coarse: equality compares the resolved surface of every exported type, which is far too much to
		// hash on every lookup. Equal APIs export the same names under the same module, so this stays consistent.
		return Objects.hash(libraryTypes.getModule(), exportedTypes.keySet());
	}
}
