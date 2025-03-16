package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.extractors.spoon.SpoonAPIFactory;

/**
 * Creates {@link TypeDecl} instances reflectively to represent and navigate types outside the API.
 * Current implementation simply delegates this job to Spoon, without classpath, so this just resolves JDK types.
 */
public class ReflectiveTypeFactory {
	private final SpoonAPIFactory spoonFactory;

	public ReflectiveTypeFactory(TypeReferenceFactory typeReferenceFactory) {
		spoonFactory = new SpoonAPIFactory(typeReferenceFactory);
	}

	TypeDecl convertCtType(String qualifiedName) {
		return spoonFactory.convertCtType(qualifiedName);
	}
}
