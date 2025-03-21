package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ReflectiveTypeFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An interface declaration in an {@link API}.
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

	@Override
	public InterfaceDecl deepCopy() {
		return new InterfaceDecl(qualifiedName, visibility, modifiers,
			annotations.stream().map(Annotation::deepCopy).toList(), location,
			TypeReference.deepCopy(implementedInterfaces),
			formalTypeParameters.stream().map(FormalTypeParameter::deepCopy).toList(),
			fields.stream().map(FieldDecl::deepCopy).toList(), methods.stream().map(MethodDecl::deepCopy).toList(),
			getEnclosingType().map(TypeReference::deepCopy).orElse(null), getPermittedTypes());
	}

	@Override
	public InterfaceDecl deepCopy(ReflectiveTypeFactory factory) {
		return new InterfaceDecl(qualifiedName, visibility, modifiers,
			annotations.stream().map(a -> a.deepCopy(factory)).toList(), location,
			TypeReference.deepCopy(implementedInterfaces, factory),
			formalTypeParameters.stream().map(fT -> fT.deepCopy(factory)).toList(),
			fields.stream().map(f -> f.deepCopy(factory)).toList(), methods.stream().map(m -> m.deepCopy(factory)).toList(),
			getEnclosingType().map(t -> t.deepCopy(factory)).orElse(null), getPermittedTypes());
	}
}
