package io.github.alien.roseau.api.visit;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ReflectiveTypeFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;

public class APITypeResolver extends AbstractAPIVisitor {
	private final API api;
	private final ReflectiveTypeFactory factory;

	public APITypeResolver(API api, ReflectiveTypeFactory factory) {
		this.api = api;
		this.factory = factory;
	}

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
