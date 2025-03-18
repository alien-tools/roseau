package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;

import java.util.Objects;

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
) implements DeepCopyable<ParameterDecl> {
	public ParameterDecl {
		Objects.requireNonNull(name);
		Objects.requireNonNull(type);
	}

	@Override
	public ParameterDecl deepCopy() {
		return new ParameterDecl(name, type.deepCopy(), isVarargs);
	}

	@Override
	public String toString() {
		return "%s%s %s".formatted(type, isVarargs ? "..." : "", name);
	}
}
