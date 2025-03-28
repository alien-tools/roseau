package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.FieldDecl;

public final class FieldBuilder extends TypeMemberBuilder {
	public FieldDecl make() {
		return new FieldDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type);
	}

	public static FieldBuilder from(FieldDecl decl) {
		var builder = new FieldBuilder();

		TypeMemberBuilder.mutateTypeMemberBuilderWithTypeMember(builder, decl);

		return builder;
	}
}
