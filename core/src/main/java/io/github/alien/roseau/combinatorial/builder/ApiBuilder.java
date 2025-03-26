package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.reference.CachedTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;

import java.util.HashMap;
import java.util.Map;

public final class ApiBuilder implements Builder<API> {
	public final Map<String, TypeDeclBuilder> allTypes = new HashMap<>();

	public final TypeReferenceFactory typeReferenceFactory;

	public ApiBuilder(TypeReferenceFactory typeReferenceFactory) {
		this.typeReferenceFactory = typeReferenceFactory;
	}

	@Override
	public API make() {
		return new API(allTypes.values().stream().map(TypeDeclBuilder::make).toList(), typeReferenceFactory);
	}

	public static ApiBuilder from(API api) {
		var factory = new CachedTypeReferenceFactory();
		var apiBuilder = new ApiBuilder(factory);

		var apiCloned = api.deepCopy(factory);
		apiCloned.getAllTypes().forEach(typeDecl -> {
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
