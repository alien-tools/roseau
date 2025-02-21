package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Flyweight {@link ITypeReference} factory.
 * <br>
 * This implementation caches the created references to ensure that there is a single shared reference towards any type
 * within a given factory.
 */
public class CachedTypeReferenceFactory implements TypeReferenceFactory {
	private final Map<String, ITypeReference> referencesCache = new ConcurrentHashMap<>(100);
	private final ReflectiveTypeFactory reflectiveTypeFactory;

	public CachedTypeReferenceFactory() {
		this.reflectiveTypeFactory = new ReflectiveTypeFactory(this);
	}

	private <U extends ITypeReference> U cache(String key, Supplier<U> f) {
		return (U) referencesCache.computeIfAbsent(key, k -> f.get());
	}

	@Override
	public <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName,
	                                                                 List<ITypeReference> typeArguments) {
		return cache("TR" + qualifiedName + typeArguments.toString(),
			() -> new TypeReference<>(qualifiedName, typeArguments, reflectiveTypeFactory));
	}

	@Override
	public PrimitiveTypeReference createPrimitiveTypeReference(String qualifiedName) {
		return cache("PTR" + qualifiedName,
			() -> new PrimitiveTypeReference(qualifiedName));
	}

	@Override
	public ArrayTypeReference createArrayTypeReference(ITypeReference componentType, int dimension) {
		return cache("ATR" + componentType + dimension,
			() -> new ArrayTypeReference(componentType, dimension));
	}

	@Override
	public TypeParameterReference createTypeParameterReference(String qualifiedName) {
		return cache("TPR" + qualifiedName,
			() -> new TypeParameterReference(qualifiedName));
	}

	@Override
	public WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper) {
		return cache("WTR" + bounds + upper,
			() -> new WildcardTypeReference(bounds, upper));
	}
}
