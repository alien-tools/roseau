package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;

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

		builder.values = new ArrayList<>(decl.getValues());

		return builder;
	}
}
