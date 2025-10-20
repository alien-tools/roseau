package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Optional;

public interface SymbolMatcher {
	Optional<TypeDecl> matchType(API api, TypeDecl t1);

	Optional<FieldDecl> matchField(API api, TypeDecl t2, FieldDecl f1);

	Optional<MethodDecl> matchMethod(API api, TypeDecl t2, MethodDecl m1);

	Optional<ConstructorDecl> matchConstructor(API api, ClassDecl c2, ConstructorDecl cons1);
}
