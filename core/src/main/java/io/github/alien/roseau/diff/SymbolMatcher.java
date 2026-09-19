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

/**
 * Matches the symbols of an {@link API} with their counterpart in another version of the same API.
 * <br>
 * Symbols matched by name only (types, fields, annotation methods) are looked up in {@code targetApi} directly.
 * Executables are matched by erasure, so the source API is needed to compute erasures accurately.
 */
public interface SymbolMatcher {
	Optional<TypeDecl> matchType(API targetApi, TypeDecl type);

	Optional<FieldDecl> matchField(API targetApi, TypeDecl targetType, FieldDecl field);

	Optional<MethodDecl> matchMethod(API targetApi, TypeDecl targetType, MethodDecl method, API sourceApi,
	                                 TypeDecl sourceType);

	Optional<ConstructorDecl> matchConstructor(API targetApi, ClassDecl targetCls, ConstructorDecl cons, API sourceApi,
	                                           ClassDecl sourceCls);

	Optional<AnnotationMethodDecl> matchAnnotationMethod(API targetApi, AnnotationDecl targetAnnotation,
	                                                     AnnotationMethodDecl method);
}
