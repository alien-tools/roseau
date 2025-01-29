package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.*;

import java.util.HashMap;
import java.util.Map;

public final class ApiBuilder implements Builder<API> {
	public final Map<String, TypeDeclBuilder> allTypes = new HashMap<>();

	private SpoonAPIFactory factory;

	@Override
	public API make() {
		return new API(allTypes.values().stream().map(TypeDeclBuilder::make).toList(), factory);
	}

	public static ApiBuilder from(API api) {
		var apiBuilder = new ApiBuilder();

		apiBuilder.factory = api.getFactory();
		api.getAllTypes().forEach(typeDecl -> {
			switch (typeDecl) {
				case EnumDecl enumDecl:
					break;
				case RecordDecl recordDecl:
					break;
				case ClassDecl classDecl:
					break;
				case InterfaceDecl interfaceDecl:
					var interfaceBuilder = InterfaceBuilder.from(interfaceDecl);
					apiBuilder.allTypes.put(interfaceDecl.getQualifiedName(), interfaceBuilder);
					break;
				case AnnotationDecl annotationDecl:
					break;
			}
		});

		return apiBuilder;
	}
}
