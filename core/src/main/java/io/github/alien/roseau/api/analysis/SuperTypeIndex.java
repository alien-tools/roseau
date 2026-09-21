package io.github.alien.roseau.api.analysis;

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
 * Resolves the transitive supertypes of a type, and indexes them for every type reachable from a {@link LibraryTypes}
 * snapshot.
 * <p>
 * Supertype closures underpin every hierarchy query: they drive subtyping checks and the resolution of inherited
 * members. Deriving one top-down is prohibitively expensive, as the closure of every supertype is then rebuilt from
 * scratch, once per path leading to it. This resolves them bottom-up instead, so that every {@code (type, supertype)}
 * edge is visited exactly once: the closure of a type is the union of the closures its direct supertypes already
 * resolved. Types outside the snapshot, such as the classpath types a query incidentally reaches, are resolved the same
 * way, on demand.
 * <p>
 * Two closures are resolved per type:
 * <ul>
 *   <li>the <em>nominal</em> one, where each supertype keeps the type arguments of its own declaration site;</li>
 *   <li>the <em>instantiated</em> one, where type arguments are propagated down the hierarchy and expressed in terms of
 *   the type's own formal type parameters, so that instantiating it for a particular {@link TypeReference} is a single
 *   substitution away.</li>
 * </ul>
 */
final class SuperTypeIndex {
	private final HierarchyProvider hierarchy;
	private final TypeResolver resolver;
	private final Map<String, Closure> closures;

	/**
	 * The supertypes resolved for one type declaration. The declaration itself is kept so that a type that merely
	 * shares its qualified name, such as a classpath type shadowed by a library type, is never answered from it.
	 */
	private record Closure(TypeDecl type, List<TypeReference<TypeDecl>> nominal,
	                       Set<TypeReference<TypeDecl>> instantiated) {
		static final Closure EMPTY = new Closure(null, List.of(), Set.of());
	}

	SuperTypeIndex(HierarchyProvider hierarchy, LibraryTypes libraryTypes) {
		this.hierarchy = hierarchy;
		this.resolver = hierarchy.resolver();

		// Resolving a type resolves its whole hierarchy, including the types it reaches through the classpath
		Map<String, Closure> indexed = new HashMap<>(2_000);
		libraryTypes.getAllTypes().forEach(type -> resolve(type, Map.of(), indexed, new HashSet<>()));
		this.closures = ImmutableMap.copyOf(indexed);
	}

	/**
	 * @see HierarchyProvider#getAllSuperTypes(TypeDecl)
	 */
	List<TypeReference<TypeDecl>> getAllSuperTypes(TypeDecl type) {
		return closureOf(type).nominal();
	}

	/**
	 * @see HierarchyProvider#getAllInstantiatedSuperTypes(TypeReference)
	 */
	Set<TypeReference<TypeDecl>> getAllInstantiatedSuperTypes(TypeReference<?> reference) {
		Optional<TypeDecl> resolved = resolver.resolve(reference);
		if (resolved.isEmpty()) {
			return Set.of();
		}

		// Closures are resolved in terms of the type's own formal type parameters: instantiating one for this
		// particular reference is a single substitution of the arguments it supplies
		TypeDecl type = resolved.get();
		return substitute(closureOf(type).instantiated(), TypeParameterMapping.forTypeArguments(type, reference));
	}

	private Closure closureOf(TypeDecl type) {
		Closure indexed = closures.get(type.getQualifiedName());
		return indexed != null && indexed.type() == type
			? indexed
			: resolve(type, closures, new HashMap<>(), new HashSet<>());
	}

	/**
	 * Resolves the closures of {@code type}, reusing those {@code indexed} when the index already holds them and
	 * accumulating the ones it has to resolve itself into {@code computed}.
	 */
	private Closure resolve(TypeDecl type, Map<String, Closure> indexed, Map<String, Closure> computed,
	                        Set<String> inProgress) {
		String qualifiedName = type.getQualifiedName();
		Closure resolved = indexed.getOrDefault(qualifiedName, computed.get(qualifiedName));
		if (resolved != null && resolved.type() == type) {
			return resolved;
		}
		// Malformed hierarchies may be cyclic; break the cycle rather than looping forever
		if (!inProgress.add(qualifiedName)) {
			return Closure.EMPTY;
		}

		// Both builders deduplicate on insertion and keep insertion order, so the closures come out ordered and
		// deduplicated without a second pass over the references, which are costly to hash
		ImmutableSet.Builder<TypeReference<TypeDecl>> nominal = ImmutableSet.builder();
		ImmutableSet.Builder<TypeReference<TypeDecl>> instantiated = ImmutableSet.builder();
		for (TypeReference<TypeDecl> superType : hierarchy.getSuperTypes(type)) {
			nominal.add(superType);
			instantiated.add(superType);
			resolver.resolve(superType).ifPresent(superDecl -> {
				Closure superClosure = resolve(superDecl, indexed, computed, inProgress);
				nominal.addAll(superClosure.nominal());
				// The supertype's closure is expressed in terms of its own formal type parameters; substituting the
				// arguments this declaration supplies expresses it in terms of ours instead
				instantiated.addAll(substitute(superClosure.instantiated(),
					TypeParameterMapping.forTypeArguments(superDecl, superType)));
			});
		}

		inProgress.remove(qualifiedName);
		Closure closure = new Closure(type, nominal.build().asList(), instantiated.build());
		computed.put(qualifiedName, closure);
		return closure;
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
