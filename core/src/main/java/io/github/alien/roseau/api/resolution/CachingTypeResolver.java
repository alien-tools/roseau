package io.github.alien.roseau.api.resolution;

import com.google.common.collect.ImmutableSortedSet;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A type resolver implementation that caches the result of attempting to resolve a type reference. If a reference
 * cannot be resolved, no further resolution will be attempted. If a reference is successfully resolved, the
 * corresponding type declaration is cached and returned in subsequent calls.
 */
public class CachingTypeResolver implements TypeResolver {
	/**
	 * An ordered list of type providers used to resolve type references. Resolution is attempted using each type
	 * provider, sequentially, until there are none left.
	 */
	private final List<TypeProvider> typeProviders;

	/**
	 * Stores the resolution results.
	 */
	private final Map<String, ResolvedType> typeCache = new ConcurrentHashMap<>(5_000);

	/**
	 * Keeps track of every type reference this resolver was asked to resolve and could not.
	 */
	private final Set<String> unresolvedTypes = ConcurrentHashMap.newKeySet();

	private static final Logger LOGGER = LoggerFactory.getLogger(CachingTypeResolver.class);

	// Cannot store null in typeCache, so this serves as a marker/sentinel value
	// to keep track of whether we've already attempted resolution or not
	private record ResolvedType(TypeDecl typeDecl) {
		private static final ResolvedType UNRESOLVED = new ResolvedType(null);
	}

	/**
	 * Constructs a new type resolver using the provided ordered sequence of type providers to resolve references.
	 *
	 * @param typeProviders ordered sequence of type providers
	 */
	public CachingTypeResolver(List<TypeProvider> typeProviders) {
		this.typeProviders = List.copyOf(typeProviders);
	}

	@Override
	public <T extends TypeDecl> Optional<T> resolve(TypeReference<T> reference, Class<T> type) {
		String fqn = reference.getQualifiedName();
		ResolvedType cached = typeCache.computeIfAbsent(fqn, this::resolveType);
		return Optional.ofNullable(cached.typeDecl()).filter(type::isInstance).map(type::cast);
	}

	private ResolvedType resolveType(String qualifiedName) {
		return typeProviders.stream()
			.map(provider -> provider.findType(qualifiedName, TypeDecl.class))
			.flatMap(Optional::stream)
			.findFirst()
			.map(ResolvedType::new)
			.orElseGet(() -> {
				unresolvedTypes.add(qualifiedName);
				LOGGER.debug("Failed to resolve type reference {}", qualifiedName);
				return ResolvedType.UNRESOLVED;
			});
	}

	@Override
	public Set<String> getUnresolvedTypes() {
		return ImmutableSortedSet.copyOf(unresolvedTypes);
	}
}
