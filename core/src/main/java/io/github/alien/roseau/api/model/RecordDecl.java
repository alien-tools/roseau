package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Set;

/**
 * A record declaration is a special {@link ClassDecl} within an {@link API}.
 */
public final class RecordDecl extends ClassDecl {
	public RecordDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                  List<Annotation> annotations, SourceLocation location,
	                  List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                  List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                  TypeReference<TypeDecl> enclosingType, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType, TypeReference.RECORD, constructors);
	}

	@Override
	public boolean isRecord() {
		return true;
	}

	@Override
	public String toString() {
		return """
			%s record %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, methods);
	}

	@Override
	public RecordDecl deepCopy() {
		return new RecordDecl(qualifiedName, visibility, modifiers, annotations.stream().map(Annotation::deepCopy).toList(),
			location, TypeReference.deepCopy(implementedInterfaces),
			formalTypeParameters.stream().map(FormalTypeParameter::deepCopy).toList(),
			fields.stream().map(FieldDecl::deepCopy).toList(), methods.stream().map(MethodDecl::deepCopy).toList(),
			getEnclosingType().map(TypeReference::deepCopy).orElse(null),
			constructors.stream().map(ConstructorDecl::deepCopy).toList());
	}
}
