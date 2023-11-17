package com.github.maracas.roseau.api.model;

import java.util.List;

public record FormalTypeParameter(
	String name,
	List<TypeReference<TypeDecl>> bounds
) {

}
