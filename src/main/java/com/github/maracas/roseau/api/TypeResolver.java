package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeReference;
import com.github.maracas.roseau.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.visit.Visit;

import java.util.HashMap;
import java.util.Map;

public class TypeResolver extends AbstractAPIVisitor {
	private final API api;
	private final Map<String, TypeDecl> resolved;

	public TypeResolver(API api) {
		this.api = api;
		this.resolved = HashMap.newHashMap(api.getAllTypes().size());
	}

	@Override
	public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
		return () -> {
			if (it.getActualType().isPresent())
				return;

			// compute/putIfAbsent do not work with null values
			String toResolve = it.getQualifiedName();
			if (!resolved.containsKey(toResolve))
				resolved.put(toResolve, api.getType(toResolve).orElse(null));

			it.setActualType((U) resolved.get(toResolve));
		};
	}
}
