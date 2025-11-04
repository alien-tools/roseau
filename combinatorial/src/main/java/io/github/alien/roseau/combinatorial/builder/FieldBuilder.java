package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.FieldDecl;

import java.util.Set;

public final class FieldBuilder extends TypeMemberBuilder {
	public FieldDecl make() {
		return new FieldDecl(qualifiedName, visibility, modifiers, Set.copyOf(annotations), location,
				containingType, type);
	}

	public static FieldBuilder from(FieldDecl decl) {
		var builder = new FieldBuilder();

		builder.mutateWithDecl(decl);

		return builder;
	}
}
