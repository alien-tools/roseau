package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.utils.StringUtils;

import java.util.Locale;

/**
 * Java's four legal access modifiers.
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

	public String toCapitalize() {
		return StringUtils.splitSpecialCharsAndCapitalize(toString());
	}
}
