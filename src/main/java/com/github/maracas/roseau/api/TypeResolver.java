package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ArrayTypeReference;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeParameterReference;
import com.github.maracas.roseau.api.model.TypeReference;
import com.github.maracas.roseau.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.visit.Visit;
import spoon.reflect.factory.FactoryImpl;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeResolver extends AbstractAPIVisitor {
	private final API api;
	private final Map<String, TypeDecl> resolved;
	private final TypeFactory typeFactory;

	public TypeResolver(API api, TypeFactory typeFactory) {
		this.api = api;
		this.typeFactory = typeFactory;
		this.resolved = HashMap.newHashMap(api.getAllTypes().size());
	}

	@Override
	public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
		return () -> {
			if (it.getResolvedApiType().isPresent() || it.getForeignTypeReference().isPresent())
				return;

			// compute/putIfAbsent do not work with null values
			String toResolve = it.getQualifiedName();
			if (!resolved.containsKey(toResolve)) {
				Optional<TypeDecl> withinAPI = api.findType(toResolve);

				if (withinAPI.isPresent()) {
					resolved.put(toResolve, withinAPI.get());
				} else {
					CtTypeReference<?> spoonTypeRef = switch (it) {
						case ArrayTypeReference<U> arrayRef -> typeFactory.createArrayReference(toResolve);
						case TypeParameterReference<U> tpRef -> typeFactory.createTypeParameterReference(tpRef.getQualifiedName()); // FIXME
						default -> typeFactory.createReference(toResolve);
					};

					if (spoonTypeRef.getTypeDeclaration() == null)
						throw new IllegalStateException("Couldn't resolve " + spoonTypeRef + "; is classpath properly set?")

					it.setForeignTypeReference(spoonTypeRef);
				}
			}

			it.setResolvedApiType((U) resolved.get(toResolve));
		};
	}
}
