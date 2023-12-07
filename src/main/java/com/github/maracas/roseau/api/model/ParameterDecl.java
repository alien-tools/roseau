package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;

public record ParameterDecl(
	String name,
	ITypeReference type,
	boolean isVarargs
) {

}
