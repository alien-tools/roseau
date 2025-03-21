package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.InterfaceDecl;

import java.util.ArrayList;
import java.util.List;

public final class InterfaceBuilder extends TypeDeclBuilder {
	public List<String> permittedTypes = new ArrayList<>();

	public InterfaceDecl make() {
		return new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, permittedTypes);
	}

	public static InterfaceBuilder from(InterfaceDecl decl) {
		var builder = new InterfaceBuilder();

		TypeDeclBuilder.mutateTypeDeclBuilderWithTypeDecl(builder, decl);

		builder.permittedTypes = decl.getPermittedTypes();

		return builder;
	}
}
