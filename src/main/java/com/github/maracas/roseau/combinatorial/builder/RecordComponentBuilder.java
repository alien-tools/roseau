package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.RecordComponentDecl;

public final class RecordComponentBuilder extends TypeMemberBuilder {
	public boolean isVarargs;

	public RecordComponentDecl make() {
		return new RecordComponentDecl(qualifiedName, annotations, location, containingType, type, isVarargs);
	}
}
