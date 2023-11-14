package com.github.maracas.roseau.visit;

import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.TypeDecl;
import com.github.maracas.roseau.model.TypeReference;

import java.util.Optional;

public class TypeResolver extends AbstractVisitor {
	final API api;

	public TypeResolver(API api) {
		this.api = api;
	}

	@Override
	public Visit typeReference(TypeReference it) {
		return () -> {
			if (it.getActualType() != null)
				return;

			Optional<TypeDecl> resolved = api.types().stream()
				.filter(t -> t.getQualifiedName().equals(it.getQualifiedName()))
				.findFirst();

			resolved.ifPresentOrElse(
				it::setActualType,
				() -> {}
			);
		};
	}
}
