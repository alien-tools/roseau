package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A class declaration is a {@link TypeDecl} with an optional superclass and a list of {@link ConstructorDecl}.
 * {@link ClassDecl} instantiated without a superclass implicitly extend {@link java.lang.Object}.
 */
public sealed class ClassDecl extends TypeDecl permits RecordDecl, EnumDecl {
	protected final TypeReference<ClassDecl> superClass;
	protected final List<ConstructorDecl> constructors;
	private final List<TypeReference<TypeDecl>> permittedTypes;

	public ClassDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                 List<Annotation> annotations, SourceLocation location,
	                 List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                 List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                 TypeReference<TypeDecl> enclosingType, TypeReference<ClassDecl> superClass,
	                 List<ConstructorDecl> constructors, List<TypeReference<TypeDecl>> permittedTypes) {
		super(qualifiedName, visibility, modifiers, annotations, location,
			implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
		Preconditions.checkNotNull(constructors);
		Preconditions.checkNotNull(permittedTypes);
		this.superClass = superClass != null ? superClass : TypeReference.OBJECT;
		this.constructors = List.copyOf(constructors);
		this.permittedTypes = List.copyOf(permittedTypes);
	}

	@Override
	public boolean isClass() {
		return true;
	}

	/**
	 * Checks whether the current class is effectively abstract. A class is effectively abstract if it is explicitly
	 * declared abstract or if it has no subclass-accessible constructors.
	 *
	 * @return whether the current class is effectively abstract
	 */
	public boolean isEffectivelyAbstract() {
		return isAbstract() || constructors.isEmpty();
	}

	public TypeReference<ClassDecl> getSuperClass() {
		return superClass;
	}

	public List<ConstructorDecl> getDeclaredConstructors() {
		return constructors;
	}

	public List<TypeReference<TypeDecl>> getPermittedTypes() {
		return permittedTypes;
	}

	@Override
	public String toString() {
		return """
			%s class %s
			  %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, constructors, fields, methods);
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		ClassDecl other = (ClassDecl) obj;
		return Objects.equals(superClass, other.superClass) &&
			Objects.equals(constructors, other.constructors) &&
			Objects.equals(permittedTypes, other.permittedTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), superClass, constructors, permittedTypes);
	}
}
