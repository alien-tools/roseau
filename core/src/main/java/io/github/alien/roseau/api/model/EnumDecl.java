package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An enum declaration is a {@link TypeDecl} representing a Java enumeration.
 */
public final class EnumDecl extends ClassDecl {
	// Just duplicating the simpleName of fields extending CtEnumValue for now, but we should probably refactor this
	private final Set<EnumValueDecl> values;

	public EnumDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                Set<Annotation> annotations, SourceLocation location,
	                Set<TypeReference<InterfaceDecl>> implementedInterfaces, Set<FieldDecl> fields,
	                Set<MethodDecl> methods, TypeReference<TypeDecl> enclosingType,
	                Set<ConstructorDecl> constructors, Set<EnumValueDecl> values) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, List.of(),
			fields, methods, enclosingType, TypeReference.ENUM, constructors, Set.of());
		Preconditions.checkNotNull(values);
		this.values = Set.copyOf(values);
	}

	@Override
	public boolean isEnum() {
		return true;
	}

	public Set<EnumValueDecl> getValues() {
		return values;
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
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		return obj instanceof EnumDecl other
			&& Objects.equals(values, other.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), values);
	}
}
