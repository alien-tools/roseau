package com.github.maracas.roseau.api.model;

/**
 * Enumerates the possible access modifiers in Java.
 */
public enum AccessModifier {
	/**
	 * Private access modifier
	 */
	PRIVATE,

	/**
	 * Protected access modifier
	 */
	PROTECTED,

	/**
	 * Public access modifier
	 */
	PUBLIC,

	/**
	 * Package-private access modifier
	 */
	PACKAGE_PRIVATE;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
