package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An enum declaration is a {@link TypeDecl} representing a Java enumeration.
 */
public final class EnumDecl extends ClassDecl {
	// Just duplicating the simpleName of fields extending CtEnumValue for now, but we should probably refactor this
	private final List<EnumValueDecl> values;

	public EnumDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                List<Annotation> annotations, SourceLocation location,
	                List<TypeReference<InterfaceDecl>> implementedInterfaces, List<FieldDecl> fields,
	                List<MethodDecl> methods, TypeReference<TypeDecl> enclosingType,
	                List<ConstructorDecl> constructors, List<EnumValueDecl> values) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, Collections.emptyList(),
			fields, methods, enclosingType, TypeReference.ENUM, constructors, List.of());
		this.values = Objects.requireNonNull(List.copyOf(values));
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		EnumDecl enumDecl = (EnumDecl) o;
		return Objects.equals(values, enumDecl.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), values);
	}
}
