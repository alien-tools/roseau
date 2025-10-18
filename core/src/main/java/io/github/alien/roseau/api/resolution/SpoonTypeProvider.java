package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.spoon.SpoonAPIFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A type provider implementation that creates new type declarations reflectively to represent and navigate types
 * outside the API's scope. This implementation delegates the job to Spoon, configured with the provided classpath.
 */
public class SpoonTypeProvider implements TypeProvider {
	private final SpoonAPIFactory spoonFactory;

	private static final Logger LOGGER = LogManager.getLogger(SpoonTypeProvider.class);

	/**
	 * Creates a new reflective type provider using {@code typeReferenceFactory} to create new references if needed, and
	 * using {@code classpath} to reflectively construct new types.
	 *
	 * @param typeReferenceFactory the {@link TypeReferenceFactory} to create new type references with
	 * @param classpath            the classpath used to find the requested types
	 */
	public SpoonTypeProvider(TypeReferenceFactory typeReferenceFactory, Set<Path> classpath) {
		Preconditions.checkNotNull(typeReferenceFactory);
		Preconditions.checkNotNull(classpath);
		Library virtual = Library.builder().location(Path.of("")).classpath(classpath).build();
		spoonFactory = new SpoonAPIFactory(virtual, typeReferenceFactory);
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		Preconditions.checkNotNull(qualifiedName);
		Optional<TypeDecl> resolved = Optional.ofNullable(spoonFactory.convertCtType(qualifiedName));

		if (resolved.isPresent() && !type.isInstance(resolved.get())) {
			LOGGER.warn("Type {} is not of expected type {}", qualifiedName, type);
			return Optional.empty();
		}

		return resolved.map(type::cast);
	}
}
