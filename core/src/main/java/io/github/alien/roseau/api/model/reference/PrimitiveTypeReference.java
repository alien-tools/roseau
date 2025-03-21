package io.github.alien.roseau.api.model.reference;

import io.github.alien.roseau.api.utils.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * A reference to a primitive type (e.g., {@code int}, {@code byte}).
 *
 * @param qualifiedName the simple name of this primitive type
 */
public record PrimitiveTypeReference(String qualifiedName) implements ITypeReference {
	public PrimitiveTypeReference {
		Objects.requireNonNull(qualifiedName);
	}

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public String getPrettyQualifiedName() {
		return StringUtils.splitSpecialCharsAndCapitalize(getQualifiedName());
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		if (equals(other)) {
			return true;
		}

		// Narrowing is fine, widening isn't
		if (other instanceof PrimitiveTypeReference(String name)) {
			return switch (qualifiedName) {
				case "byte" ->          List.of("short", "int", "long", "float", "double").contains(name);
				case "short", "char" -> List.of("int", "long", "float", "double").contains(name);
				case "int" ->           List.of("long", "float", "double").contains(name);
				case "long" ->          List.of("float", "double").contains(name);
				case "float" ->         Objects.equals("double", name);
				default -> false;
			};
		}

		return false;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	@Override
	public PrimitiveTypeReference deepCopy() {
		return this;
	}
}
