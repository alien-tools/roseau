package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;

/**
 * An interface declaration in an {@link LibraryTypes}.
 */
public final class InterfaceDecl extends TypeDecl {
	public InterfaceDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                     List<Annotation> annotations, SourceLocation location,
	                     List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                     List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                     TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType);
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public String toString() {
		return """
			%s interface %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, methods);
	}

	@Override
	public InterfaceDecl deepCopy() {
		return new InterfaceDecl(qualifiedName, visibility, modifiers,
			annotations.stream().map(Annotation::deepCopy).toList(), location,
			TypeReference.deepCopy(implementedInterfaces),
			formalTypeParameters.stream().map(FormalTypeParameter::deepCopy).toList(),
			fields.stream().map(FieldDecl::deepCopy).toList(), methods.stream().map(MethodDecl::deepCopy).toList(),
			getEnclosingType().map(TypeReference::deepCopy).orElse(null));
	}
}
