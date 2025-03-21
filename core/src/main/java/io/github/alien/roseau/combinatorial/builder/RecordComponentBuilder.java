package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.RecordComponentDecl;

public final class RecordComponentBuilder extends TypeMemberBuilder {
	public boolean isVarargs;

	public RecordComponentDecl make() {
		return new RecordComponentDecl(qualifiedName, annotations, location, containingType, type, isVarargs);
	}
}
