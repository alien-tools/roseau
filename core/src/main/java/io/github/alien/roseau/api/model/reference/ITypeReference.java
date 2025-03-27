package io.github.alien.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.alien.roseau.api.model.DeepCopyable;

import java.util.Collection;
import java.util.List;

/**
 * A named reference to another type (primitive, type parameter, wildcard, array, or type declaration).
 *
 * @see TypeReferenceFactory
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "refKind")
public sealed interface ITypeReference extends DeepCopyable<ITypeReference>
	permits TypeReference, ArrayTypeReference, PrimitiveTypeReference, TypeParameterReference, WildcardTypeReference {
	/**
	 * The qualified name this reference points to
	 *
	 * @return the qualified name
	 */
	String getQualifiedName();

	/**
	 * Returns a deep-copy of each {@link TypeReference} in {@code refs}
	 *
	 * @param refs the references to deep-copy
	 * @return the deep-copied references
	 */
	static List<ITypeReference> deepCopy(Collection<ITypeReference> refs) {
		return refs.stream()
			.map(ITypeReference::deepCopy)
			.toList();
	}
}
