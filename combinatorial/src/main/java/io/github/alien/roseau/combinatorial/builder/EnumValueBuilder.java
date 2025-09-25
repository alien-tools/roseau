package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.EnumValueDecl;

public final class EnumValueBuilder extends TypeMemberBuilder {
	public EnumValueDecl make() {
		return new EnumValueDecl(qualifiedName, annotations, location, containingType, type);
	}

	public static EnumValueBuilder from(EnumValueDecl decl) {
		var builder = new EnumValueBuilder();

		builder.mutateWithDecl(decl);

		return builder;
	}
}
