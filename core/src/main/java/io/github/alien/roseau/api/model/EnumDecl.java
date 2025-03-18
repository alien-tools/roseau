package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An enum declaration is a {@link TypeDecl} representing a Java enumeration.
 */
public final class EnumDecl extends ClassDecl {
	public EnumDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                List<Annotation> annotations, SourceLocation location,
	                List<TypeReference<InterfaceDecl>> implementedInterfaces, List<FieldDecl> fields,
	                List<MethodDecl> methods, TypeReference<TypeDecl> enclosingType, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, Collections.emptyList(),
			fields, methods, enclosingType, TypeReference.ENUM, constructors);
	}

	@Override
	public boolean isEnum() {
		return true;
	}

	@Override
	public String toString() {
		return """
			%s enum %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, methods);
	}

	@Override
	public EnumDecl deepCopy() {
		return new EnumDecl(qualifiedName, visibility, modifiers, annotations.stream().map(Annotation::deepCopy).toList(),
			location, TypeReference.deepCopy(implementedInterfaces),
			fields.stream().map(FieldDecl::deepCopy).toList(), methods.stream().map(MethodDecl::deepCopy).toList(),
			getEnclosingType().map(TypeReference::deepCopy).orElse(null),
			constructors.stream().map(ConstructorDecl::deepCopy).toList());
	}
}
