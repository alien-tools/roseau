package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.*;

import java.util.HashMap;
import java.util.Map;

public final class ApiBuilder implements Builder<API> {
	public final Map<String, TypeDeclBuilder> allTypes = new HashMap<>();

	private final SpoonAPIFactory factory;

	public ApiBuilder(SpoonAPIFactory factory) {
		this.factory = factory;
	}

	@Override
	public API make() {
		return new API(allTypes.values().stream().map(TypeDeclBuilder::make).toList(), factory);
	}

	public static ApiBuilder from(API api, SpoonAPIFactory factory) {
		var apiBuilder = new ApiBuilder(factory);

		api.getAllTypes().forEach(typeDecl -> {
			switch (typeDecl) {
				case EnumDecl enumDecl:
					var enumBuilder = EnumBuilder.from(enumDecl);
					apiBuilder.allTypes.put(enumDecl.getQualifiedName(), enumBuilder);
					break;
				case RecordDecl recordDecl:
					var recordBuilder = RecordBuilder.from(recordDecl);
					apiBuilder.allTypes.put(recordDecl.getQualifiedName(), recordBuilder);
					break;
				case ClassDecl classDecl:
					var classBuilder = ClassBuilder.from(classDecl);
					apiBuilder.allTypes.put(classDecl.getQualifiedName(), classBuilder);
					break;
				case InterfaceDecl interfaceDecl:
					var interfaceBuilder = InterfaceBuilder.from(interfaceDecl);
					apiBuilder.allTypes.put(interfaceDecl.getQualifiedName(), interfaceBuilder);
					break;
				case AnnotationDecl ignored:
					break;
			}
		});

		return apiBuilder;
	}
}
