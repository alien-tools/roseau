package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class InterfaceBuilder extends TypeBuilder {
	public List<TypeReference<TypeDecl>> permittedTypes = new ArrayList<>();

	public InterfaceDecl make() {
		var fields = this.fields.stream().map(FieldBuilder::make).toList();
		var methods = this.methods.stream().map(MethodBuilder::make).toList();

		return new InterfaceDecl(qualifiedName, visibility, modifiers, Set.copyOf(annotations), location,
				Set.copyOf(implementedInterfaces), formalTypeParameters, Set.copyOf(fields), Set.copyOf(methods), enclosingType, Set.copyOf(permittedTypes));
	}

	public static InterfaceBuilder from(InterfaceDecl decl) {
		var builder = new InterfaceBuilder();

		builder.mutateWithDecl(decl);

		builder.permittedTypes = new ArrayList<>(decl.getPermittedTypes());

		return builder;
	}
}
