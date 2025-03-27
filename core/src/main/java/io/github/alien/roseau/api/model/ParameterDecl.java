package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.ITypeReference;

/**
 * An {@link ExecutableDecl}'s parameter.
 *
 * @param name      the name of this parameter
 * @param type      the type of this parameter
 * @param isVarargs whether this parameter is the variadic parameter of its {@link ExecutableDecl}
 */
public record ParameterDecl(
	String name,
	ITypeReference type,
	boolean isVarargs
) {
	public ParameterDecl {
		Preconditions.checkNotNull(name);
		Preconditions.checkNotNull(type);
	}

	@Override
	public String toString() {
		return "%s%s %s".formatted(type, isVarargs ? "..." : "", name);
	}
}
