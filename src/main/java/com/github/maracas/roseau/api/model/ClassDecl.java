package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public sealed class ClassDecl extends TypeDecl permits RecordDecl, EnumDecl {
	/**
	 * The superclass as a type reference (null if there isn't any).
	 */
	protected final TypeReference<ClassDecl> superClass;

	/**
	 * List of constructors declared within the class.
	 */
	protected final List<ConstructorDecl> constructors;

	public ClassDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, List<Annotation> annotations,
	                 SourceLocation location, List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                 List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                 TypeReference<TypeDecl> enclosingType,
	                 TypeReference<ClassDecl> superClass, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, annotations, location,
			implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
		this.superClass = superClass;
		this.constructors = constructors;
	}

	@Override
	public boolean isClass() {
		return true;
	}

	public boolean isCheckedException() {
		List<String> superClasses = getAllSuperClasses().stream().map(TypeReference::getQualifiedName).toList();

		return "java.lang.Exception".equals(qualifiedName) || superClasses.contains("java.lang.Exception") && !isUncheckedException();
	}

	public boolean isUncheckedException() {
		List<String> superClasses = getAllSuperClasses().stream().map(TypeReference::getQualifiedName).toList();

		return "java.lang.RuntimeException".equals(qualifiedName)
			|| superClasses.contains("java.lang.RuntimeException");
	}

	@Override
	public boolean isEffectivelyFinal() {
		// A class without a subclass-accessible constructor cannot be extended
		// If the class had a default constructor, it would be there
		return super.isEffectivelyFinal() || constructors.isEmpty();
	}

	@Override
	public List<TypeReference<? extends TypeDecl>> getAllSuperTypes() {
		Stream<TypeReference<? extends TypeDecl>> interfaceHierarchy = super.getAllSuperTypes().stream();
		Stream<TypeReference<? extends TypeDecl>> superClassHierarchy = Stream.concat(
		getAllSuperClasses().stream(),
		getAllSuperClasses().stream()
			.map(TypeReference::getResolvedApiType)
			.flatMap(Optional::stream)
			.map(ClassDecl::getAllSuperTypes)
			.flatMap(Collection::stream)
		);

		return Stream.concat(interfaceHierarchy, superClassHierarchy).toList();
	}

	public Optional<TypeReference<ClassDecl>> getSuperClass() {
		return Optional.ofNullable(superClass);
	}

	public List<TypeReference<ClassDecl>> getAllSuperClasses() {
		return superClass == null
			? Collections.emptyList()
			: Stream.concat(
				Stream.of(superClass),
				superClass.getResolvedApiType().map(sup -> sup.getAllSuperClasses().stream()).orElse(Stream.empty())
			).distinct().toList();
	}

	public List<ConstructorDecl> getConstructors() {
		return constructors;
	}

	@Override
	public String toString() {
		return """
			class %s [%s] (%s)
			  %s
			  %s
			""".formatted(qualifiedName, visibility, enclosingType, fields, methods);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		ClassDecl classDecl = (ClassDecl) o;
		return Objects.equals(superClass, classDecl.superClass) && Objects.equals(constructors, classDecl.constructors);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), superClass, constructors);
	}
}
