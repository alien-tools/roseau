package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides hierarchy navigation utilities (supertypes, inherited members, overriding/shadowing).
 */
public interface HierarchyProvider {
	// Dependencies
	TypeResolver resolver();

	ErasureProvider erasure();

	SubtypingProvider subtyping();

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
		return Optional.ofNullable(getExportedFieldsByName(type).get(name));
	}

	/**
	 * Finds a {@link MethodDecl} by erasure, declared (or inherited) by this type.
	 *
	 * @param typeDecl the type to search in
	 * @param erasure  the erasure of the method to find
	 * @return an {@link Optional} indicating whether the matching method was found
	 * @see ErasureProvider#getErasure(ExecutableDecl)
	 */
	default Optional<MethodDecl> findMethod(TypeDecl typeDecl, String erasure) {
		Preconditions.checkNotNull(typeDecl);
		Preconditions.checkNotNull(erasure);
		return Optional.ofNullable(getExportedMethodsByErasure(typeDecl).get(erasure));
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
			if (resolver().resolve(method.getContainingType())
				.map(scope -> subtyping().isSubtypeOf(scope, method.getContainingType(), other.getContainingType()))
				.orElse(false)) {
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

	/**
	 * Returns the direct supertypes of {@code type} (superclass when present, plus implemented interfaces).
	 *
	 * @param type the base type
	 * @return the direct supertypes of {@code type}
	 */
	default List<TypeReference<TypeDecl>> getSuperTypes(TypeDecl type) {
		return Stream.concat(
				// Interfaces technically do not extend java.lang.Object but the compiler still assumes they do
				// since there will be a concrete java.lang.Object on which the methods are invoked anyway
				type instanceof ClassDecl cls ? Stream.of(cls.getSuperClass()) : Stream.of(TypeReference.OBJECT),
				type.getImplementedInterfaces().stream()
			)
			.map(ref -> (TypeReference<TypeDecl>) ref)
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
	 * Returns all supertypes of the given reference with generic arguments instantiated through the hierarchy.
	 * For instance, {@code ArrayList<String> -> List<String> -> Collection<String>}, etc.
	 */
	default Set<TypeReference<TypeDecl>> getAllInstantiatedSuperTypes(TypeReference<?> reference) {
		Preconditions.checkNotNull(reference);
		Set<TypeReference<TypeDecl>> result = new LinkedHashSet<>();
		collectInstantiatedSuperTypes(reference, result);
		return result;
	}

	private void collectInstantiatedSuperTypes(TypeReference<?> reference, Set<TypeReference<TypeDecl>> accumulator) {
		Optional<TypeDecl> resolved = resolver().resolve(reference);
		if (resolved.isEmpty()) {
			accumulator.addAll(getAllSuperTypes(reference));
			return;
		}

		TypeDecl typeDecl = resolved.get();
		Map<String, ITypeReference> substitutions = new HashMap<>();
		for (int i = 0; i < Math.min(typeDecl.getFormalTypeParameters().size(), reference.typeArguments().size()); i++) {
			substitutions.put(typeDecl.getFormalTypeParameters().get(i).name(), reference.typeArguments().get(i));
		}

		for (TypeReference<TypeDecl> superType : getSuperTypes(typeDecl)) {
			TypeReference<TypeDecl> instantiated = substituteTypeReference(superType, substitutions);
			if (accumulator.add(instantiated)) {
				collectInstantiatedSuperTypes(instantiated, accumulator);
			}
		}
	}

	private static TypeReference<TypeDecl> substituteTypeReference(TypeReference<TypeDecl> reference,
	                                                               Map<String, ITypeReference> substitutions) {
		return new TypeReference<>(reference.getQualifiedName(),
			reference.typeArguments().stream().map(arg -> substituteType(arg, substitutions)).toList());
	}

	private static ITypeReference substituteType(ITypeReference reference, Map<String, ITypeReference> substitutions) {
		return switch (reference) {
			case TypeParameterReference tp -> substitutions.getOrDefault(tp.name(), tp);
			case ArrayTypeReference(var componentType, var dimension) ->
				new ArrayTypeReference(substituteType(componentType, substitutions), dimension);
			case WildcardTypeReference(var bounds, var upper) ->
				new WildcardTypeReference(bounds.stream().map(bound -> substituteType(bound, substitutions)).toList(), upper);
			case TypeReference<?> tr -> new TypeReference<>(tr.getQualifiedName(),
				tr.typeArguments().stream().map(arg -> substituteType(arg, substitutions)).toList());
			default -> reference;
		};
	}

	/**
	 * Returns all methods that can be invoked on this type, including those declared in its super types. For each unique
	 * method erasure, returns the most concrete implementation, indexed by erasure.
	 *
	 * @param type the base type
	 * @return a map from method erasure to the most concrete implementation of each {@link MethodDecl} that can be
	 * invoked on this type
	 */
	default Map<String, MethodDecl> getExportedMethodsByErasure(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return Stream.concat(
				type.getDeclaredMethods().stream(),
				getAllSuperTypes(type).stream()
					.map(resolver()::resolve)
					.flatMap(t -> t.map(TypeDecl::getDeclaredMethods).orElseGet(Set::of).stream()))
			.filter(m -> properties().isExported(type, m))
			.collect(Collectors.toMap(
				erasure()::getErasure,
				Function.identity(),
				(m1, m2) -> isOverriding(m1, m2) ? m1 : m2
			));
	}

	/**
	 * Returns all methods that can be invoked on this type, including those declared in its super types. For each unique
	 * method erasure, returns the most concrete implementation.
	 *
	 * @param type the base type
	 * @return the most concrete implementation of each {@link MethodDecl} that can be invoked on this type
	 */
	default Set<MethodDecl> getExportedMethods(TypeDecl type) {
		return Set.copyOf(getExportedMethodsByErasure(type).values());
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
		}).collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Returns all fields that can be accessed on this type, including those declared in its super types. In case of
	 * shadowing, returns the visible field, indexed by simple name.
	 *
	 * @param type the base type
	 * @return a map from field simple name to each {@link FieldDecl} that can be accessed on this type
	 */
	default Map<String, FieldDecl> getExportedFieldsByName(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return Stream.concat(
				type.getDeclaredFields().stream(),
				getAllSuperTypes(type).stream()
					.map(resolver()::resolve)
					.flatMap(t -> t.map(TypeDecl::getDeclaredFields).orElseGet(Set::of).stream()))
			.filter(f -> properties().isExported(type, f))
			.collect(Collectors.toMap(
				FieldDecl::getSimpleName,
				Function.identity(),
				(f1, f2) -> isShadowing(f1, f2) ? f1 : f2
			));
	}

	/**
	 * Returns all fields that can be accessed on this type, including those declared in its super types. In case of
	 * shadowing, returns the visible field.
	 *
	 * @param type the base type
	 * @return all {@link FieldDecl} that can be accessed on this type
	 */
	default Set<FieldDecl> getExportedFields(TypeDecl type) {
		return Set.copyOf(getExportedFieldsByName(type).values());
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
			resolveSubtypeScope(reference, other)
				.map(scope -> subtyping().isSubtypeOf(scope, reference, other) ||
					subtyping().isSubtypeOf(scope, other, reference))
				.orElse(false);
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
			resolver().resolve(field.getContainingType())
				.map(scope -> subtyping().isSubtypeOf(scope, field.getContainingType(), other.getContainingType()))
				.orElse(false);
	}

	private Optional<TypeDecl> resolveSubtypeScope(TypeReference<?> first, TypeReference<?> second) {
		return resolver().resolve(first).or(() -> resolver().resolve(second));
	}
}
