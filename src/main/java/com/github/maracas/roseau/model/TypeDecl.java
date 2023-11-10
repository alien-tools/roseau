package com.github.maracas.roseau.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a type declaration in the library.
 * This class extends the {@link Symbol} class and contains information about the type's kind, fields, methods, constructors, and more.
 */
public abstract sealed class TypeDecl extends Symbol permits ClassDecl, InterfaceDecl, AnnotationDecl {
	protected final List<TypeReference> superInterfaces;

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

	protected TypeDecl(String qualifiedName, AccessModifier visibility, List<Modifier> modifiers, String position,
	                   TypeReference containingType, List<TypeReference> superInterfaces, List<FormalTypeParameter> formalTypeParameters,
	                   List<FieldDecl> fields, List<MethodDecl> methods) {
		super(qualifiedName, visibility, modifiers, position, containingType);
		this.superInterfaces = superInterfaces;
		this.formalTypeParameters = formalTypeParameters;
		this.fields = fields;
		this.methods = methods;
	}

	public boolean isNested() {
		return containingType != null;
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

	public boolean isCheckedException() {
		return false;
	}

	public List<MethodDecl> getAllMethods() {
		return Stream.concat(
			methods.stream(),
			superInterfaces.stream()
				.map(intf -> intf.getActualType().getAllMethods())
				.flatMap(Collection::stream)
		).toList();
	}

	/**
	 * Retrieves the superinterfaces of the type as typeDeclarations.
	 *
	 * @return Type's superinterfaces as typeDeclarations
	 */
	public List<TypeReference> getSuperInterfaces() {
		return superInterfaces;
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

	/**
	 * Retrieves the list of methods declared within the type.
	 *
	 * @return List of methods declared within the type
	 */
	public List<MethodDecl> getMethods() {
		return methods;
	}
}
