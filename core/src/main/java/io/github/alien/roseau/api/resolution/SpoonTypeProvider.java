package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.spoon.SpoonAPIFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A type provider implementation that creates new type declarations reflectively to represent and navigate types
 * outside the API's scope. This implementation delegates the job to Spoon, configured with the provided classpath.
 */
public class SpoonTypeProvider implements TypeProvider {
	private final SpoonAPIFactory spoonFactory;

	/**
	 * Creates a new reflective type provider using {@code typeReferenceFactory} to create new references if needed, and
	 * using {@code classpath} to reflectively construct new types.
	 *
	 * @param typeReferenceFactory the {@link TypeReferenceFactory} to create new type references with
	 * @param classpath            the classpath used to find the requested types
	 */
	public SpoonTypeProvider(TypeReferenceFactory typeReferenceFactory, List<Path> classpath) {
		Preconditions.checkNotNull(typeReferenceFactory);
		Preconditions.checkNotNull(classpath);
		this.spoonFactory = new SpoonAPIFactory(typeReferenceFactory, classpath);
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		Preconditions.checkNotNull(qualifiedName);
		return Optional.ofNullable(spoonFactory.convertCtType(qualifiedName))
			.filter(type::isInstance)
			.map(type::cast);
	}
}
