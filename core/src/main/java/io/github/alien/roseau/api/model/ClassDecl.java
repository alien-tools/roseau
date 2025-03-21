package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A class declaration is a {@link TypeDecl} with an optional superclass and a list of {@link ConstructorDecl}.
 * {@link ClassDecl} instantiated without superclass implicitly extend {@link java.lang.Object}.
 */
public sealed class ClassDecl extends TypeDecl permits RecordDecl, EnumDecl {
	protected final TypeReference<ClassDecl> superClass;
	protected final List<ConstructorDecl> constructors;

	public ClassDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                 List<Annotation> annotations, SourceLocation location,
	                 List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                 List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                 TypeReference<TypeDecl> enclosingType, TypeReference<ClassDecl> superClass,
	                 List<ConstructorDecl> constructors) {
		super(qualifiedName, visibility, modifiers, annotations, location,
			implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
		this.superClass = superClass != null ? superClass : TypeReference.OBJECT;
		this.constructors = Objects.requireNonNull(constructors);
	}

	@Override
	public boolean isClass() {
		return true;
	}

	@Override
	public boolean isEffectivelyFinal() {
		// A class without a subclass-accessible constructor cannot be extended
		// If the class had a default, public, or protected constructor, it would be there.
		return super.isEffectivelyFinal() || constructors.isEmpty();
	}

	@Override
	public Stream<TypeReference<? extends TypeDecl>> getAllSuperTypes() {
		return Stream.concat(
			getAllSuperClasses().flatMap(ref -> Stream.concat(Stream.of(ref), ref.getAllSuperTypes())),
			super.getAllSuperTypes()
		).distinct();
	}

	/**
	 * Returns all super classes extended (transitively) by this class, excluding the current class.
	 *
	 * @return a {@link Stream} of {@link TypeReference<ClassDecl>} to the super classes
	 */
	public Stream<TypeReference<ClassDecl>> getAllSuperClasses() {
		if (superClass.getQualifiedName().equals(getQualifiedName())) {
			// Houston, we have a problem
			return Stream.empty();
		}
		return Stream.concat(Stream.of(superClass),
			superClass.getResolvedApiType().map(ClassDecl::getAllSuperClasses).orElseGet(Stream::empty));
	}

	/**
	 * Checks whether the current class is a checked exception type. Checked exception types are subclasses of
	 * {@link java.lang.Exception} but not of {@link java.lang.RuntimeException}.
	 *
	 * @return whether the current class is a checked exception
	 */
	public boolean isCheckedException() {
		return isSubtypeOf(TypeReference.EXCEPTION) && !isUncheckedException();
	}

	/**
	 * Checks whether the current class is an unchecked exception type. Checked exception types are subclasses of
	 * {@link java.lang.RuntimeException}.
	 *
	 * @return whether the current class is an unchecked exception
	 */
	public boolean isUncheckedException() {
		return isSubtypeOf(TypeReference.RUNTIME_EXCEPTION);
	}

	/**
	 * Checks whether the current class is effectively abstract. A class is effectively abstract if it is explicitly
	 * declared abstract or if is has no subclass-accessible constructors.
	 *
	 * @return whether the current class is effectively abstract
	 */
	public boolean isEffectivelyAbstract() {
		return isAbstract() || constructors.stream().noneMatch(cons -> cons.isPublic() || cons.isProtected());
	}

	/**
	 * Finds a constructor by its erasure.
	 *
	 * @param erasure The erasure to look for
	 * @return an {@link Optional} indicating whether the constructor was found
	 */
	public Optional<ConstructorDecl> findConstructor(String erasure) {
		return getDeclaredConstructors().stream()
			.filter(cons -> Objects.equals(erasure, cons.getErasure()))
			.findFirst();
	}

	public TypeReference<ClassDecl> getSuperClass() {
		return superClass;
	}

	public List<ConstructorDecl> getDeclaredConstructors() {
		return Collections.unmodifiableList(constructors);
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ClassDecl classDecl = (ClassDecl) o;
		return Objects.equals(superClass, classDecl.superClass) && Objects.equals(constructors, classDecl.constructors);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), superClass, constructors);
	}

	@Override
	public ClassDecl deepCopy() {
		return new ClassDecl(qualifiedName, visibility, modifiers, annotations.stream().map(Annotation::deepCopy).toList(),
			location, TypeReference.deepCopy(implementedInterfaces),
			formalTypeParameters.stream().map(FormalTypeParameter::deepCopy).toList(),
			fields.stream().map(FieldDecl::deepCopy).toList(), methods.stream().map(MethodDecl::deepCopy).toList(),
			getEnclosingType().map(TypeReference::deepCopy).orElse(null), superClass.deepCopy(),
			constructors.stream().map(ConstructorDecl::deepCopy).toList());
	}
}
