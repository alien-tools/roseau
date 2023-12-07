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
	@JsonSubTypes.Type(value = ClassDecl.class, name = "ClassDecl"),
	@JsonSubTypes.Type(value = InterfaceDecl.class, name = "InterfaceDecl"),
	@JsonSubTypes.Type(value = AnnotationDecl.class, name = "AnnotationDecl"),
	@JsonSubTypes.Type(value = EnumDecl.class, name = "EnumDecl"),
	@JsonSubTypes.Type(value = RecordDecl.class, name = "RecordDecl")
})
public abstract sealed class TypeDecl extends Symbol implements Type permits ClassDecl, InterfaceDecl, AnnotationDecl {
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
	public boolean isPrimitive() {
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
		return !modifiers.contains(Modifier.NON_SEALED) && (isFinal() || isSealed());
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
			implementedInterfaces.stream()
				.map(TypeReference::getResolvedApiType)
				.flatMap(Optional::stream)
				.map(InterfaceDecl::getAllMethods)
				.flatMap(Collection::stream)
		).toList();
	}

	@Override
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
	@Override
	public List<TypeReference<InterfaceDecl>> getImplementedInterfaces() {
		return implementedInterfaces;
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
	public Optional<MethodDecl> findMethod(String name, List<ITypeReference> parameterTypes) {
		return methods.stream()
			.filter(m -> m.hasSignature(name, parameterTypes))
			.findFirst();
	}

	@Override
	public List<MethodDecl> getMethods() {
		return methods;
	}

	@Override
	public Optional<FieldDecl> findField(String name) {
		return fields.stream()
			.filter(f -> f.getSimpleName().equals(name))
			.findFirst();
	}

	@Override
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
