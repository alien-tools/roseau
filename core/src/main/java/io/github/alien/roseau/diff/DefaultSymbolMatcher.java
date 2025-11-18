package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Optional;

public class DefaultSymbolMatcher implements SymbolMatcher {
	@Override
	public Optional<TypeDecl> matchType(API api, TypeDecl type) {
		return api.findExportedType(type.getQualifiedName());
	}

	@Override
	public Optional<FieldDecl> matchField(API api, TypeDecl type, FieldDecl field) {
		return api.findField(type, field.getSimpleName());
	}

	@Override
	public Optional<MethodDecl> matchMethod(API api, TypeDecl type, MethodDecl method) {
		return api.findMethod(type, api.getErasure(method));
	}

	@Override
	public Optional<ConstructorDecl> matchConstructor(API api, ClassDecl cls, ConstructorDecl cons) {
		return api.findConstructor(cls, api.getErasure(cons));
	}

	@Override
	public Optional<AnnotationMethodDecl> matchAnnotationMethod(API api, AnnotationDecl annotation,
	                                                            AnnotationMethodDecl method) {
		return annotation.getAnnotationMethods().stream()
			.filter(m -> m.getSimpleName().equals(method.getSimpleName()))
			.findFirst();
	}
}
