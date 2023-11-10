package com.github.maracas.roseau.model;

public final class ParameterDecl {
	private final String name;
	private final TypeReference type;
	private final boolean isVarargs;

	public ParameterDecl(String name, TypeReference type, boolean isVarargs) {
		this.name = name;
		this.type = type;
		this.isVarargs = isVarargs;
	}

	public String getName() {
		return name;
	}

	public TypeReference getType() {
		return type;
	}

	public boolean isVarargs() {
		return isVarargs;
	}
}
