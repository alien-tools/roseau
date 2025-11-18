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

public interface SymbolMatcher {
	Optional<TypeDecl> matchType(API api, TypeDecl type);

	Optional<FieldDecl> matchField(API api, TypeDecl type, FieldDecl field);

	Optional<MethodDecl> matchMethod(API api, TypeDecl type, MethodDecl method);

	Optional<ConstructorDecl> matchConstructor(API api, ClassDecl cls, ConstructorDecl cons);

	Optional<AnnotationMethodDecl> matchAnnotationMethod(API api, AnnotationDecl type, AnnotationMethodDecl method);
}
