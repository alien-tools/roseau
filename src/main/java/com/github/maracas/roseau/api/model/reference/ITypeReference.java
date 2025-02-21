package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.maracas.roseau.api.model.DeepCopyable;
import com.github.maracas.roseau.api.model.TypeDecl;

import java.util.List;
import java.util.stream.Stream;

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
