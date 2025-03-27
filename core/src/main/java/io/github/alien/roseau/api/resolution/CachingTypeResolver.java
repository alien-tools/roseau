package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	private final Map<String, ResolvedType> typeCache = new ConcurrentHashMap<>();

	private static final Logger LOGGER = LogManager.getLogger(CachingTypeResolver.class);

	// Cannot store null in typeCache, so this serves as a marker/sentinel value
	// to keep track of whether we've already attempted resolution or not
	private record ResolvedType(TypeDecl typeDecl) {}

	/**
	 * Constructs a new type resolver using the provided ordered sequence of type providers to resolve references.
	 *
	 * @param typeProviders ordered sequence of type providers
	 */
	public CachingTypeResolver(List<TypeProvider> typeProviders) {
		this.typeProviders = List.copyOf(typeProviders);
	}

	@Override
	public <T extends TypeDecl> Optional<T> resolve(TypeReference<T> reference) {
		if (typeCache.containsKey(reference.getQualifiedName())) {
			return Optional.ofNullable((T) typeCache.get(reference.getQualifiedName()).typeDecl());
		} else {
			Optional<T> typeDecl = typeProviders.stream()
				.map(provider -> provider.<T>findType(reference.getQualifiedName()))
				.flatMap(Optional::stream)
				.findFirst();

			if (typeDecl.isPresent()) {
				typeCache.put(reference.getQualifiedName(), new ResolvedType(typeDecl.get()));
			} else {
				LOGGER.warn("Failed to resolve type reference {}", reference.getQualifiedName());
				typeCache.put(reference.getQualifiedName(), new ResolvedType(null));
			}

			return typeDecl;
		}
	}
}
