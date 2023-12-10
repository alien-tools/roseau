package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.visit.Visit;
import spoon.reflect.factory.TypeFactory;

public class TypeResolver extends AbstractAPIVisitor {
	private final API api;
	private final SpoonAPIFactory factory;

	public TypeResolver(API api, SpoonAPIFactory factory) {
		this.api = api;
		this.factory = factory;
	}

	@Override
	public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
		return () -> {
			it.setFactory(factory);
			api.findType(it.getQualifiedName()).ifPresent(t -> it.setResolvedApiType((U) t));
		};
	}
}
