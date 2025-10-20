package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Optional;

public class DefaultSymbolMatcher implements SymbolMatcher {
	@Override
	public Optional<TypeDecl> matchType(API api, TypeDecl t1) {
		return api.findExportedType(t1.getQualifiedName());
	}

	@Override
	public Optional<FieldDecl> matchField(API api, TypeDecl t2, FieldDecl f1) {
		return api.findField(t2, f1.getSimpleName());
	}

	@Override
	public Optional<MethodDecl> matchMethod(API api, TypeDecl t2, MethodDecl m1) {
		return api.findMethod(t2, api.getErasure(m1));
	}

	@Override
	public Optional<ConstructorDecl> matchConstructor(API api, ClassDecl c2, ConstructorDecl cons1) {
		return api.findConstructor(c2, api.getErasure(cons1));
	}
}
