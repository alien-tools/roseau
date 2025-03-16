package io.github.alien.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.alien.roseau.api.model.DeepCopyable;

import java.util.List;

/**
 * @see TypeReferenceFactory
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "refKind")
public sealed interface ITypeReference extends DeepCopyable<ITypeReference>
	permits TypeReference, ArrayTypeReference, PrimitiveTypeReference, TypeParameterReference, WildcardTypeReference {
	String getQualifiedName();
	boolean isSubtypeOf(ITypeReference other);

	static List<ITypeReference> deepCopy(List<ITypeReference> refs) {
		return refs.stream()
			.map(ITypeReference::deepCopy)
			.toList();
	}
}
