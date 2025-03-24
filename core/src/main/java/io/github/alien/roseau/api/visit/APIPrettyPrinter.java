package io.github.alien.roseau.api.visit;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;

import java.util.Arrays;
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
			%s

			%s %s class %s %s %s %s {
			%s

			%s

			%s
			}""".formatted(
				getPackageFromQualifiedName(it.getQualifiedName()),
				prettyPrint(it.getVisibility()),
				prettyPrint(it.getModifiers()),
				it.getSimpleName(),
				it.getSuperClass().equals(TypeReference.OBJECT) ? "" : "extends %s".formatted(it.getSuperClass().getQualifiedName()),
				it.getImplementedInterfaces().isEmpty()
					? ""
					: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
				it.getPermittedTypes().isEmpty()
					? ""
					: "permits " + String.join(", ", it.getPermittedTypes()),
				it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
				it.getDeclaredConstructors().stream().map(cons -> $(cons).print()).collect(Collectors.joining("\n")),
				it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n"))
			);
	}

	@Override
	public Print interfaceDecl(InterfaceDecl it) {
		return () -> """
			%s

			%s %s interface %s %s %s {
			%s

			%s
			}""".formatted(
			getPackageFromQualifiedName(it.getQualifiedName()),
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
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n"))
		);
	}

	@Override
	public Print enumDecl(EnumDecl it) {
		return () -> """
			%s

			%s %s enum %s %s {
				%s;

			%s

			%s

			%s
			}""".formatted(
			getPackageFromQualifiedName(it.getQualifiedName()),
			prettyPrint(it.getVisibility()),
			prettyPrint(it.getModifiers()),
			it.getSimpleName(),
			it.getImplementedInterfaces().isEmpty()
				? ""
				: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
			it.getValues().stream().map(eV -> $(eV).print()).collect(Collectors.joining(", ")),
			it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
			it.getDeclaredConstructors().stream().map(cons -> $(cons).print()).collect(Collectors.joining("\n")),
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n"))
		);
	}

	@Override
	public Print annotationDecl(AnnotationDecl it) {
		return () -> """
			%s

			%s %s @interface %s %s {
				%s
				%s
			}""".formatted(
			getPackageFromQualifiedName(it.getQualifiedName()),
			prettyPrint(it.getVisibility()),
			prettyPrint(it.getModifiers()),
			it.getSimpleName(),
			it.getImplementedInterfaces().isEmpty()
				? ""
				: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
			it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n"))
		);
	}

	@Override
	public Print recordDecl(RecordDecl it) {
		return () -> """
			%s

			%s %s record %s(%s) %s {
			%s

			%s

			%s
			}""".formatted(
			getPackageFromQualifiedName(it.getQualifiedName()),
			prettyPrint(it.getVisibility()),
			prettyPrint(it.getModifiers()),
			it.getSimpleName(),
			it.getRecordComponents().stream().map(rC -> $(rC).print()).collect(Collectors.joining(", ")),
			it.getImplementedInterfaces().isEmpty()
				? ""
				: "implements " + it.getImplementedInterfaces().stream().map(TypeReference::getQualifiedName).collect(Collectors.joining(", ")),
			it.getDeclaredFields().stream().map(f -> $(f).print()).collect(Collectors.joining("\n")),
			it.getDeclaredConstructors().stream().map(cons -> $(cons).print()).collect(Collectors.joining("\n")),
			it.getDeclaredMethods().stream().map(m -> $(m).print()).collect(Collectors.joining("\n"))
		);
	}

	@Override
	public Print methodDecl(MethodDecl it) {
		return () -> "\t%s %s %s %s(%s)%s %s".formatted(
			prettyPrint(it.getVisibility()), prettyPrint(it.getModifiers()), it.getType(), it.getSimpleName(),
			it.getParameters().stream().map(p -> $(p).print()).collect(Collectors.joining(", ")),
			!it.getThrownExceptions().isEmpty() ? " throws " + it.getThrownExceptions().stream().map(ITypeReference::getQualifiedName).collect(Collectors.joining(", ")) : "",
			hasBody(it)
					? it.getType().getQualifiedName().equals("void")
						? "{}"
						: "{ return %s; }".formatted(getDefaultValue(it.getType()))
					: ";"
		);
	}

	@Override
	public Print constructorDecl(ConstructorDecl it) {
		return () -> "\t%s %s(%s)%s {}".formatted(
				prettyPrint(it.getVisibility()),
				it.getSimpleName(),
				it.getParameters().stream().map(p -> $(p).print()).collect(Collectors.joining(", ")),
				!it.getThrownExceptions().isEmpty() ? " throws " + it.getThrownExceptions().stream().map(ITypeReference::getQualifiedName).collect(Collectors.joining(", ")) : ""
		);
	}

	@Override
	public Print fieldDecl(FieldDecl it) {
		return () -> "\t%s %s %s %s%s;".formatted(
			prettyPrint(it.getVisibility()), prettyPrint(it.getModifiers()), it.getType(), it.getSimpleName(),
			it.isFinal() || it.getContainingType().getResolvedApiType().get().isInterface() ? " = " + getDefaultValue(it.getType()) : "");
	}

	@Override
	public Print parameterDecl(ParameterDecl it) {
		return () -> it.isVarargs()
				? "%s ...%s".formatted(it.type().getQualifiedName(), it.name())
				: "%s %s".formatted(it.type().getQualifiedName(), it.name());
	}

	@Override
	public Print enumValueDecl(EnumValueDecl it) {
		return it::toString;
	}

	@Override
	public Print recordComponentDecl(RecordComponentDecl it) {
		return it::toString;
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

	private static String getPackageFromQualifiedName(String qualifiedName) {
		var qualifiedNameSplit = qualifiedName.split("\\.");
		if (qualifiedNameSplit.length == 1)
			return "";

		var packageName = Arrays.stream(qualifiedNameSplit)
				.limit(qualifiedNameSplit.length - 1)
				.collect(Collectors.joining("."));

		return "package %s;".formatted(packageName);
	}

	private static String prettyPrint(Set<Modifier> modifiers) {
		return modifiers.stream().map(m -> m.toString().replaceAll("_", "-")).collect(Collectors.joining(" "));
	}

	private static String prettyPrint(AccessModifier visibility) {
		if (visibility == AccessModifier.PACKAGE_PRIVATE)
			return "";
		return visibility.toString();
	}

	private static boolean hasBody(MethodDecl it) {
		if (it.getContainingType().getResolvedApiType().get().isInterface())
			return it.isDefault() || it.isStatic() || it.getVisibility() == AccessModifier.PRIVATE;
		return !it.isAbstract() && !it.isNative();
	}

	private static String getDefaultValue(ITypeReference ref) {
		if (ref.getQualifiedName().equals("int"))
			return "0";
		return "null";
	}
}
