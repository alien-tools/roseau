package io.github.alien.roseau.api.visit;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ReflectiveTypeFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Objects;

/**
 * An {@link API} visitor that visits {@link TypeReference} instances to set their factory and eagerly resolve
 * within-API {@link TypeDecl} instances.
 */
public class APITypeResolver extends AbstractAPIVisitor {
	private final API api;
	private final ReflectiveTypeFactory factory;

	/**
	 * Creates a new type resolver for the given API.
	 *
	 * @param api the {@link API} to visit
	 * @param factory the {@link ReflectiveTypeFactory} to set on {@link TypeReference} instances
	 * @throws NullPointerException if {@code api} or {@code factory} is null
	 */
	public APITypeResolver(API api, ReflectiveTypeFactory factory) {
		this.api = Objects.requireNonNull(api);
		this.factory = Objects.requireNonNull(factory);
	}

	/**
	 * Runs the visit on all {@link TypeReference} instances
	 */
	public void resolve() {
		$(api).visit();
	}

	@Override
	public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
		return () -> {
			it.setFactory(factory);
			api.findType(it.getQualifiedName()).ifPresent(t -> it.resolve((U) t));
			super.typeReference(it).visit();
		};
	}
}
