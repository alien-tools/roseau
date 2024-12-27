package com.github.maracas.roseau.api.visit;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.Annotation;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;

import java.util.Set;
import java.util.stream.Collectors;

public class APIPrettyPrinter implements APIAlgebra<Print> {
	@Override
	public Print api(API it) {
		return () -> "";
	}

	@Override
	public Print classDecl(ClassDecl it) {
		return () -> """
			%s %s class %s %s %s %s {
				%s
				%s
				%s
				%s
			}""".formatted(
				prettyPrint(it.getVisibility()),
				prettyPrint(it.getModifiers()),
				it.getSimpleName(),
				it.getSuperClass().map(ref -> "extends " + ref.getQualifiedName()).orElse(""),
				it.getImplementedInterfaces().isEmpty()
					? ""
					: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
				it.getPermittedTypes().isEmpty()
					? ""
					: "permits " + String.join(", ", it.getPermittedTypes()),
				it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
				it.getConstructors().stream().map(cons -> $(cons).print()).collect(Collectors.joining("\n")),
				it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n")),
				""
			);
	}

	@Override
	public Print interfaceDecl(InterfaceDecl it) {
		return () -> """
			%s %s interface %s %s %s {
				%s
				%s
				%s
			}""".formatted(
			prettyPrint(it.getVisibility()),
			prettyPrint(it.getModifiers()),
			it.getSimpleName(),
			it.getImplementedInterfaces().isEmpty()
				? ""
				: "extends " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
			it.getPermittedTypes().isEmpty()
					? ""
					: "permits " + String.join(", ", it.getPermittedTypes()),
			it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n")),
			""
		);
	}

	@Override
	public Print enumDecl(EnumDecl it) {
		return () -> """
			%s %s enum %s %s %s {
				%s;
				%s
				%s
				%s
			}""".formatted(
			prettyPrint(it.getVisibility()),
			prettyPrint(it.getModifiers()),
			it.getSimpleName(),
			it.getSuperClass().map(ref -> "extends " + ref.getQualifiedName()).orElse(""),
			it.getImplementedInterfaces().isEmpty()
				? ""
				: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
			String.join(", ", it.getValues()),
			it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
			it.getConstructors().stream().map(cons -> $(cons).print()).collect(Collectors.joining("\n")),
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n"))
		);
	}

	@Override
	public Print annotationDecl(AnnotationDecl it) {
		return () -> """
			%s %s @interface %s %s {
				%s
				%s
				%s
			}""".formatted(
			prettyPrint(it.getVisibility()),
			prettyPrint(it.getModifiers()),
			it.getSimpleName(),
			it.getImplementedInterfaces().isEmpty()
				? ""
				: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
			it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n")),
			""
		);
	}

	@Override
	public Print recordDecl(RecordDecl it) {
		return () -> """
			%s %s record %s() %s {
				%s
				%s
				%s
				%s
			}""".formatted(
			prettyPrint(it.getVisibility()),
			prettyPrint(it.getModifiers()),
			it.getSimpleName(),
			it.getImplementedInterfaces().isEmpty()
				? ""
				: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
			it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
			it.getConstructors().stream().map(cons -> $(cons).print()).collect(Collectors.joining("\n")),
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n")),
			""
		);
	}

	@Override
	public Print methodDecl(MethodDecl it) {
		return () -> "\t%s %s %s %s(%s)%s %s".formatted(
			prettyPrint(it.getVisibility()), prettyPrint(it.getModifiers()), it.getType(), it.getSimpleName(),
			it.getParameters().stream().map(p -> $(p).print()).collect(Collectors.joining(", ")),
			!it.getThrownExceptions().isEmpty() ? " throws " + it.getThrownExceptions().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")) : "",
			hasBody(it) ? "{ return %s; }".formatted(defaultValue(it.getType())) : ";"
		);
	}

	String defaultValue(ITypeReference type) {
		if (type.getQualifiedName().equals("int"))
			return "0";
		return "null";
	}

	boolean hasBody(MethodDecl it) {
		if (it.getContainingType().getResolvedApiType().get().isInterface())
			return it.isDefault() || it.isStatic() || it.getVisibility() == AccessModifier.PRIVATE;
		return !it.isAbstract() && !it.isNative();
	}

	@Override
	public Print constructorDecl(ConstructorDecl it) {
		return null;
	}

	@Override
	public Print fieldDecl(FieldDecl it) {
		return () -> "\t%s %s %s %s%s;".formatted(
			prettyPrint(it.getVisibility()), prettyPrint(it.getModifiers()), it.getType(), it.getSimpleName(),
			it.isFinal() || it.getContainingType().getResolvedApiType().get().isInterface() ? " = " + getDefaultValue(it.getType()) : "");
	}

	private String getDefaultValue(ITypeReference ref) {
		if (ref.getQualifiedName().equals("int"))
			return "0";
		return "null";
	}

	@Override
	public Print parameterDecl(ParameterDecl it) {
		return () -> "%s %s".formatted(it.type().getQualifiedName(), it.name());
	}

	@Override
	public <U extends TypeDecl> Print typeReference(TypeReference<U> it) {
		return null;
	}

	@Override
	public Print primitiveTypeReference(PrimitiveTypeReference it) {
		return null;
	}

	@Override
	public Print arrayTypeReference(ArrayTypeReference it) {
		return null;
	}

	@Override
	public Print typeParameterReference(TypeParameterReference it) {
		return null;
	}

	@Override
	public Print wildcardTypeReference(WildcardTypeReference it) {
		return null;
	}

	@Override
	public Print annotation(Annotation it) {
		return null;
	}

	@Override
	public Print formalTypeParameter(FormalTypeParameter it) {
		return null;
	}

	String prettyPrint(Set<Modifier> modifiers) {
		return modifiers.stream().map(m -> m.toString().replaceAll("_", "-")).collect(Collectors.joining(" "));
	}

	String prettyPrint(AccessModifier visibility) {
		if (visibility == AccessModifier.PACKAGE_PRIVATE)
			return "";
		return visibility.toString();
	}
}
