package io.github.alien.roseau.api.model.reference;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A flyweight {@link ITypeReference} factory.
 * <br>
 * This implementation caches the created references to ensure that there is only a single shared reference towards a
 * given name within the factory.
 */
public class CachingTypeReferenceFactory implements TypeReferenceFactory {
	private final Cache<ITypeReference, ITypeReference> referencesCache =
		CacheBuilder.newBuilder()
			.maximumSize(5_000L)
			.build();

	@SuppressWarnings("unchecked")
	private <U extends ITypeReference> U cache(U reference) {
		try {
			return (U) referencesCache.get(reference, () -> reference);
		} catch (ExecutionException e) {
			throw new RoseauException("Failed to cache type reference: " + reference, e);
		}
	}

	@Override
	public <T extends TypeDecl> TypeReference<T> createTypeReference(String qualifiedName,
	                                                                 List<ITypeReference> typeArguments) {
		return cache(new TypeReference<>(qualifiedName, typeArguments));
	}

	@Override
	public PrimitiveTypeReference createPrimitiveTypeReference(String simpleName) {
		return cache(new PrimitiveTypeReference(simpleName));
	}

	@Override
	public ArrayTypeReference createArrayTypeReference(ITypeReference componentType, int dimension) {
		return cache(new ArrayTypeReference(componentType, dimension));
	}

	@Override
	public TypeParameterReference createTypeParameterReference(String simpleName) {
		return cache(new TypeParameterReference(simpleName));
	}

	@Override
	public WildcardTypeReference createWildcardTypeReference(List<ITypeReference> bounds, boolean upper) {
		return cache(new WildcardTypeReference(bounds, upper));
	}
}
