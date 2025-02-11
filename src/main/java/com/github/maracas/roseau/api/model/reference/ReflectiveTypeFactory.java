package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.extractors.sources.SpoonAPIFactory;

/**
 * Creates {@link TypeDecl} types reflectively to represent and navigate types outside the API.
 * Current implementation simply delegates this job to Spoon, without classpath, so this just resolves JDK types.
 */
class ReflectiveTypeFactory {
	private final SpoonAPIFactory spoonFactory;

	ReflectiveTypeFactory() {
		spoonFactory = new SpoonAPIFactory();
	}

	TypeDecl convertCtType(String qualifiedName) {
		return spoonFactory.convertCtType(qualifiedName);
	}
}
