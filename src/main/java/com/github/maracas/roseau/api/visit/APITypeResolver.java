package com.github.maracas.roseau.api.visit;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.TypeReference;

public class APITypeResolver extends AbstractAPIVisitor {
	private final API api;

	public APITypeResolver(API api) {
		this.api = api;
	}

	public void resolve() {
		$(api).visit();
	}

	@Override
	public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
		return () -> api.findType(it.getQualifiedName()).ifPresent(t -> it.resolve((U) t));
	}
}
