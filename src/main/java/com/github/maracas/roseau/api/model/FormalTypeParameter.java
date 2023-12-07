package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.List;

public record FormalTypeParameter(
	String name,
	List<TypeReference<TypeDecl>> bounds
) {

}
