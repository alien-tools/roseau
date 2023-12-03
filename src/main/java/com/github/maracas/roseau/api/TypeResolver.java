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
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeResolver extends AbstractAPIVisitor {
	private final API api;
	private final Map<String, TypeDecl> resolved;
	private final TypeFactory typeFactory;

	public TypeResolver(API api) {
		this.api = api;
		this.resolved = HashMap.newHashMap(api.getAllTypes().size());
		this.typeFactory = new FactoryImpl(new DefaultCoreFactory(), new StandardEnvironment()).Type();
	}

	@Override
	public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
		return () -> {
			if (it.getResolvedApiType().isPresent() || it.getForeignTypeReference().isPresent())
				return;

			// compute/putIfAbsent do not work with null values
			String toResolve = it.getQualifiedName();
			if (!resolved.containsKey(toResolve)) {
				Optional<TypeDecl> withinAPI = api.getType(toResolve);

				if (withinAPI.isPresent()) {
					resolved.put(toResolve, withinAPI.get());
				} else {
					System.out.println("Looking for " + toResolve);

					it.setForeignTypeReference(switch (it) {
						case ArrayTypeReference<U> arrayRef -> typeFactory.createArrayReference(toResolve);
						case TypeParameterReference<U> tpRef -> typeFactory.createTypeParameterReference(tpRef.getQualifiedName()); // FIXME
						default -> typeFactory.createReference(toResolve);
					});
				}
			}

			it.setResolvedApiType((U) resolved.get(toResolve));
		};
	}
}
