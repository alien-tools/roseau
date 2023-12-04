package com.github.maracas.roseau.api.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a type declaration in the library.
 * This class extends the {@link Symbol} class and contains information about the type's kind, fields, methods, constructors, and more.
 */
public abstract sealed class TypeDecl extends Symbol implements Type permits ClassDecl, InterfaceDecl, AnnotationDecl {
	protected final List<TypeReference<InterfaceDecl>> superInterfaces;

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

	protected TypeDecl(String qualifiedName, AccessModifier visibility, boolean isExported, List<Modifier> modifiers, SourceLocation position,
	                   TypeReference<TypeDecl> containingType, List<TypeReference<InterfaceDecl>> superInterfaces,
	                   List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods) {
		super(qualifiedName, visibility, isExported, modifiers, position, containingType);
		this.superInterfaces = superInterfaces;
		this.formalTypeParameters = formalTypeParameters;
		this.fields = fields;
		this.methods = methods;
	}

	@Override
	public boolean isNested() {
		return containingType != null;
	}

	@Override
	public boolean isClass() {
		return false;
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	@Override
	public boolean isEnum() {
		return false;
	}

	@Override
	public boolean isRecord() {
		return false;
	}

	@Override
	public boolean isAnnotation() {
		return false;
	}

	@Override
	public boolean isCheckedException() {
		return false;
	}

	@Override
	public boolean isStatic() {
		return modifiers.contains(Modifier.STATIC);
	}

	@Override
	public boolean isFinal() {
		return modifiers.contains(Modifier.FINAL);
	}

	@Override
	public boolean isSealed() {
		return modifiers.contains(Modifier.SEALED);
	}

	@Override
	public boolean isEffectivelyFinal() {
		// FIXME: in fact, a sealed class may not be final if one of its permitted subclass
		//        is explicitly marked as non-sealed
		return isFinal() || isSealed();
	}

	@Override
	public boolean isPublic() {
		return visibility == AccessModifier.PUBLIC;
	}

	@Override
	public boolean isProtected() {
		return visibility == AccessModifier.PROTECTED;
	}

	@Override
	public boolean isPrivate() {
		return visibility == AccessModifier.PRIVATE;
	}

	@Override
	public boolean isPackagePrivate() {
		return visibility == AccessModifier.PACKAGE_PRIVATE;
	}

	@Override
	public boolean isAbstract() {
		return modifiers.contains(Modifier.ABSTRACT);
	}

	@Override
	public List<MethodDecl> getAllMethods() {
		return Stream.concat(
			methods.stream(),
			superInterfaces.stream()
				.map(TypeReference::getAllMethods)
				.flatMap(Collection::stream)
		).toList();
	}

	/**
	 * Retrieves the superinterfaces of the type as typeDeclarations.
	 *
	 * @return Type's superinterfaces as typeDeclarations
	 */
	@Override
	public List<TypeReference<InterfaceDecl>> getSuperInterfaces() {
		return superInterfaces;
	}

	/**
	 * Retrieves the list of formal type parameters for generic types.
	 *
	 * @return List of formal type parameters
	 */
	@Override
	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	/**
	 * Retrieves the list of fields declared within the type.
	 *
	 * @return List of fields declared within the type
	 */
	@Override
	public List<FieldDecl> getFields() {
		return fields;
	}

	@Override
	public Optional<FieldDecl> getField(String name) {
		return fields.stream()
			.filter(f -> f.getSimpleName().equals(name))
			.findFirst();
	}

	/**
	 * Retrieves the list of methods declared within the type.
	 *
	 * @return List of methods declared within the type
	 */
	@Override
	public List<MethodDecl> getMethods() {
		return methods;
	}
}
