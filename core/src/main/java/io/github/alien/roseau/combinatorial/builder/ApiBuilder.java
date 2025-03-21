package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.extractors.spoon.SpoonAPIFactory;

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
		return new API(allTypes.values().stream().map(TypeDeclBuilder::make).toList(), factory.getTypeReferenceFactory());
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
