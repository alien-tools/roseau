package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.RecordComponentDecl;

public final class RecordComponentBuilder extends TypeMemberBuilder {
	public boolean isVarargs;

	public RecordComponentDecl make() {
		return new RecordComponentDecl(qualifiedName, annotations, location, containingType, type, isVarargs);
	}

	public static RecordComponentBuilder from(RecordComponentDecl decl) {
		var builder = new RecordComponentBuilder();

		builder.mutateWithDecl(decl);

		builder.isVarargs = decl.isVarargs();

		return builder;
	}
}
