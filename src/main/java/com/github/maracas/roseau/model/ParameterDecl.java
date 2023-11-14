package com.github.maracas.roseau.model;

public record ParameterDecl(
	String name,
	TypeReference type,
	boolean isVarargs
) {

}
