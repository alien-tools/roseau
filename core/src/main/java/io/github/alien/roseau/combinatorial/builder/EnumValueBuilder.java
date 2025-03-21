package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.EnumValueDecl;

public final class EnumValueBuilder extends TypeMemberBuilder {
	public EnumValueDecl make() {
		return new EnumValueDecl(qualifiedName, annotations, location, containingType, type);
	}
}
