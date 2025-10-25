package io.github.alien.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A type declaration in an API, either a {@link ClassDecl}, {@link InterfaceDecl}, {@link AnnotationDecl},
 * {@link EnumDecl}, or {@link RecordDecl}. Type declarations can implement interfaces, declare formal type parameters,
 * contain fields and methods, and be nested within other type declarations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "typeKind")
public abstract sealed class TypeDecl extends Symbol permits ClassDecl, InterfaceDecl, AnnotationDecl {
	protected final Set<TypeReference<InterfaceDecl>> implementedInterfaces;
	protected final List<FormalTypeParameter> formalTypeParameters;
	protected final Set<FieldDecl> fields;
	protected final Set<MethodDecl> methods;
	protected final TypeReference<TypeDecl> enclosingType;
	protected final Set<TypeReference<TypeDecl>> permittedTypes;

	protected TypeDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                   Set<Annotation> annotations, SourceLocation location,
	                   Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                   List<FormalTypeParameter> formalTypeParameters, Set<FieldDecl> fields, Set<MethodDecl> methods,
	                   TypeReference<TypeDecl> enclosingType, Set<TypeReference<TypeDecl>> permittedTypes) {
		super(qualifiedName, visibility, modifiers, annotations, location);
		Preconditions.checkNotNull(implementedInterfaces);
		Preconditions.checkNotNull(formalTypeParameters);
		Preconditions.checkNotNull(fields);
		Preconditions.checkNotNull(methods);
		Preconditions.checkArgument(enclosingType != null ||
				Set.of(AccessModifier.PUBLIC, AccessModifier.PACKAGE_PRIVATE).contains(visibility),
			"Top-level type declarations are either PUBLIC or PACKAGE_PRIVATE");
		this.implementedInterfaces = ImmutableSet.copyOf(implementedInterfaces);
		this.formalTypeParameters = List.copyOf(formalTypeParameters);
		this.fields = ImmutableSet.copyOf(fields);
		this.methods = ImmutableSet.copyOf(methods);
		this.enclosingType = enclosingType;
		this.permittedTypes = ImmutableSet.copyOf(permittedTypes);
	}

	public boolean isNested() {
		return enclosingType != null;
	}

	public boolean isClass() {
		return false;
	}

	public boolean isInterface() {
		return false;
	}

	public boolean isEnum() {
		return false;
	}

	public boolean isRecord() {
		return false;
	}

	public boolean isAnnotation() {
		return false;
	}

	public boolean isSealed() {
		return modifiers.contains(Modifier.SEALED);
	}

	public boolean isNonSealed() {
		return modifiers.contains(Modifier.NON_SEALED);
	}

	public boolean isAbstract() {
		return modifiers.contains(Modifier.ABSTRACT);
	}

	public Set<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		return implementedInterfaces;
	}

	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	public Set<FieldDecl> getDeclaredFields() {
		return fields;
	}

	public Set<MethodDecl> getDeclaredMethods() {
		return methods;
	}

	public Optional<TypeReference<TypeDecl>> getEnclosingType() {
		return Optional.ofNullable(enclosingType);
	}

	public Set<TypeReference<TypeDecl>> getPermittedTypes() {
		return permittedTypes;
	}

	public String getPackageName() {
		return qualifiedName.contains(".")
			? qualifiedName.substring(0, qualifiedName.lastIndexOf('.'))
			: "";
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		TypeDecl other = (TypeDecl) obj;
		return Objects.equals(implementedInterfaces, other.implementedInterfaces)
			&& Objects.equals(formalTypeParameters, other.formalTypeParameters)
			&& Objects.equals(fields, other.fields)
			&& Objects.equals(methods, other.methods)
			&& Objects.equals(enclosingType, other.enclosingType)
			&& Objects.equals(permittedTypes, other.permittedTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), implementedInterfaces, formalTypeParameters, fields, methods,
			enclosingType, permittedTypes);
	}
}
