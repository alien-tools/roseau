package com.github.maracas.roseau.api.model;

public record ParameterDecl(
	String name,
	TypeReference<TypeDecl> type,
	boolean isVarargs
) {

}
