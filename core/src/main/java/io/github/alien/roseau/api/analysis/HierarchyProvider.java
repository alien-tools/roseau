package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface HierarchyProvider {
	// Dependencies
	TypeResolver resolver();

	ErasureProvider erasure();

	SubtypingResolver subtyping();

	PropertiesProvider properties();

	/**
	 * Finds a {@link FieldDecl} by simple name, declared (or inherited) by this type.
	 *
	 * @param type the base type to look into
	 * @param name the simple name of the field to find
	 * @return an {@link Optional} indicating whether the matching field was found
	 */
	default Optional<FieldDecl> findField(TypeDecl type, String name) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(name);
		return getExportedFields(type).stream()
			.filter(f -> Objects.equals(f.getSimpleName(), name))
			.findFirst();
	}

	/**
	 * Finds a {@link MethodDecl} by erasure, declared (or inherited) by this type.
	 *
	 * @param erasure the erasure of the method to find
	 * @return an {@link Optional} indicating whether the matching method was found
	 * @see ErasureProvider#getErasure(ExecutableDecl)
	 */
	default Optional<MethodDecl> findMethod(TypeDecl typeDecl, String erasure) {
		Preconditions.checkNotNull(typeDecl);
		Preconditions.checkNotNull(erasure);
		return getExportedMethods(typeDecl).stream()
			.filter(m -> Objects.equals(erasure().getErasure(m), erasure))
			.findFirst();
	}

	/**
	 * Finds a {@link ConstructorDecl} by erasure, declared by this type.
	 *
	 * @param erasure The erasure to look for
	 * @return an {@link Optional} indicating whether the matching constructor was found
	 */
	default Optional<ConstructorDecl> findConstructor(ClassDecl classDecl, String erasure) {
		Preconditions.checkNotNull(classDecl);
		Preconditions.checkNotNull(erasure);
		return classDecl.getDeclaredConstructors().stream()
			.filter(cons -> Objects.equals(erasure, erasure().getErasure(cons)))
			.findFirst();
	}

	/**
	 * Checks whether this method overrides the supplied method {@code other}. A method overrides itself.
	 *
	 * @param method the first method
	 * @param other  The other method
	 * @return true if method overrides other
	 */
	default boolean isOverriding(MethodDecl method, MethodDecl other) {
		Preconditions.checkNotNull(method);
		Preconditions.checkNotNull(other);
		if (method.equals(other)) {
			return true;
		}
		if (erasure().haveSameErasure(method, other)) {
			if (subtyping().isSubtypeOf(method.getContainingType(), other.getContainingType())) {
				return true;
			}
			if (!method.isAbstract() && other.isAbstract()) {
				return true;
			}
			return !method.isDefault() && !method.isAbstract() && other.isDefault();
		}
		return false;
	}

	/**
	 * Returns all super classes extended (transitively) by this class, excluding the current class.
	 *
	 * @param cls the base class
	 * @return the list of super classes inherited by this class
	 */
	default List<TypeReference<ClassDecl>> getAllSuperClasses(ClassDecl cls) {
		Preconditions.checkNotNull(cls);
		if (cls.getSuperClass().getQualifiedName().equals(cls.getQualifiedName())) {
			return List.of();
		}
		return Stream.concat(
			Stream.of(cls.getSuperClass()),
			resolver().resolve(cls.getSuperClass(), ClassDecl.class)
				.map(this::getAllSuperClasses)
				.orElseGet(List::of)
				.stream()
		).toList();
	}

	/**
	 * Returns all super interfaces implemented by this type, directly or indirectly, this type excluded.
	 *
	 * @param type the base type
	 * @return all interfaces implemented by this type, directly or indirectly
	 */
	default List<TypeReference<TypeDecl>> getAllImplementedInterfaces(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return getAllSuperTypes(type).stream()
			.filter(ref -> resolver().resolve(ref).map(TypeDecl::isInterface).orElse(false))
			.toList();
	}

	/**
	 * Returns all super types recursively present in the hierarchy starting from this type, excluded. In the case of
	 * {@link ClassDecl}, this includes super classes.
	 *
	 * @param type the base type
	 * @return all super types in this type's hierarchy
	 */
	@SuppressWarnings("unchecked")
	default List<TypeReference<TypeDecl>> getAllSuperTypes(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return getSuperTypes(type).stream()
			.flatMap(ref -> Stream.concat(Stream.of(ref), getAllSuperTypes(ref).stream()))
			.distinct()
			.toList();
	}

	default List<TypeReference<TypeDecl>> getSuperTypes(TypeDecl type) {
		return Stream.concat(
				// Interfaces technically do not extend java.lang.Object but the compiler still assumes they do
				// since there will be a concrete java.lang.Object on which the methods are invoked anyway
				type instanceof ClassDecl cls ? Stream.of(cls.getSuperClass()) : Stream.of(TypeReference.OBJECT),
				type.getImplementedInterfaces().stream()
			)
			.map(ref -> (TypeReference<TypeDecl>) (TypeReference<?>) ref)
			.filter(ref -> !ref.qualifiedName().equals(type.getQualifiedName()))
			.toList();
	}

	/**
	 * Returns all super types recursively present in the hierarchy starting from this type, excluded. In the case of
	 * {@link ClassDecl}, this includes super classes.
	 *
	 * @param reference reference to the base type
	 * @return all super types in this type's hierarchy
	 */
	default List<TypeReference<TypeDecl>> getAllSuperTypes(TypeReference<?> reference) {
		Preconditions.checkNotNull(reference);
		return resolver().resolve(reference)
			.map(this::getAllSuperTypes)
			.orElseGet(List::of);
	}

	/**
	 * Returns all methods that can be invoked on this type, including those declared in its super types. For each unique
	 * method erasure, returns the most concrete implementation.
	 *
	 * @param type the base type
	 * @return the most concrete implementation of each {@link MethodDecl} that can be invoked on this type
	 */
	default Set<MethodDecl> getExportedMethods(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return Stream.concat(
				type.getDeclaredMethods().stream(),
				getSuperTypes(type).stream()
					.map(resolver()::resolve)
					.flatMap(t -> t.map(this::getExportedMethods).orElseGet(Set::of).stream()))
			.filter(m -> properties().isExported(type, m))
			.collect(Collectors.toMap(
				erasure()::getErasure,
				Function.identity(),
				(m1, m2) -> isOverriding(m1, m2) ? m1 : m2
			)).values().stream().collect(ImmutableSet.toImmutableSet());
	}

	/**
	 * Returns all methods that must be implemented on this type, including those declared in its super types.
	 *
	 * @param type the base type
	 * @return each {@link MethodDecl} that must be implemented on this type
	 */
	default Set<MethodDecl> getAllMethodsToImplement(TypeDecl type) {
		return getExportedMethods(type).stream().filter(m -> {
			if (resolver().resolve(m.getContainingType()).map(TypeDecl::isInterface).orElse(false)) {
				return !m.isDefault() && !m.isStatic();
			}

			return m.isAbstract();
		}).collect(ImmutableSet.toImmutableSet());
	}

	/**
	 * Returns all fields that can be accessed on this type, including those declared in its super types. In case of
	 * shadowing, returns the visible field.
	 *
	 * @param type the base type
	 * @return all {@link FieldDecl} that can be accessed on this type
	 */
	default Set<FieldDecl> getExportedFields(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return Stream.concat(
				type.getDeclaredFields().stream(),
				getSuperTypes(type).stream()
					.map(resolver()::resolve)
					.flatMap(t -> t.map(this::getExportedFields).orElseGet(Set::of).stream()))
			.filter(f -> properties().isExported(type, f))
			.collect(Collectors.toMap(
				FieldDecl::getSimpleName,
				Function.identity(),
				(f1, f2) -> isShadowing(f1, f2) ? f1 : f2
			)).values().stream().collect(ImmutableSet.toImmutableSet());
	}

	/**
	 * Checks whether the two executables are overloading each others. Assuming that the {@link LibraryTypes} they belong
	 * to is consistent (the source compiles), there should not be two methods with the same erasure but different return
	 * types, so an executable overloads another if it has the same name but different erasure. An {@link ExecutableDecl}
	 * does not overload itself.
	 *
	 * @param executable the first executable
	 * @param other      the second executable
	 * @return whether the two executables overload each other
	 */
	default boolean isOverloading(ExecutableDecl executable, ExecutableDecl other) {
		Preconditions.checkNotNull(executable);
		Preconditions.checkNotNull(other);
		return executable.getSimpleName().equals(other.getSimpleName()) &&
			!erasure().haveSameErasure(executable, other) &&
			isSameHierarchy(executable.getContainingType(), other.getContainingType());
	}

	/**
	 * Checks whether the two types belong to the same type hierarchy.
	 *
	 * @param reference the first type
	 * @param other     the second type
	 * @return whether the two types belong to the same type hierarchy
	 */
	default boolean isSameHierarchy(TypeReference<?> reference, TypeReference<?> other) {
		return reference.equals(other) ||
			subtyping().isSubtypeOf(reference, other) ||
			subtyping().isSubtypeOf(other, reference);
	}

	/**
	 * Checks whether a field shadows another one.
	 *
	 * @param field the first field
	 * @param other the second field
	 * @return true if the first field shadows the other
	 */
	default boolean isShadowing(FieldDecl field, FieldDecl other) {
		return field.getSimpleName().equals(other.getSimpleName()) &&
			subtyping().isSubtypeOf(field.getContainingType(), other.getContainingType());
	}
}
