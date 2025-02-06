package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class EnumDecl extends ClassDecl {
	// Just duplicating the simpleName of fields extending CtEnumValue for now, but we should probably refactor this
	private final List<EnumValueDecl> values;

	public EnumDecl(String qualifiedName, AccessModifier visibility, EnumSet<Modifier> modifiers,
	                List<Annotation> annotations, SourceLocation location,
	                List<TypeReference<InterfaceDecl>> implementedInterfaces, List<FieldDecl> fields,
	                List<MethodDecl> methods, TypeReference<TypeDecl> enclosingType,
					List<ConstructorDecl> constructors, List<EnumValueDecl> values) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, Collections.emptyList(),
			fields, methods, enclosingType, null, constructors, List.of());

		this.values = Objects.requireNonNull(values);
	}

	@Override
	public boolean isEnum() {
		return true;
	}

	public List<EnumValueDecl> getValues() {
		return Collections.unmodifiableList(values);
	}

	@Override
	public String toString() {
		return """
			%s enum %s
			  %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, values, fields, methods);
	}
}
