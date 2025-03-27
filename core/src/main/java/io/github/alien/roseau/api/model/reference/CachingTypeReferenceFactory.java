package io.github.alien.roseau.api.model.reference;

import io.github.alien.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A flyweight {@link ITypeReference} factory.
 * <br>
 * This implementation caches the created references to ensure that there is only a single shared reference towards a
 * given name within the factory.
 */
public class CachingTypeReferenceFactory implements TypeReferenceFactory {
	private final Map<String, ITypeReference> referencesCache = new ConcurrentHashMap<>(100);

	private <U extends ITypeReference> U cache(String key, Supplier<U> supplier) {
		return (U) referencesCache.computeIfAbsent(key, k -> supplier.get());
	}

	@Override
	public <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName,
	                                                                 List<ITypeReference> typeArguments) {
		return cache("TR" + qualifiedName + typeArguments.stream().map(ITypeReference::getQualifiedName).toList(),
			() -> new TypeReference<>(qualifiedName, typeArguments));
	}

	@Override
	public PrimitiveTypeReference createPrimitiveTypeReference(String simpleName) {
		return cache("PTR" + simpleName,
			() -> new PrimitiveTypeReference(simpleName));
	}

	@Override
	public ArrayTypeReference createArrayTypeReference(ITypeReference componentType, int dimension) {
		return cache("ATR" + componentType + dimension,
			() -> new ArrayTypeReference(componentType, dimension));
	}

	@Override
	public TypeParameterReference createTypeParameterReference(String simpleName) {
		return cache("TPR" + simpleName,
			() -> new TypeParameterReference(simpleName));
	}

	@Override
	public WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper) {
		return cache("WTR" + bounds + upper,
			() -> new WildcardTypeReference(bounds, upper));
	}
}
