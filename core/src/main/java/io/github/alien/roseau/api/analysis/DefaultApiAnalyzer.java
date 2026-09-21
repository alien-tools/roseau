package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The default {@link ApiAnalyzer}, backed by immutable indexes computed once, when the analyzer is built.
 * <p>
 * Which members a type offers and where it sits in the hierarchy are properties of the library snapshot: they cannot
 * change once it is extracted. Resolving them on demand is what used to make analysis expensive, as the answer for a
 * single type was re-derived once per question asked about it, and each answer re-walked the whole hierarchy above the
 * type. Both are instead resolved here exactly once per type, bottom-up:
 * <ol>
 *   <li>{@link SuperTypeIndex} indexes the transitive supertypes of every type in the snapshot, so that walking a
 *   hierarchy costs one lookup rather than a fresh traversal;</li>
 *   <li>the inherited members of every type are then resolved on top of that index, and indexed too, so that looking a
 *   method or a field up on a type is a plain map lookup.</li>
 * </ol>
 * Types that are not part of the snapshot, such as the classpath types a query may incidentally reach, are resolved on
 * the fly by the {@link HierarchyProvider} implementations the indexes are built from.
 */
public final class DefaultApiAnalyzer implements ApiAnalyzer {
	private final LibraryTypes libraryTypes;
	private final TypeResolver resolver;
	private final SetMultimap<String, TypeDecl> directKnownSubtypes;
	private final SuperTypeIndex superTypes;
	private final Map<String, TypeDecl> indexedTypes;
	private final Map<String, Map<String, MethodDecl>> allMethodsByErasure;
	private final Map<String, Map<String, MethodDecl>> exportedMethodsByErasure;
	private final Map<String, Map<String, FieldDecl>> exportedFieldsByName;

	public DefaultApiAnalyzer(LibraryTypes libraryTypes, TypeResolver resolver) {
		this.libraryTypes = Preconditions.checkNotNull(libraryTypes);
		this.resolver = Preconditions.checkNotNull(resolver);
		this.directKnownSubtypes = buildDirectKnownSubtypesBySuperType(libraryTypes);
		// Indexes are built in dependency order: each one is only ever read once the previous is in place, so none of
		// them observes a partially-built analyzer
		this.superTypes = new SuperTypeIndex(this, libraryTypes);
		// Members are only ever looked up on the types the library exposes; the others are left to be resolved on demand
		List<TypeDecl> exportedTypes = libraryTypes.getAllTypes().parallelStream().filter(this::isExported).toList();
		this.indexedTypes = index(exportedTypes, Function.identity());
		this.allMethodsByErasure = index(exportedTypes, type -> ApiAnalyzer.super.getAllMethodsByErasure(type));
		this.exportedMethodsByErasure = index(exportedTypes, type -> ApiAnalyzer.super.getExportedMethodsByErasure(type));
		this.exportedFieldsByName = index(exportedTypes, type -> ApiAnalyzer.super.getExportedFieldsByName(type));
	}

	@Override
	public LibraryTypes libraryTypes() {
		return libraryTypes;
	}

	@Override
	public TypeResolver resolver() {
		return resolver;
	}

	@Override
	public Set<TypeDecl> getDirectKnownSubtypes(TypeDecl type) {
		return directKnownSubtypes.get(type.getQualifiedName());
	}

	@Override
	public List<TypeReference<TypeDecl>> getAllSuperTypes(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return superTypes.getAllSuperTypes(type);
	}

	@Override
	public Set<TypeReference<TypeDecl>> getAllInstantiatedSuperTypes(TypeReference<?> reference) {
		Preconditions.checkNotNull(reference);
		return superTypes.getAllInstantiatedSuperTypes(reference);
	}

	@Override
	public Map<String, MethodDecl> getAllMethodsByErasure(TypeDecl type) {
		Preconditions.checkNotNull(type);
		Map<String, MethodDecl> indexed = indexed(allMethodsByErasure, type);
		return indexed != null ? indexed : ApiAnalyzer.super.getAllMethodsByErasure(type);
	}

	@Override
	public Map<String, MethodDecl> getExportedMethodsByErasure(TypeDecl type) {
		Preconditions.checkNotNull(type);
		Map<String, MethodDecl> indexed = indexed(exportedMethodsByErasure, type);
		return indexed != null ? indexed : ApiAnalyzer.super.getExportedMethodsByErasure(type);
	}

	@Override
	public Map<String, FieldDecl> getExportedFieldsByName(TypeDecl type) {
		Preconditions.checkNotNull(type);
		Map<String, FieldDecl> indexed = indexed(exportedFieldsByName, type);
		return indexed != null ? indexed : ApiAnalyzer.super.getExportedFieldsByName(type);
	}

	/**
	 * Returns what was indexed for {@code type}, or {@code null} if it is not the declaration that was indexed under
	 * that qualified name. A type may share its name with another declaration, be it one resolved from the classpath or
	 * one from the version this API is compared against: only the declaration the index was built from is answered.
	 */
	private <T> T indexed(Map<String, T> index, TypeDecl type) {
		return indexedTypes.get(type.getQualifiedName()) == type ? index.get(type.getQualifiedName()) : null;
	}

	private static <T> Map<String, T> index(List<TypeDecl> types, Function<TypeDecl, T> resolve) {
		return types.parallelStream()
			.collect(ImmutableMap.toImmutableMap(Symbol::getQualifiedName, resolve));
	}

	private static SetMultimap<String, TypeDecl> buildDirectKnownSubtypesBySuperType(LibraryTypes libraryTypes) {
		HashMultimap<String, TypeDecl> subtypes = HashMultimap.create();
		libraryTypes.getAllTypes().forEach(type ->
			PropertiesProvider.directSuperTypeNames(type).forEach(superTypeName -> subtypes.put(superTypeName, type)));
		return ImmutableSetMultimap.copyOf(subtypes);
	}
}
