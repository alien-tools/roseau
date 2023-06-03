package com.github.maracas.roseau.model;

import spoon.reflect.declaration.ModifierKind;

public enum VisibilityKind {
	PUBLIC, PROTECTED;

	public static VisibilityKind of(ModifierKind visibility) {
		return switch (visibility) {
			case PUBLIC    -> VisibilityKind.PUBLIC;
			case PROTECTED -> VisibilityKind.PROTECTED;
			default -> throw new IllegalArgumentException("Expected public or protected, got " + visibility);
		};
	}
}
