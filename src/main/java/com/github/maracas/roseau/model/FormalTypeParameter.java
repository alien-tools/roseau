package com.github.maracas.roseau.model;

import java.util.List;

public final class FormalTypeParameter {
	private final String name;
	private final List<TypeReference> bounds;

	public FormalTypeParameter(String name, List<TypeReference> bounds) {
		this.name = name;
		this.bounds = bounds;
	}
}
