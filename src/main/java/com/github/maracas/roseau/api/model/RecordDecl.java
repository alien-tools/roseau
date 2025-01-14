package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public final class RecordDecl extends ClassDecl {
	public RecordDecl(String qualifiedName, AccessModifier visibility, EnumSet<Modifier> modifiers,
	                  List<Annotation> annotations, SourceLocation location,
	                  List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                  List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                  TypeReference<TypeDecl> enclosingType, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType, null, constructors, List.of());
	}

	@Override
	public boolean isRecord() {
		return true;
	}

	public Stream<FieldDecl> getRecordComponents() {
		return super.getDeclaredFields().stream().filter(f -> !f.isStatic());
	}

	public Stream<FieldDecl> getStaticDeclaredFields() {
		return super.getDeclaredFields().stream().filter(FieldDecl::isStatic);
	}

	@Override
	public String toString() {
		return """
			%s record %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, methods);
	}
}
