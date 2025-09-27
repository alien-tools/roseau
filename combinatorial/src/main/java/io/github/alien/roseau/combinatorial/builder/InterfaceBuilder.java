package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;

public final class InterfaceBuilder extends TypeBuilder {
	public List<TypeReference<TypeDecl>> permittedTypes = new ArrayList<>();

	public InterfaceDecl make() {
		var fields = this.fields.stream().map(FieldBuilder::make).toList();
		var methods = this.methods.stream().map(MethodBuilder::make).toList();

		return new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, permittedTypes);
	}

	public static InterfaceBuilder from(InterfaceDecl decl) {
		var builder = new InterfaceBuilder();

		builder.mutateWithDecl(decl);

		builder.permittedTypes = new ArrayList<>(decl.getPermittedTypes());

		return builder;
	}
}
