package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An interface declaration in an {@link LibraryTypes}.
 */
public final class InterfaceDecl extends TypeDecl {
	protected final Set<TypeReference<TypeDecl>> permittedTypes;

	public InterfaceDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                     Set<Annotation> annotations, SourceLocation location,
	                     Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                     List<FormalTypeParameter> formalTypeParameters, Set<FieldDecl> fields, Set<MethodDecl> methods,
	                     TypeReference<TypeDecl> enclosingType, Set<TypeReference<TypeDecl>> permittedTypes) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType);
		Preconditions.checkNotNull(permittedTypes);
		this.permittedTypes = ImmutableSet.copyOf(permittedTypes);
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	public Set<TypeReference<TypeDecl>> getPermittedTypes() {
		return permittedTypes;
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
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		InterfaceDecl other = (InterfaceDecl) obj;
		return Objects.equals(permittedTypes, other.permittedTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), permittedTypes);
	}
}
