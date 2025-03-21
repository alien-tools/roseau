package io.github.alien.roseau.api.model.reference;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.spoon.SpoonAPIFactory;

import java.util.Objects;

/**
 * Creates {@link TypeDecl} instances reflectively to represent and navigate types outside the API's scope. Current
 * implementation simply delegates this job to Spoon, without classpath, so this just resolves JDK types.
 */
public class ReflectiveTypeFactory {
	private final SpoonAPIFactory spoonFactory;

	/**
	 * Creates a new reflective type factory using {@code typeReferenceFactory} to create new references if needed.
	 *
	 * @param typeReferenceFactory the {@link TypeReferenceFactory} to use
	 * @throws NullPointerException if {@code typeReferenceFactory} is null
	 */
	public ReflectiveTypeFactory(TypeReferenceFactory typeReferenceFactory) {
		spoonFactory = new SpoonAPIFactory(Objects.requireNonNull(typeReferenceFactory));
	}

	/**
	 * Creates a new {@link TypeDecl} corresponding to the name pointed by {@code qualifiedName}.
	 *
	 * @param qualifiedName the fully qualified name of the {@link TypeDecl} to create
	 * @return the new {@link TypeDecl} or null if it cannot be created
	 */
	TypeDecl convertCtType(String qualifiedName) {
		return spoonFactory.convertCtType(qualifiedName);
	}
}
