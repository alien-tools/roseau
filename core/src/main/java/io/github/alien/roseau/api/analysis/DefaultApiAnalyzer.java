package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The default {@link ApiAnalyzer}, backed by facts resolved once, when the analyzer is built.
 * <p>
 * Where a type sits in the hierarchy, whether clients can see and extend it, and which members they can use are all
 * properties of the library snapshot: they cannot change once it is extracted. Resolving them on demand is what made
 * analysis expensive, as the answer for a single type was re-derived once per question asked about it. They are instead
 * resolved here exactly once per type, in dependency order:
 * <ol>
 *   <li>whether each type is exported and whether clients can subtype it, which only depends on the declarations;</li>
 *   <li>{@link SuperTypeIndex} resolves the supertypes of every exported type, bottom-up, and so those of every type
 *   in their hierarchies;</li>
 *   <li>on top of both, the members clients can use on each exported type, indexed for lookup by erasure or name.</li>
 * </ol>
 * Only the hierarchies clients can observe are resolved eagerly: resolving those of internal types would also report
 * the types they cannot resolve, such as the supertype of an internal class that comes from an optional dependency,
 * although no part of the API depends on them.
 * Each phase is complete before the next one starts, so none of them ever observes a partially-resolved snapshot: an
 * index answers nothing until it holds every type it is meant to hold. The other types, such as the classpath types a
 * query incidentally reaches, are resolved on demand by the {@link HierarchyProvider} and {@link PropertiesProvider}
 * implementations this class inherits.
 */
public final class DefaultApiAnalyzer implements ApiAnalyzer {
	private final LibraryTypes libraryTypes;
	private final TypeResolver resolver;
	private final ListMultimap<String, TypeDecl> directKnownSubtypes;
	private final SuperTypeIndex superTypes;
	private final Map<String, Accessibility> accessibility;
	private final Map<String, Members> members;

	/**
	 * A fact resolved for one type declaration. The declaration itself is kept so that a type that merely shares its
	 * qualified name, such as a classpath type or a type from the version this API is compared against, is never
	 * answered from it.
	 */
	private sealed interface Resolved {
		TypeDecl type();
	}

	/**
	 * What clients can do with a type. Both answers are needed to tell which of its members are part of the API, so
	 * they are resolved before {@link Members}.
	 */
	private record Accessibility(TypeDecl type, boolean exported, boolean subtypable) implements Resolved {
	}

	/**
	 * The members clients can use on a type, its inherited ones included, indexed the way they are looked up.
	 */
	private record Members(TypeDecl type, Map<String, MethodDecl> methodsByErasure,
	                       Map<String, FieldDecl> fieldsByName) implements Resolved {
	}

	public DefaultApiAnalyzer(LibraryTypes libraryTypes, TypeResolver resolver) {
		this.libraryTypes = Preconditions.checkNotNull(libraryTypes);
		this.resolver = Preconditions.checkNotNull(resolver);
		this.directKnownSubtypes = directKnownSubtypesBySuperType(libraryTypes);
		this.accessibility = index(libraryTypes.getAllTypes().stream(), type ->
			new Accessibility(type, ApiAnalyzer.super.isExported(type), ApiAnalyzer.super.canBeSubtyped(type)));
		this.superTypes = new SuperTypeIndex(this, libraryTypes.getAllTypes().stream().filter(this::isExported));
		// Members are only ever looked up on the types the library exposes; the others are resolved on demand
		this.members = index(libraryTypes.getAllTypes().stream().filter(this::isExported), type ->
			new Members(type, ApiAnalyzer.super.getExportedMethodsByErasure(type),
				ApiAnalyzer.super.getExportedFieldsByName(type)));
		// What is indexed is handed out as is, and the resolution above builds it immutably, so no caller can alter
		// what every later query answers from
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
	public Collection<TypeDecl> getDirectKnownSubtypes(TypeDecl type) {
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
	public boolean isExported(TypeDecl type) {
		Preconditions.checkNotNull(type);
		Accessibility resolved = resolved(accessibility, type);
		return resolved != null ? resolved.exported() : ApiAnalyzer.super.isExported(type);
	}

	@Override
	public boolean canBeSubtyped(TypeDecl type) {
		Preconditions.checkNotNull(type);
		Accessibility resolved = resolved(accessibility, type);
		return resolved != null ? resolved.subtypable() : ApiAnalyzer.super.canBeSubtyped(type);
	}

	@Override
	public Map<String, MethodDecl> getExportedMethodsByErasure(TypeDecl type) {
		Preconditions.checkNotNull(type);
		Members resolved = resolved(members, type);
		return resolved != null ? resolved.methodsByErasure() : ApiAnalyzer.super.getExportedMethodsByErasure(type);
	}

	@Override
	public Map<String, FieldDecl> getExportedFieldsByName(TypeDecl type) {
		Preconditions.checkNotNull(type);
		Members resolved = resolved(members, type);
		return resolved != null ? resolved.fieldsByName() : ApiAnalyzer.super.getExportedFieldsByName(type);
	}

	/**
	 * Returns what was resolved for {@code type}, or {@code null} when nothing was: either this index is still being
	 * built, and answers nothing yet, or {@code type} is not the declaration it holds under that qualified name.
	 */
	private static <T extends Resolved> T resolved(Map<String, T> index, TypeDecl type) {
		if (index == null) {
			return null;
		}
		T resolved = index.get(type.getQualifiedName());
		return resolved != null && resolved.type() == type ? resolved : null;
	}

	private static <T> Map<String, T> index(Stream<TypeDecl> types, Function<TypeDecl, T> resolve) {
		return types.parallel().collect(ImmutableMap.toImmutableMap(TypeDecl::getQualifiedName, resolve));
	}

	/**
	 * Indexes the types of the snapshot by the qualified name of the supertypes they declare. A multimap of lists keeps
	 * the declarations out of hash buckets: a type declares a given supertype at most once, so there is nothing to
	 * deduplicate, and hashing a declaration walks its modifiers and annotations.
	 */
	private static ListMultimap<String, TypeDecl> directKnownSubtypesBySuperType(LibraryTypes libraryTypes) {
		ImmutableListMultimap.Builder<String, TypeDecl> subtypes = ImmutableListMultimap.builder();
		libraryTypes.getAllTypes().forEach(type ->
			PropertiesProvider.directSuperTypeNames(type).forEach(superTypeName -> subtypes.put(superTypeName, type)));
		return subtypes.build();
	}
}
