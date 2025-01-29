package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.RecordDecl;

public final class RecordBuilder extends ClassBuilder {
	public RecordDecl make() {
		return new RecordDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
				formalTypeParameters, fields, methods, enclosingType, constructors);
	}
}
