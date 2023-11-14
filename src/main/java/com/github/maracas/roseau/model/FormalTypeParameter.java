package com.github.maracas.roseau.model;

import java.util.List;

public record FormalTypeParameter(
	String name,
	List<TypeReference> bounds
) {

}
