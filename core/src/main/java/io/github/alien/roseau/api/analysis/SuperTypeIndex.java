package io.github.alien.roseau.api.analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable index of the transitive supertypes of every type reachable from a {@link LibraryTypes} snapshot.
 * <p>
 * Supertype closures underpin every hierarchy query: they drive subtyping checks and the resolution of inherited
 * members. Deriving them on demand is prohibitively expensive, as the closure of a type is then re-derived from scratch
 * at every level of its own hierarchy, once per path leading to it. Indexing them bottom-up instead visits every
 * {@code (type, supertype)} edge exactly once: the closure of a type is the union of the closures its direct supertypes
 * already computed.
 * <p>
 * Two closures are indexed per type:
 * <ul>
 *   <li>the <em>nominal</em> closure, where each supertype keeps the type arguments of its own declaration site;</li>
 *   <li>the <em>instantiated</em> closure, where type arguments are propagated down the hierarchy and expressed in
 *   terms of the indexed type's own formal type parameters, so that instantiating it for a particular
 *   {@link TypeReference} is a single substitution away.</li>
 * </ul>
 */
final class SuperTypeIndex {
	private final HierarchyProvider hierarchy;
	private final TypeResolver resolver;
	private final Map<String, Closure> closures;

	/**
	 * The supertypes indexed for one type declaration. The declaration itself is kept so that a type that merely shares
	 * its qualified name, such as a classpath type shadowed by a library type, is never answered from it.
	 */
	private record Closure(TypeDecl type, List<TypeReference<TypeDecl>> nominal,
	                       Set<TypeReference<TypeDecl>> instantiated) {
		static final Closure EMPTY = new Closure(null, List.of(), Set.of());
	}

	SuperTypeIndex(HierarchyProvider hierarchy, LibraryTypes libraryTypes) {
		this.hierarchy = hierarchy;
		this.resolver = hierarchy.resolver();

		// Indexing a type indexes its whole hierarchy, including the types it resolves from the classpath
		Map<String, Closure> indexed = new HashMap<>(2_000);
		libraryTypes.getAllTypes().forEach(type -> closure(type, Map.of(), indexed, new HashSet<>()));
		this.closures = ImmutableMap.copyOf(indexed);
	}

	/**
	 * Returns all supertypes of {@code type}, transitively, {@code type} excluded. Supertypes are returned as written at
	 * their declaration site: type arguments are <strong>not</strong> propagated down the hierarchy.
	 *
	 * @see #getAllInstantiatedSuperTypes(TypeReference)
	 */
	List<TypeReference<TypeDecl>> getAllSuperTypes(TypeDecl type) {
		return closureOf(type).nominal();
	}

	/**
	 * Returns all supertypes of {@code reference}, transitively, with generic arguments instantiated through the
	 * hierarchy: for {@code ArrayList<String>}, this yields {@code List<String>}, {@code Collection<String>}, etc.
	 */
	Set<TypeReference<TypeDecl>> getAllInstantiatedSuperTypes(TypeReference<?> reference) {
		Optional<TypeDecl> resolved = resolver.resolve(reference);
		if (resolved.isEmpty()) {
			return Set.of();
		}

		// The closure is indexed in terms of the type's own formal type parameters: instantiating it for this particular
		// reference is a single substitution of the arguments it supplies
		TypeDecl type = resolved.get();
		return substitute(closureOf(type).instantiated(), TypeParameterMapping.forTypeArguments(type, reference));
	}

	/**
	 * Returns the closure indexed for {@code type}, or computes it if this type is not part of the snapshot the index
	 * was built from, as classpath types a query incidentally reaches may be.
	 */
	private Closure closureOf(TypeDecl type) {
		Closure indexed = closures.get(type.getQualifiedName());
		return indexed != null && indexed.type() == type
			? indexed
			: closure(type, closures, new HashMap<>(), new HashSet<>());
	}

	private Closure closure(TypeDecl type, Map<String, Closure> indexed, Map<String, Closure> computed,
	                        Set<String> inProgress) {
		String qualifiedName = type.getQualifiedName();
		Closure closure = indexed.getOrDefault(qualifiedName, computed.get(qualifiedName));
		if (closure != null && closure.type() == type) {
			return closure;
		}
		// Malformed hierarchies may be cyclic; break the cycle rather than looping forever
		if (!inProgress.add(qualifiedName)) {
			return Closure.EMPTY;
		}

		Set<TypeReference<TypeDecl>> nominal = new LinkedHashSet<>();
		Set<TypeReference<TypeDecl>> instantiated = new LinkedHashSet<>();
		for (TypeReference<TypeDecl> superType : hierarchy.getSuperTypes(type)) {
			nominal.add(superType);
			instantiated.add(superType);
			resolver.resolve(superType).ifPresent(superDecl -> {
				Closure superClosure = closure(superDecl, indexed, computed, inProgress);
				nominal.addAll(superClosure.nominal());
				// The supertype's own closure is expressed in terms of its formal type parameters; substituting the
				// arguments this declaration supplies expresses it in terms of ours instead
				instantiated.addAll(substitute(superClosure.instantiated(),
					TypeParameterMapping.forTypeArguments(superDecl, superType)));
			});
		}

		inProgress.remove(qualifiedName);
		Closure resolved = new Closure(type, ImmutableList.copyOf(nominal), ImmutableSet.copyOf(instantiated));
		computed.put(qualifiedName, resolved);
		return resolved;
	}

	@SuppressWarnings("unchecked")
	private static Set<TypeReference<TypeDecl>> substitute(Set<TypeReference<TypeDecl>> superTypes,
	                                                       Map<String, ITypeReference> substitutions) {
		if (substitutions.isEmpty()) {
			return superTypes;
		}
		Set<TypeReference<TypeDecl>> substituted = LinkedHashSet.newLinkedHashSet(superTypes.size());
		superTypes.forEach(superType ->
			substituted.add((TypeReference<TypeDecl>) TypeParameterMapping.substitute(superType, substitutions)));
		return substituted;
	}
}
