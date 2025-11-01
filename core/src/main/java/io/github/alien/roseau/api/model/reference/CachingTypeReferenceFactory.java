package io.github.alien.roseau.api.model.reference;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A flyweight {@link ITypeReference} factory.
 * <br>
 * This implementation caches the created references to ensure that there is only a single shared reference towards a
 * given name within the factory.
 */
public class CachingTypeReferenceFactory implements TypeReferenceFactory {
	private final Cache<String, ITypeReference> referencesCache =
		CacheBuilder.newBuilder()
			.maximumSize(5_000L)
			.build();

	@SuppressWarnings("unchecked")
	private <U extends ITypeReference> U cache(String key, Supplier<U> supplier) {
		try {
			return (U) referencesCache.get(key, supplier::get);
		} catch (ExecutionException e) {
			throw new RoseauException("Failed to create type reference for key: " + key, e);
		}
	}

	@Override
	public <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName,
	                                                                 List<ITypeReference> typeArguments) {
		return cache("TR" + qualifiedName + key(typeArguments),
			() -> new TypeReference<>(qualifiedName, typeArguments));
	}

	@Override
	public PrimitiveTypeReference createPrimitiveTypeReference(String simpleName) {
		return cache("PTR" + simpleName,
			() -> new PrimitiveTypeReference(simpleName));
	}

	@Override
	public ArrayTypeReference createArrayTypeReference(ITypeReference componentType, int dimension) {
		return cache("ATR" + key(componentType) + dimension,
			() -> new ArrayTypeReference(componentType, dimension));
	}

	@Override
	public TypeParameterReference createTypeParameterReference(String simpleName) {
		return cache("TPR" + simpleName,
			() -> new TypeParameterReference(simpleName));
	}

	@Override
	public WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper) {
		return cache("WTR" + key(bounds) + upper,
			() -> new WildcardTypeReference(bounds, upper));
	}

	private String key(List<ITypeReference> references) {
		return references.stream().map(this::key).collect(Collectors.joining());
	}

	private String key(ITypeReference reference) {
		return switch (reference) {
			case ArrayTypeReference(var type, var dimension) -> key(type) + "[]".repeat(dimension);
			case PrimitiveTypeReference(var name) -> name;
			case TypeParameterReference(var name) -> name;
			case TypeReference(var fqn, var args) -> fqn + key(args);
			case WildcardTypeReference(var bounds, var upper) -> key(bounds) + upper;
		};
	}
}
