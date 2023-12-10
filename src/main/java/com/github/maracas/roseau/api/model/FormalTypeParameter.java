package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;

import java.util.List;

public record FormalTypeParameter(
	String name,
	List<ITypeReference> bounds
) {

}
