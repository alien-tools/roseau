package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.model.reference.TypeReference;

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

	@JsonCreator
	public ClassDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, SourceLocation location,
	                 List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                 List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                 TypeReference<TypeDecl> enclosingType,
	                 TypeReference<ClassDecl> superClass, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, location,
			implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
		this.superClass = superClass;
		this.constructors = constructors;
	}

	@Override
	public boolean isClass() {
		return true;
	}

	@Override
	protected List<MethodDecl> getSuperMethods() {
		return Stream.concat(
			superClass != null
				? superClass.getResolvedApiType().map(cls -> cls.getAllMethods().stream()).orElse(Stream.empty())
				: Stream.empty(),
			super.getSuperMethods().stream()
			).toList();
	}

	@Override
	public List<FieldDecl> getAllFields() {
		return Stream.concat(
			superClass != null
				? superClass.getResolvedApiType().map(cls -> cls.getAllFields().stream()).orElse(Stream.empty())
				: Stream.empty(),
			super.getAllFields().stream()
		).toList();
	}

	@Override
	public boolean isCheckedException() {
		return getAllSuperClasses().stream().anyMatch(cls -> "java.lang.Exception".equals(cls.getQualifiedName()))
			&& getAllSuperClasses().stream().noneMatch(cls -> "java.lang.RuntimeException".equals(cls.getQualifiedName()));
	}

	@Override
	public boolean isEffectivelyFinal() {
		// A class without a subclass-accessible constructor cannot be extended
		// If the class had a default constructor, it would be there
		return super.isEffectivelyFinal() || constructors.isEmpty();
	}

	@Override
	public List<TypeReference<? extends TypeDecl>> getAllSuperTypes() {
		return Stream.concat(
			super.getAllSuperTypes().stream(),
			getAllSuperClasses().stream()
		).toList();
	}

	public Optional<TypeReference<ClassDecl>> getSuperClass() {
		return Optional.ofNullable(superClass);
	}

	@JsonIgnore
	public List<TypeReference<ClassDecl>> getAllSuperClasses() {
		return superClass != null
			? Stream.concat(
					Stream.of(superClass),
					superClass.getResolvedApiType().map(c -> c.getAllSuperClasses().stream()).orElse(Stream.empty())
				).toList()
			: Collections.emptyList();
	}

	@Override
	public List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces() {
		return Stream.concat(
			super.getAllImplementedInterfaces().stream(),
			superClass != null
				? superClass.getResolvedApiType().map(cls -> cls.getAllImplementedInterfaces().stream()).orElse(Stream.empty())
				: Stream.empty()
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
