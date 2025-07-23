package io.github.alien.roseau.api.model.reference;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.utils.StringUtils;

/**
 * A reference to a primitive type (e.g., {@code int}, {@code byte}).
 *
 * @param name the simple name of this primitive type
 */
public record PrimitiveTypeReference(
	String name
) implements ITypeReference {
	public PrimitiveTypeReference {
		Preconditions.checkNotNull(name);
	}

	public static final PrimitiveTypeReference BOOLEAN = new PrimitiveTypeReference("boolean");
	public static final PrimitiveTypeReference CHAR = new PrimitiveTypeReference("char");
	public static final PrimitiveTypeReference BYTE = new PrimitiveTypeReference("byte");
	public static final PrimitiveTypeReference SHORT = new PrimitiveTypeReference("short");
	public static final PrimitiveTypeReference INT = new PrimitiveTypeReference("int");
	public static final PrimitiveTypeReference LONG = new PrimitiveTypeReference("long");
	public static final PrimitiveTypeReference FLOAT = new PrimitiveTypeReference("float");
	public static final PrimitiveTypeReference DOUBLE = new PrimitiveTypeReference("double");
	public static final PrimitiveTypeReference VOID = new PrimitiveTypeReference("void");

	@Override
	public String getQualifiedName() {
		return name;
	}

	@Override
	public String getPrettyQualifiedName() {
		return StringUtils.splitSpecialCharsAndCapitalize(getQualifiedName());
	}

	@Override
	public String toString() {
		return name;
	}
}
