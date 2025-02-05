package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class RecordDecl extends ClassDecl {
	private final List<RecordComponentDecl> recordComponents;

	public RecordDecl(String qualifiedName, AccessModifier visibility, EnumSet<Modifier> modifiers,
	                  List<Annotation> annotations, SourceLocation location,
	                  List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                  List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                  TypeReference<TypeDecl> enclosingType, List<ConstructorDecl> constructors,
					  List<RecordComponentDecl> recordComponents) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType, null, constructors, List.of());

		this.recordComponents = Objects.requireNonNull(recordComponents);
	}

	@Override
	public boolean isRecord() {
		return true;
	}

	public List<RecordComponentDecl> getRecordComponents() {
		return Collections.unmodifiableList(recordComponents);
	}

	@Override
	public List<FieldDecl> getDeclaredFields() {
		return super.getDeclaredFields().stream().filter(FieldDecl::isStatic).toList();
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
