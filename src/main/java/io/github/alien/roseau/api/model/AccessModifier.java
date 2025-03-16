package io.github.alien.roseau.api.model;

import java.util.Locale;

/**
 * All legal Java access modifiers
 */
public enum AccessModifier {
	PRIVATE,
	PROTECTED,
	PUBLIC,
	PACKAGE_PRIVATE;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}
}
