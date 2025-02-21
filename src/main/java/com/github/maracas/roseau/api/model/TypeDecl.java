package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a type declaration in the API, either a {@link ClassDecl}, {@link InterfaceDecl}
 * or {@link AnnotationDecl}.
 * Type declarations may implement interfaces, declare formal type parameters, contain fields and methods,
 * and be nested within other type declarations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "typeKind")
public abstract sealed class TypeDecl extends Symbol permits ClassDecl, InterfaceDecl, AnnotationDecl {
	protected final List<TypeReference<InterfaceDecl>> implementedInterfaces;

	/**
	 * List of formal type parameters for generic types.
	 */
	protected final List<FormalTypeParameter> formalTypeParameters;

	/**
	 * List of fields declared within the type.
	 */
	protected final List<FieldDecl> fields;

	/**
	 * List of methods declared within the type.
	 */
	protected final List<MethodDecl> methods;

	protected final TypeReference<TypeDecl> enclosingType;

	/**
	 * It kinda sucks having to cache that, but it really makes a huge difference
	 */
	@JsonIgnore
	protected List<MethodDecl> allMethods;
	@JsonIgnore
	protected List<FieldDecl> allFields;

	protected TypeDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                   List<Annotation> annotations, SourceLocation location,
	                   List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                   List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                   TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, annotations, location);
		this.implementedInterfaces = Objects.requireNonNull(implementedInterfaces);
		this.formalTypeParameters = Objects.requireNonNull(formalTypeParameters);
		this.fields = Objects.requireNonNull(fields);
		this.methods = Objects.requireNonNull(methods);
		this.enclosingType = enclosingType;
	}

	@Override
	public boolean isExported() {
		boolean isExported = isPublic() || (isProtected() && !isEffectivelyFinal());
		boolean isParentExported = !isNested() || enclosingType.isExported();

		return isExported && isParentExported;
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

	/**
	 * Checks whether this type is effectively final, i.e. if it cannot be extended regardless
	 * of its {@code final} modifier.
	 *
	 * @return whether the type is effectively final
	 */
	public boolean isEffectivelyFinal() {
		// FIXME: in fact, a sealed class may not be final if one of its permitted subclass
		//        is explicitly marked as non-sealed...
		return (isFinal() || isSealed()) && !isNonSealed();
	}

	/**
	 * Returns every super type in the hierarchy starting from this type, excluded.
	 */
	public Stream<TypeReference<? extends TypeDecl>> getAllSuperTypes() {
		return implementedInterfaces.stream()
			.flatMap(ref -> Stream.concat(Stream.of(ref), ref.getAllSuperTypes()))
			.distinct();
	}

	/**
	 * Returns every interface implemented by this type, directly or indirectly, this type excluded.
	 */
	public Stream<TypeReference<? extends TypeDecl>> getAllImplementedInterfaces() {
		return getAllSuperTypes()
			.filter(ref -> ref.getResolvedApiType().map(TypeDecl::isInterface).orElse(false))
			.distinct();
	}

	/**
	 * Returns all methods that can be invoked on this type, including those declared in its super types.
	 * Returns the most concrete implementation for each unique method erasure.
	 */
	public Stream<MethodDecl> getAllMethods() {
		if (allMethods == null) {
			allMethods = Stream.concat(
				methods.stream(),
				getAllSuperTypes()
					.map(TypeReference::getResolvedApiType)
					.flatMap(t -> t.map(TypeDecl::getDeclaredMethods).orElseGet(Collections::emptyList).stream())
			).collect(Collectors.toMap(
				MethodDecl::getErasure,
				Function.identity(),
				(m1, m2) -> m1.isOverriding(m2) ? m1 : m2
			)).values().stream().toList();
		}

		return allMethods.stream();
	}

	/**
	 * Returns all fields declared by this type, including those of its super types.
	 */
	public Stream<FieldDecl> getAllFields() {
		if (allFields == null) {
			allFields = Stream.concat(
				fields.stream(),
				getAllSuperTypes()
					.map(TypeReference::getResolvedApiType)
					.flatMap(t -> t.map(TypeDecl::getDeclaredFields).orElseGet(Collections::emptyList).stream())
			).collect(Collectors.toMap(
				FieldDecl::getSimpleName,
				Function.identity(),
				(f1, f2) -> f1.isShadowing(f2) ? f1 : f2
			)).values().stream().toList();
		}

		return allFields.stream();
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

	public Optional<FieldDecl> findField(String name) {
		return getAllFields()
			.filter(f -> Objects.equals(f.getSimpleName(), name))
			.findFirst();
	}

	public Optional<MethodDecl> findMethod(String erasure) {
		return getAllMethods()
			.filter(m -> Objects.equals(erasure, m.getErasure()))
			.findFirst();
	}

	public boolean isSubtypeOf(ITypeReference other) {
		return other.equals(TypeReference.OBJECT)
			|| Objects.equals(qualifiedName, other.getQualifiedName())
			|| getAllSuperTypes().anyMatch(sup -> Objects.equals(sup.getQualifiedName(), other.getQualifiedName()));
	}

	public Optional<FormalTypeParameter> resolveTypeParameter(TypeParameterReference tpr) {
		var resolved = formalTypeParameters.stream()
			.filter(ftp -> ftp.name().equals(tpr.getQualifiedName()))
			.findFirst();

		if (resolved.isPresent()) {
			return resolved;
		} else if (enclosingType != null) {
			return enclosingType.getResolvedApiType().flatMap(t -> t.resolveTypeParameter(tpr));
		} else {
			return Optional.empty();
		}
	}

	public Optional<ITypeReference> resolveTypeParameterBound(TypeParameterReference tpr) {
		var ftp = resolveTypeParameter(tpr);

		if (ftp.isPresent()) {
			if (ftp.get().bounds().getFirst() instanceof TypeParameterReference tpr2) {
				return resolveTypeParameterBound(tpr2);
			} else {
				return Optional.of(ftp.get().bounds().getFirst());
			}
		} else if (enclosingType != null) {
			return enclosingType.getResolvedApiType().flatMap(t -> t.resolveTypeParameterBound(tpr));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
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
}
