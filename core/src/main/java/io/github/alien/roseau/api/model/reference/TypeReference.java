package io.github.alien.roseau.api.model.reference;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.utils.StringUtils;

import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A strongly-typed reference to a {@link TypeDecl}. Type references can be parameterized with type arguments (e.g.,
 * {@code List<String>}).
 *
 * @param <T>           The type of {@link TypeDecl} this reference points to
 * @param qualifiedName the qualified name this reference points to
 * @param typeArguments this reference's type arguments
 */
public record TypeReference<T extends TypeDecl>(
	String qualifiedName,
	List<ITypeReference> typeArguments
) implements ITypeReference {
	public TypeReference {
		Preconditions.checkNotNull(qualifiedName);
		Preconditions.checkNotNull(typeArguments);
		typeArguments = List.copyOf(typeArguments);
	}

	public TypeReference(String qualifiedName) {
		this(qualifiedName, List.of());
	}

	/**
	 * A reference to {@link Object}.
	 */
	public static final TypeReference<ClassDecl> OBJECT =
		new TypeReference<>(Object.class.getCanonicalName(), List.of());
	/**
	 * A reference to {@link Record}.
	 */
	public static final TypeReference<ClassDecl> RECORD =
		new TypeReference<>(Record.class.getCanonicalName(), List.of());
	/**
	 * A reference to {@link Enum}.
	 */
	public static final TypeReference<ClassDecl> ENUM =
		new TypeReference<>(Enum.class.getCanonicalName(), List.of());
	/**
	 * A reference to {@link Exception}.
	 */
	public static final TypeReference<ClassDecl> EXCEPTION =
		new TypeReference<>(Exception.class.getCanonicalName(), List.of());
	/**
	 * A reference to {@link RuntimeException}.
	 */
	public static final TypeReference<ClassDecl> RUNTIME_EXCEPTION =
		new TypeReference<>(RuntimeException.class.getCanonicalName(), List.of());
	/**
	 * A reference to {@link IOException}.
	 */
	public static final TypeReference<ClassDecl> IO_EXCEPTION =
		new TypeReference<>(IOException.class.getCanonicalName(), List.of());
	/**
	 * A reference to {@link String}.
	 */
	public static final TypeReference<ClassDecl> STRING =
		new TypeReference<>(String.class.getCanonicalName(), List.of());

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public String getPrettyQualifiedName() {
		return StringUtils.splitSpecialCharsAndCapitalize(getQualifiedName());
	}

	@Override
	public String toString() {
		if (typeArguments.isEmpty()) {
			return qualifiedName;
		}
		return "%s<%s>".formatted(qualifiedName,
			typeArguments.stream().map(ITypeReference::toString).collect(Collectors.joining(",")));
	}
}
