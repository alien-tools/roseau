package io.github.alien.roseau.api.model.reference;

import java.util.List;
import java.util.Objects;

public record PrimitiveTypeReference(String qualifiedName) implements ITypeReference {
	public PrimitiveTypeReference {
		Objects.requireNonNull(qualifiedName);
	}

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		if (equals(other)) {
			return true;
		}

		// Narrowing is fine, widening isn't
		if (other instanceof PrimitiveTypeReference ptr) {
			return switch (qualifiedName) {
				case "byte" ->          List.of("short", "int", "long", "float", "double").contains(ptr.qualifiedName);
				case "short", "char" -> List.of("int", "long", "float", "double").contains(ptr.qualifiedName);
				case "int" ->           List.of("long", "float", "double").contains(ptr.qualifiedName);
				case "long" ->          List.of("float", "double").contains(ptr.qualifiedName);
				case "float" ->         Objects.equals("double", ptr.qualifiedName);
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
