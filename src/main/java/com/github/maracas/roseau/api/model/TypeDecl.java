package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Represents a type declaration in the library.
 * This class extends the {@link Symbol} class and contains information about the type's kind, fields, methods, constructors, and more.
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type")
@JsonSubTypes({
	@JsonSubTypes.Type(value = ClassDecl.class),
	@JsonSubTypes.Type(value = InterfaceDecl.class),
	@JsonSubTypes.Type(value = AnnotationDecl.class),
	@JsonSubTypes.Type(value = EnumDecl.class),
	@JsonSubTypes.Type(value = RecordDecl.class)
})
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

	protected TypeDecl(String qualifiedName,
	                   AccessModifier visibility,
	                   List<Modifier> modifiers,
	                   SourceLocation position,
	                   TypeReference<TypeDecl> containingType,
	                   List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                   List<FormalTypeParameter> formalTypeParameters,
	                   List<FieldDecl> fields,
	                   List<MethodDecl> methods) {
		super(qualifiedName, visibility, modifiers, position, containingType);
		this.implementedInterfaces = implementedInterfaces;
		this.formalTypeParameters = formalTypeParameters;
		this.fields = fields;
		this.methods = methods;
	}

	@Override
	public boolean isExported() {
		return (isPublic() || (isProtected() && !isEffectivelyFinal()))
			&& (containingType == null || containingType.getResolvedApiType().map(TypeDecl::isExported).orElse(true));
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
		return !modifiers.contains(Modifier.NON_SEALED) && (isFinal() || isSealed());
	}

	public boolean isPublic() {
		return visibility == AccessModifier.PUBLIC;
	}

	public boolean isProtected() {
		return visibility == AccessModifier.PROTECTED;
	}

	public boolean isPrivate() {
		return visibility == AccessModifier.PRIVATE;
	}

	public boolean isPackagePrivate() {
		return visibility == AccessModifier.PACKAGE_PRIVATE;
	}

	public boolean isAbstract() {
		return modifiers.contains(Modifier.ABSTRACT);
	}

	public List<MethodDecl> getAllMethods() {
		return Stream.concat(
			methods.stream(),
			implementedInterfaces.stream()
				.map(TypeReference::getResolvedApiType)
				.flatMap(Optional::stream)
				.map(InterfaceDecl::getAllMethods)
				.flatMap(Collection::stream)
		).toList();
	}

	public List<FieldDecl> getAllFields() {
		return Stream.concat(
			fields.stream(),
			implementedInterfaces.stream()
				.map(TypeReference::getResolvedApiType)
				.flatMap(Optional::stream)
				.map(InterfaceDecl::getAllFields)
				.flatMap(Collection::stream)
		).toList();
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

	public Optional<FieldDecl> findField(String name) {
		return fields.stream()
			.filter(f -> f.getSimpleName().equals(name))
			.findFirst();
	}

	public List<TypeReference<InterfaceDecl>> getAllImplementedInterfaces() {
		return Stream.concat(
			implementedInterfaces.stream(),
			implementedInterfaces.stream()
				.map(TypeReference::getResolvedApiType)
				.flatMap(Optional::stream)
				.map(InterfaceDecl::getAllImplementedInterfaces)
				.flatMap(Collection::stream)
				.distinct()
		).toList();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		TypeDecl typeDecl = (TypeDecl) o;
		return Objects.equals(implementedInterfaces, typeDecl.implementedInterfaces) && Objects.equals(formalTypeParameters, typeDecl.formalTypeParameters) && Objects.equals(fields, typeDecl.fields) && Objects.equals(methods, typeDecl.methods);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), implementedInterfaces, formalTypeParameters, fields, methods);
	}
}
