package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.utils.StringUtils;

import java.util.Locale;

/**
 * Every legal non-access Java modifier
 */
public enum Modifier {
	STATIC,
	FINAL,
	ABSTRACT,
	SYNCHRONIZED,
	VOLATILE,
	TRANSIENT,
	NATIVE,
	STRICTFP,
	SEALED,
	NON_SEALED,
	DEFAULT;

	@Override
	public String toString() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String toCapitalize() {
		return StringUtils.splitSpecialCharsAndCapitalize(toString());
	}
}
