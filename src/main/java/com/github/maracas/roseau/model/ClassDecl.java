package com.github.maracas.roseau.model;

import java.util.List;
import java.util.stream.Stream;

public sealed class ClassDecl extends TypeDecl permits RecordDecl, EnumDecl {
	/**
	 * The superclass as a type declaration (null if there isn't any).
	 */
	protected final TypeReference superClass;

	/**
	 * List of constructors declared within the class.
	 */
	protected final List<ConstructorDecl> constructors;

	public ClassDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position, TypeReference containingType, List<TypeReference> superInterfaces, List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods, TypeReference superClass, List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, formalTypeParameters, fields, methods);
		this.superClass = superClass;
		this.constructors = constructors;
	}

	@Override
	public boolean isClass() {
		return true;
	}

	@Override
	public boolean isCheckedException() {
		return getAllSuperClasses().stream().anyMatch(cls -> cls.getQualifiedName().equals("java.lang.Exception"))
			&& getAllSuperClasses().stream().noneMatch(cls -> cls.getQualifiedName().equals("java.lang.RuntimeException"));
	}

	public List<TypeReference> getAllSuperClasses() {
		return Stream.concat(
			Stream.of(superClass), ((ClassDecl) superClass.getActualType()).getAllSuperClasses().stream()
		).toList();
	}

	public TypeReference getSuperClass() {
		return superClass;
	}

	public List<ConstructorDecl> getConstructors() {
		return constructors;
	}

	@Override
	public String toString() {
		return """
			Class %s [%s] [%s]
				Containing type: %s
			  Position: %s
			  Fields: %s
			  Methods: %s
			""".formatted(qualifiedName, visibility, modifiers, containingType, position, fields, methods);
	}
}
