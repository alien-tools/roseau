package io.github.alien.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
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
	protected final List<TypeReference<InterfaceDecl>> implementedInterfaces;
	protected final List<FormalTypeParameter> formalTypeParameters;
	protected final List<FieldDecl> fields;
	protected final List<MethodDecl> methods;
	protected final TypeReference<TypeDecl> enclosingType;

	protected TypeDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                   List<Annotation> annotations, SourceLocation location,
	                   List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                   List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                   TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, annotations, location);
		Preconditions.checkNotNull(implementedInterfaces);
		Preconditions.checkNotNull(formalTypeParameters);
		Preconditions.checkNotNull(fields);
		Preconditions.checkNotNull(methods);
		this.implementedInterfaces = implementedInterfaces;
		this.formalTypeParameters = formalTypeParameters;
		this.fields = fields;
		this.methods = methods;
		this.enclosingType = enclosingType;
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

	public List<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		return Collections.unmodifiableList(implementedInterfaces);
	}

	public List<FormalTypeParameter> getFormalTypeParameters() {
		return Collections.unmodifiableList(formalTypeParameters);
	}

	public List<FieldDecl> getDeclaredFields() {
		return Collections.unmodifiableList(fields);
	}

	public List<MethodDecl> getDeclaredMethods() {
		return Collections.unmodifiableList(methods);
	}

	public Optional<TypeReference<TypeDecl>> getEnclosingType() {
		return Optional.ofNullable(enclosingType);
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
		TypeDecl typeDecl = (TypeDecl) o;
		return Objects.equals(implementedInterfaces, typeDecl.implementedInterfaces)
			&& Objects.equals(formalTypeParameters, typeDecl.formalTypeParameters)
			&& Objects.equals(fields, typeDecl.fields)
			&& Objects.equals(methods, typeDecl.methods)
			&& Objects.equals(enclosingType, typeDecl.enclosingType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
	}

	@Override
	public abstract TypeDecl deepCopy();
}
