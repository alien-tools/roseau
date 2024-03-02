package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Type references factory. This implementation caches the created references so no two references are
 * created towards the same type.
 */
public class SpoonTypeReferenceFactory implements TypeReferenceFactory {
	private final SpoonAPIFactory apiFactory;

	public SpoonTypeReferenceFactory(SpoonAPIFactory apiFactory) {
		this.apiFactory = Objects.requireNonNull(apiFactory);
	}

	private final Map<String, ITypeReference> referencesCache = new ConcurrentHashMap<>();

	private <U extends ITypeReference> U cache(String key, Supplier<U> f) {
		return (U) referencesCache.computeIfAbsent(key, k -> f.get());
	}

	@Override
	public <T extends TypeDecl> TypeReference<T> createTypeReference(
		String qualifiedName, List<ITypeReference> typeArguments) {
		String key = "TR" + qualifiedName + typeArguments.stream().map(Object::toString).collect(Collectors.joining(","));
		return cache(key, () -> new TypeReference<>(qualifiedName, typeArguments, apiFactory));
	}

	@Override
	public PrimitiveTypeReference createPrimitiveTypeReference(String qualifiedName) {
		return cache("PTR" + qualifiedName, () -> new PrimitiveTypeReference(qualifiedName));
	}

	@Override
	public ArrayTypeReference createArrayTypeReference(ITypeReference componentType, int dimension) {
		return cache("ATR" + componentType.toString() + dimension, () -> new ArrayTypeReference(componentType, dimension));
	}

	@Override
	public TypeParameterReference createTypeParameterReference(String qualifiedName) {
		return cache("TPR" + qualifiedName, () -> new TypeParameterReference(qualifiedName));
	}

	@Override
	public WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper) {
		String key = bounds.stream().map(Object::toString).collect(Collectors.joining(","))+upper;
		return cache("WTR" + key, () -> new WildcardTypeReference(bounds, upper));
	}
}
