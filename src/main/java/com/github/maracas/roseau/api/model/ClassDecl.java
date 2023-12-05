package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;
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

	public ClassDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation location, TypeReference<TypeDecl> containingType, List<TypeReference<InterfaceDecl>> superInterfaces, List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods, TypeReference<ClassDecl> superClass, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, fields, methods);
		this.superClass = superClass;
		this.constructors = constructors;
	}

	@Override
	public boolean isClass() {
		return true;
	}

	@Override
	public List<MethodDecl> getAllMethods() {
		return Stream.concat(
			superClass != null ? superClass.getAllMethods().stream() : Stream.empty(),
			super.getAllMethods().stream()
		).toList();
	}

	@Override
	public boolean isCheckedException() {
		return getAllSuperClasses().stream().anyMatch(cls -> cls.getQualifiedName().equals("java.lang.Exception"))
			&& getAllSuperClasses().stream().noneMatch(cls -> cls.getQualifiedName().equals("java.lang.RuntimeException"));
	}

	@Override
	public boolean isEffectivelyFinal() {
		// A class without a subclass-accessible constructor cannot be extended
		// If the class had a default constructor, it would be there
		return super.isEffectivelyFinal() || constructors.isEmpty();
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
			superClass != null ? superClass.getAllImplementedInterfaces().stream() : Stream.empty()
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
			""".formatted(qualifiedName, visibility, containingType, fields, methods);
	}
}
