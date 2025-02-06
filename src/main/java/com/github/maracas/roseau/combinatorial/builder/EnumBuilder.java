package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.EnumValueDecl;

import java.util.ArrayList;
import java.util.List;

public final class EnumBuilder extends ClassBuilder {
	public List<EnumValueDecl> values = new ArrayList<>();

	public EnumDecl make() {
		return new EnumDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
				fields, methods, enclosingType, constructors, values);
	}

	public static EnumBuilder from(EnumDecl decl) {
		var builder = new EnumBuilder();

		ClassBuilder.mutateTypeDeclBuilderWithTypeDecl(builder, decl);

		builder.values = decl.getValues();

		return builder;
	}
}
