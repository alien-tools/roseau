package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.EnumValueDecl;

public final class EnumValueBuilder extends TypeMemberBuilder {
	public EnumValueDecl make() {
		return new EnumValueDecl(qualifiedName, annotations, location, containingType, type);
	}
}
