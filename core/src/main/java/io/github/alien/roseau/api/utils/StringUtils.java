package io.github.alien.roseau.api.utils;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class StringUtils {
	public static String splitSpecialCharsAndCapitalize(String s) {
		return Arrays.stream(s
				.replaceAll("\\[]", "Arr")
				.replaceAll("\\.\\.\\.", "Varargs")
				.split("\\W+|_+")
			)
			.map(StringUtils::capitalizeFirstLetter)
			.collect(Collectors.joining());
	}

	public static String cleanQualifiedNameForType(ITypeReference typeReference) {
		return cleanQualifiedName(typeReference.getQualifiedName());
	}

	public static String cleanQualifiedNameForType(TypeDecl typeDecl) {
		return cleanQualifiedName(typeDecl.getQualifiedName());
	}

	public static String cleanSimpleNameForType(TypeDecl typeDecl) {
		var parts = typeDecl.getSimpleName().split("\\$");
		return parts[parts.length - 1];
	}

	public static String capitalizeFirstLetter(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private static String cleanQualifiedName(String qualifiedName) {
		return qualifiedName.replaceAll("\\$", ".");
	}
}
