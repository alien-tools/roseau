package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.EnumDecl;

import java.util.ArrayList;
import java.util.List;

public final class EnumBuilder extends ClassBuilder {
	public List<EnumValueBuilder> values = new ArrayList<>();

	public EnumDecl make() {
		var fields = this.fields.stream().map(FieldBuilder::make).toList();
		var methods = this.methods.stream().map(MethodBuilder::make).toList();
		var constructors = this.constructors.stream().map(ConstructorBuilder::make).toList();
		var values = this.values.stream().map(EnumValueBuilder::make).toList();

		return new EnumDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
				fields, methods, enclosingType, constructors, values);
	}

	public static EnumBuilder from(EnumDecl decl) {
		var builder = new EnumBuilder();

		ClassBuilder.mutateTypeDeclBuilderWithTypeDecl(builder, decl);

		builder.values = new ArrayList<>(decl.getValues().stream().map(EnumValueBuilder::from).toList());

		return builder;
	}
}
