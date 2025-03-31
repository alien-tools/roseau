package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;

public class ParameterBuilder {
	public String name;
	public ITypeReference type;
	public boolean isVarargs;

	public ParameterDecl make() {
		return new ParameterDecl(name, type, isVarargs);
	}

	public static ParameterBuilder from(ParameterDecl decl) {
		var builder = new ParameterBuilder();

		builder.name = decl.name();
		builder.type = decl.type();
		builder.isVarargs = decl.isVarargs();

		return builder;
	}
}
