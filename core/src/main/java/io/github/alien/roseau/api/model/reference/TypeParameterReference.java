package io.github.alien.roseau.api.model.reference;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.utils.StringUtils;

/**
 * A reference to a formal type parameter or type variable (e.g., {@code A} referring {@code <A>}).
 *
 * @param name the simple name of the pointed type variable
 */
public record TypeParameterReference(
	String name
) implements ITypeReference {
	public TypeParameterReference {
		Preconditions.checkNotNull(name);
	}

	@Override
	public String getQualifiedName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String getPrettyQualifiedName() {
		return StringUtils.splitSpecialCharsAndCapitalize(getQualifiedName());
	}
}
