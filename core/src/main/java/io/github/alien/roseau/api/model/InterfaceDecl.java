package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An interface declaration in an {@link LibraryTypes}.
 */
public final class InterfaceDecl extends TypeDecl implements ISealableTypeDecl {
	private final List<String> permittedTypes;

	public InterfaceDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                     List<Annotation> annotations, SourceLocation location,
	                     List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                     List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                     TypeReference<TypeDecl> enclosingType, List<String> permittedTypes) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType);
		Preconditions.checkNotNull(permittedTypes);
		this.permittedTypes = List.copyOf(permittedTypes);
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public List<String> getPermittedTypes() {
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
		InterfaceDecl interfaceDecl = (InterfaceDecl) o;
		return Objects.equals(permittedTypes, interfaceDecl.permittedTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), permittedTypes);
	}
}
