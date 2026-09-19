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
	public Optional<TypeDecl> matchType(API targetApi, TypeDecl type) {
		return targetApi.findExportedType(type.getQualifiedName());
	}

	@Override
	public Optional<FieldDecl> matchField(API targetApi, TypeDecl targetType, FieldDecl field) {
		return targetApi.analyzer().findField(targetType, field.getSimpleName());
	}

	@Override
	public Optional<MethodDecl> matchMethod(API targetApi, TypeDecl targetType, MethodDecl method, API sourceApi,
	                                        TypeDecl sourceType) {
		return targetApi.analyzer().findMethod(targetType, sourceApi.analyzer().getErasure(sourceType, method));
	}

	@Override
	public Optional<ConstructorDecl> matchConstructor(API targetApi, ClassDecl targetCls, ConstructorDecl cons,
	                                                  API sourceApi, ClassDecl sourceCls) {
		return targetApi.analyzer().findConstructor(targetCls, sourceApi.analyzer().getErasure(sourceCls, cons));
	}

	@Override
	public Optional<AnnotationMethodDecl> matchAnnotationMethod(API targetApi, AnnotationDecl targetAnnotation,
	                                                            AnnotationMethodDecl method) {
		return targetAnnotation.getAnnotationMethods().stream()
			.filter(m -> m.getSimpleName().equals(method.getSimpleName()))
			.findFirst();
	}
}
