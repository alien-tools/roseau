package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A class declaration is a {@link TypeDecl} with an optional superclass and a list of {@link ConstructorDecl}.
 * {@link ClassDecl} instantiated without a superclass implicitly extend {@link java.lang.Object}.
 */
public sealed class ClassDecl extends TypeDecl permits RecordDecl, EnumDecl {
	protected final TypeReference<ClassDecl> superClass;
	protected final Set<ConstructorDecl> constructors;

	public ClassDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                 Set<Annotation> annotations, SourceLocation location,
	                 Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                 List<FormalTypeParameter> formalTypeParameters, Set<FieldDecl> fields, Set<MethodDecl> methods,
	                 TypeReference<TypeDecl> enclosingType, TypeReference<ClassDecl> superClass,
	                 Set<ConstructorDecl> constructors, Set<TypeReference<TypeDecl>> permittedTypes) {
		super(qualifiedName, visibility, modifiers, annotations, location,
			implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, permittedTypes);
		Preconditions.checkNotNull(constructors);
		Preconditions.checkNotNull(permittedTypes);
		this.superClass = Optional.ofNullable(superClass).orElse(TypeReference.OBJECT);
		this.constructors = Set.copyOf(constructors);
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

	public Set<ConstructorDecl> getDeclaredConstructors() {
		return constructors;
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
		return obj instanceof ClassDecl other
			&& Objects.equals(superClass, other.superClass)
			&& Objects.equals(constructors, other.constructors);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), superClass, constructors);
	}
}
