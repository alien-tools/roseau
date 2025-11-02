package io.github.alien.roseau.api.model;

import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;

/**
 * An interface declaration in an {@link LibraryTypes}.
 */
public sealed class InterfaceDecl extends TypeDecl permits AnnotationDecl {
	public InterfaceDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                     Set<Annotation> annotations, SourceLocation location,
	                     Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                     List<FormalTypeParameter> formalTypeParameters, Set<FieldDecl> fields, Set<MethodDecl> methods,
	                     TypeReference<TypeDecl> enclosingType, Set<TypeReference<TypeDecl>> permittedTypes) {
		// §9.1.1.1: interfaces are implicitly abstract
		super(qualifiedName, visibility, Sets.union(modifiers, Set.of(Modifier.ABSTRACT)), annotations, location,
			implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, permittedTypes);
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
}
