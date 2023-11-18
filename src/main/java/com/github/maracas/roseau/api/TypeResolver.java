package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.InterfaceDecl;
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
	public Visit typeReference(TypeReference<TypeDecl> it) {
		return () -> {
			if (it.getActualType().isPresent())
				return;

			// compute/putIfAbsent do not work with null values
			String toResolve = it.getQualifiedName();
			if (!resolved.containsKey(toResolve))
				resolved.put(toResolve, api.getType(toResolve).orElse(null));

			it.setActualType(resolved.get(toResolve));
		};
	}

	@Override
	public Visit classReference(TypeReference<ClassDecl> it) {
		return () -> {
			if (it.getActualType().isPresent())
				return;

			// compute/putIfAbsent do not work with null values
			String toResolve = it.getQualifiedName();
			if (!resolved.containsKey(toResolve))
				resolved.put(toResolve, api.getClass(toResolve).orElse(null));

			if (resolved.get(toResolve) instanceof ClassDecl cls)
				it.setActualType(cls);
		};
	}

	@Override
	public Visit interfaceReference(TypeReference<InterfaceDecl> it) {
		return () -> {
			if (it.getActualType().isPresent())
				return;

			// compute/putIfAbsent do not work with null values
			String toResolve = it.getQualifiedName();
			if (!resolved.containsKey(toResolve))
				resolved.put(toResolve, api.getInterface(toResolve).orElse(null));

			if (resolved.get(toResolve) instanceof InterfaceDecl intf)
				it.setActualType(intf);
		};
	}
}
