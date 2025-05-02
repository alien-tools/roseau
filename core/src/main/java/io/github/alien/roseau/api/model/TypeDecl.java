package io.github.alien.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	// It kinda sucks having to cache those, but it really does make a huge difference
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
	 * Checks whether this type is effectively final. A type is effectively final if it cannot be extended in subtypes,
	 * either because it is explicitly declared {@code final}, or {@code sealed} and not {@code non-sealed}, or (in the
	 * case of {@link ClassDecl}) because it has no subclass-accessible constructor.
	 *
	 * @return whether this type is effectively final
	 * @see ClassDecl#isEffectivelyFinal()
	 */
	public boolean isEffectivelyFinal() {
		// FIXME: in fact, a sealed class may not be final if one of its permitted subclass
		//        is explicitly marked as non-sealed...
		return (isFinal() || isSealed()) && !isNonSealed();
	}

	/**
	 * Returns all super types recursively present in the hierarchy starting from this type, excluded. In the case of
	 * {@link ClassDecl}, this includes super classes.
	 *
	 * @return all super types in this type's hierarchy
	 */
	public Stream<TypeReference<? extends TypeDecl>> getAllSuperTypes() {
		return implementedInterfaces.stream()
			.flatMap(ref -> {
				if (ref.getQualifiedName().equals(getQualifiedName())) {
					// Houston, we have a problem
					return Stream.of(ref);
				} else {
					return Stream.concat(Stream.of(ref), ref.getAllSuperTypes());
				}
			})
			.distinct();
	}

	/**
	 * Returns all super interfaces implemented by this type, directly or indirectly, this type excluded.
	 *
	 * @return all interfaces implemented by this type, directly or indirectly
	 */
	public Stream<TypeReference<? extends TypeDecl>> getAllImplementedInterfaces() {
		return getAllSuperTypes()
			.filter(ref -> ref.getResolvedApiType().map(TypeDecl::isInterface).orElse(false))
			.distinct();
	}

	/**
	 * Returns all methods that can be invoked on this type, including those declared in its super types. For each unique
	 * method erasure, returns the most concrete implementation. The returned list is memoized for efficiency.
	 *
	 * @return the most concrete implementation of each {@link MethodDecl} that can be invoked on this type as a
	 * {@link Stream}
	 * @see #getAllFields()
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

	public Stream<MethodDecl> getAllMethodsToImplement() {
		return getAllMethods().filter(m -> {
			if (m.getContainingType().getResolvedApiType().map(TypeDecl::isInterface).orElse(false)) {
				return !m.isDefault() && !m.isStatic();
			}

			return m.isAbstract();
		});
	}

	/**
	 * Returns all fields that can be accessed on this type, including those declared in its super types. In case of
	 * shadowing, returns the visible field. The returned list is memoized for efficiency.
	 *
	 * @return all {@link FieldDecl} that can be accessed on this type as a {@link Stream}
	 * @see #getAllMethods()
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

	/**
	 * Finds a {@link FieldDecl} declared by this type by simple name.
	 *
	 * @param simpleName the simple name of the field to find
	 * @return an {@link Optional} indicating whether the field was found
	 * @throws NullPointerException if {@code simpleName} is null
	 */
	public Optional<FieldDecl> findField(String simpleName) {
		return getAllFields()
			.filter(f -> Objects.equals(f.getSimpleName(), Objects.requireNonNull(simpleName)))
			.findFirst();
	}

	/**
	 * Finds a {@link MethodDecl} declared by this type by erasure.
	 *
	 * @param erasure the erasure of the method to find
	 * @return an {@link Optional} indicating whether the method was found
	 * @throws NullPointerException if {@code erasure} is null
	 * @see MethodDecl#getErasure()
	 */
	public Optional<MethodDecl> findMethod(String erasure) {
		return getAllMethods()
			.filter(m -> Objects.equals(m.getErasure(), Objects.requireNonNull(erasure)))
			.findFirst();
	}

	/**
	 * Checks whether this type is a subtype of the supplied {@link ITypeReference}. Types are subtypes of themselves,
	 * {@link TypeReference#OBJECT}, and all types they implement or extend.
	 *
	 * @param other the {@link ITypeReference} to check for subtyping
	 * @return true if this type is a subtype of {@code other}
	 * @throws NullPointerException if {@code other} is null
	 */
	public boolean isSubtypeOf(ITypeReference other) {
		return Objects.requireNonNull(other).equals(TypeReference.OBJECT)
			|| Objects.equals(qualifiedName, other.getQualifiedName())
			|| getAllSuperTypes().anyMatch(sup -> Objects.equals(sup.getQualifiedName(), other.getQualifiedName()));
	}

	/**
	 * Returns the list of formal type parameters in this type's scope, including (recursively) from its enclosing
	 * types.
	 *
	 * @return all {@link FormalTypeParameter} in this type's scope
	 * @see ExecutableDecl#getFormalTypeParametersInScope()
	 */
	public List<FormalTypeParameter> getFormalTypeParametersInScope() {
		return Stream.concat(formalTypeParameters.stream(),
				getEnclosingType().flatMap(TypeReference::getResolvedApiType)
					.map(TypeDecl::getFormalTypeParametersInScope)
					.orElse(Collections.emptyList())
					.stream())
			.toList();
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
