package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class InterfaceDecl extends TypeDecl implements ISealableTypeDecl {
	private final List<String> permittedTypes;

	public InterfaceDecl(String qualifiedName, AccessModifier visibility, EnumSet<Modifier> modifiers,
	                     List<Annotation> annotations, SourceLocation location,
	                     List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                     List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                     TypeReference<TypeDecl> enclosingType, List<String> permittedTypes) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType);

		this.permittedTypes = Objects.requireNonNull(permittedTypes);
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public Stream<MethodDecl> getAllMethodsToImplement() {
		return getAllMethods().filter(m -> !m.isDefault() && !m.isStatic());
	}

	@Override
	public List<String> getPermittedTypes() {
		return Collections.unmodifiableList(permittedTypes);
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
