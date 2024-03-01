package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a type declaration in the library.
 * This class extends the {@link Symbol} class and contains information about the type's kind, fields, methods, constructors, and more.
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

	protected TypeDecl(String qualifiedName,
	                   AccessModifier visibility,
	                   List<Modifier> modifiers,
										 List<Annotation> annotations,
	                   SourceLocation location,
	                   List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                   List<FormalTypeParameter> formalTypeParameters,
	                   List<FieldDecl> fields,
	                   List<MethodDecl> methods,
	                   TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, annotations, location);
		this.implementedInterfaces = implementedInterfaces;
		this.formalTypeParameters = formalTypeParameters;
		this.fields = fields;
		this.methods = methods;
		this.enclosingType = enclosingType;
	}

	@Override
	public boolean isExported() {
		boolean isExported = isPublic() || (isProtected() && !isEffectivelyFinal());
		boolean isParentExported = !isNested()
			|| enclosingType.getResolvedApiType().map(TypeDecl::isExported).orElse(true);

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

	public boolean isStatic() {
		return modifiers.contains(Modifier.STATIC);
	}

	public boolean isFinal() {
		return modifiers.contains(Modifier.FINAL);
	}

	public boolean isSealed() {
		return modifiers.contains(Modifier.SEALED);
	}

	public boolean isEffectivelyFinal() {
		// FIXME: in fact, a sealed class may not be final if one of its permitted subclass
		//        is explicitly marked as non-sealed
		return (isFinal() || isSealed()) && !modifiers.contains(Modifier.NON_SEALED);
	}

	public boolean isPublic() {
		return AccessModifier.PUBLIC == visibility;
	}

	public boolean isProtected() {
		return AccessModifier.PROTECTED == visibility;
	}

	public boolean isPrivate() {
		return AccessModifier.PRIVATE == visibility;
	}

	public boolean isPackagePrivate() {
		return AccessModifier.PACKAGE_PRIVATE == visibility;
	}

	public boolean isAbstract() {
		return modifiers.contains(Modifier.ABSTRACT);
	}

	public Stream<TypeReference<? extends TypeDecl>> getAllSuperTypes() {
		return implementedInterfaces.stream()
			.flatMap(ref -> Stream.concat(
					Stream.of(ref),
					ref.getResolvedApiType()
						.map(TypeDecl::getAllSuperTypes)
						.orElseGet(Stream::empty)
			))
			.distinct();
	}

	public Stream<TypeReference<? extends TypeDecl>> getAllImplementedInterfaces() {
		return getAllSuperTypes()
			.filter(ref -> ref.getResolvedApiType().map(TypeDecl::isInterface).orElse(false))
			.distinct();
	}

	public Stream<MethodDecl> getAllMethods() {
		List<MethodDecl> allMethods = Stream.concat(
			methods.stream(),
			getAllSuperTypes()
				.map(TypeReference::getResolvedApiType)
				.flatMap(t -> t.map(TypeDecl::getMethods).orElseGet(Collections::emptyList).stream())
		).distinct().toList();

		// Huge performance bottleneck
		return allMethods.stream()
			.filter(m1 -> allMethods.stream().noneMatch(m2 -> !m2.equals(m1) && m2.isOverriding(m1)));
	}

	public Stream<FieldDecl> getAllFields() {
		return Stream.concat(
			fields.stream(),
			getAllSuperTypes()
				.map(TypeReference::getResolvedApiType)
				.flatMap(t -> t.map(TypeDecl::getFields).orElseGet(Collections::emptyList).stream())
		).distinct();
	}

	/**
	 * Retrieves the superinterfaces of the type as typeDeclarations.
	 *
	 * @return Type's superinterfaces as typeDeclarations
	 */
	public List<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		return implementedInterfaces;
	}

	/**
	 * Retrieves the list of formal type parameters for generic types.
	 *
	 * @return List of formal type parameters
	 */
	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	/**
	 * Retrieves the list of fields declared within the type.
	 *
	 * @return List of fields declared within the type
	 */
	public List<FieldDecl> getFields() {
		return fields;
	}

	public List<MethodDecl> getMethods() {
		return methods;
	}

	public Optional<TypeReference<TypeDecl>> getEnclosingType() {
		return Optional.ofNullable(enclosingType);
	}

	public Optional<FieldDecl> findField(String name) {
		return getAllFields()
			.filter(f -> f.getSimpleName().equals(name))
			.findFirst();
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
			&& Objects.equals(methods, typeDecl.methods);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), implementedInterfaces, formalTypeParameters, fields, methods);
	}
}
