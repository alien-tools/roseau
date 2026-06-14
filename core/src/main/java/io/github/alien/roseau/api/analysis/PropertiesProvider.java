package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface PropertiesProvider {
	// Dependencies
	LibraryTypes libraryTypes();

	TypeResolver resolver();

	SubtypingProvider subtyping();

	TypeParameterProvider typeParameter();

	default boolean isExported(Symbol symbol) {
		return switch (symbol) {
			case TypeDecl type -> isExported(type);
			case TypeMemberDecl member -> isExported(member);
		};
	}

	/**
	 * Checks whether the type pointed by this reference is exported.
	 *
	 * @param reference the reference to check
	 * @return true if the referenced type is exported
	 */
	default boolean isExported(TypeReference<?> reference) {
		Preconditions.checkNotNull(reference);
		return resolver().resolve(reference)
			.map(this::isExported)
			.orElse(true);
	}

	/**
	 * Checks whether this type is exported.
	 *
	 * @param type the type to check
	 * @return true if this type is exported
	 */
	default boolean isExported(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return isExported(type, new HashSet<>());
	}

	private boolean isExported(TypeDecl type, Set<String> inProgress) {
		if (!libraryTypes().getModule().isExporting(type.getPackageName())) {
			return false;
		}
		Optional<TypeDecl> enclosing = type.getEnclosingType().flatMap(resolver()::resolve);
		boolean enclosingExported = enclosing.map(enc -> isExported(enc, inProgress)).orElse(true);
		boolean enclosingSubtypable = enclosing.map(enc -> canBeSubtyped(enc, new HashSet<>(inProgress))).orElse(true);
		return enclosingExported && (type.isPublic() || (type.isProtected() && enclosingSubtypable));
	}

	/**
	 * Checks whether this field or executable is exported by its containing type.
	 *
	 * @param member the field or executable to check
	 * @return true if this type member is exported
	 */
	default boolean isExported(TypeMemberDecl member) {
		Preconditions.checkNotNull(member);
		return resolver().resolve(member.getContainingType())
			.map(type -> isExported(type, member))
			.orElse(true);
	}

	/**
	 * Checks whether this field or executable is exported in the context of this type.
	 *
	 * @param type   the containing type
	 * @param member the field or executable to check
	 * @return true if this type member is exported
	 */
	default boolean isExported(TypeDecl type, TypeMemberDecl member) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(member);
		if (member instanceof ConstructorDecl constructor) {
			return isExported(type, constructor);
		}
		return isExported(type) &&
			(member.isPublic() || (member.isProtected() && canBeSubtyped(type)));
	}

	/**
	 * Checks whether this constructor is exported in the context of this class. Protected constructors are exported only
	 * when clients can directly subtype the declaring class.
	 *
	 * @param type        the containing type
	 * @param constructor the constructor to check
	 * @return true if this constructor is exported
	 */
	default boolean isExported(TypeDecl type, ConstructorDecl constructor) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(constructor);
		return isExported(type) &&
			(constructor.isPublic() || (constructor.isProtected() && canBeDirectlySubtyped(type)));
	}

	/**
	 * Checks whether the type pointed by this reference is effectively final.
	 *
	 * @param reference the reference to check
	 * @return true if the referenced type is effectively final
	 */
	default boolean isEffectivelyFinal(TypeReference<?> reference) {
		Preconditions.checkNotNull(reference);
		return resolver().resolve(reference)
			.map(this::isEffectivelyFinal)
			.orElse(false);
	}

	/**
	 * Checks whether this type is effectively final. A type is effectively final if it cannot be extended in subtypes,
	 * either because it is explicitly declared {@code final}, because every sealed or already-declared subtype path is
	 * closed to clients, or (in the case of a class) because it has no subclass-accessible constructor and no exported
	 * extensible subtype.
	 *
	 * @param type the type to check
	 * @return whether this type is effectively final
	 */
	default boolean isEffectivelyFinal(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return !canBeSubtyped(type);
	}

	/**
	 * Checks whether a client can directly extend or implement {@code type}.
	 *
	 * @param type the type to check
	 * @return whether a client can directly subtype this type
	 */
	default boolean canBeDirectlySubtyped(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return canBeDirectlySubtyped(type, new HashSet<>());
	}

	/**
	 * Checks whether a client can indirectly subtype {@code type} through a known subtype.
	 *
	 * @param type the type to check
	 * @return whether this type has an externally subtypable known subtype path
	 */
	default boolean canBeIndirectlySubtyped(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return canBeIndirectlySubtyped(type, new HashSet<>());
	}

	/**
	 * Checks whether a client can directly or indirectly subtype {@code type}.
	 *
	 * @param type the type to check
	 * @return whether this type has any externally subtypable path
	 */
	default boolean canBeSubtyped(TypeDecl type) {
		Preconditions.checkNotNull(type);
		return canBeSubtyped(type, new HashSet<>());
	}

	/**
	 * Returns the directly declared subtypes of {@code type} known in this library snapshot.
	 *
	 * @param type the type whose known subtypes should be returned
	 * @return directly declared known subtypes
	 */
	default Set<TypeDecl> getDirectKnownSubtypes(TypeDecl type) {
		Preconditions.checkNotNull(type);
		String qualifiedName = type.getQualifiedName();
		return libraryTypes().getAllTypes().stream()
			.filter(candidate -> !candidate.equals(type))
			.filter(candidate -> directSuperTypeNames(candidate).anyMatch(qualifiedName::equals))
			.collect(Collectors.toUnmodifiableSet());
	}

	private boolean canBeSubtyped(TypeDecl type, Set<String> inProgress) {
		if (!inProgress.add(type.getQualifiedName())) {
			return false;
		}
		return canBeDirectlySubtyped(type, inProgress) || canBeIndirectlySubtyped(type, inProgress);
	}

	private boolean canBeIndirectlySubtyped(TypeDecl type, Set<String> inProgress) {
		return getDirectKnownSubtypes(type).stream()
			.anyMatch(candidate -> canBeSubtyped(candidate, new HashSet<>(inProgress)));
	}

	private boolean canBeDirectlySubtyped(TypeDecl type, Set<String> inProgress) {
		return !type.isFinal() && !isStrictlySealed(type) && isExported(type, inProgress) &&
			hasSubclassAccessibleConstructor(type);
	}

	private boolean hasSubclassAccessibleConstructor(TypeDecl type) {
		return !(type instanceof ClassDecl cls) || !cls.getDeclaredConstructors().isEmpty();
	}

	private boolean isStrictlySealed(TypeDecl type) {
		return type.isSealed() && !type.isNonSealed();
	}

	static Stream<String> directSuperTypeNames(TypeDecl type) {
		Stream<String> implementedInterfaces = type.getImplementedInterfaces().stream()
			.map(TypeReference::getQualifiedName);
		if (type instanceof ClassDecl cls) {
			return Stream.concat(Stream.of(cls.getSuperClass().getQualifiedName()), implementedInterfaces);
		}
		return implementedInterfaces;
	}

	/**
	 * Checks whether this executable is effectively final in the context of this type. An executable is effectively final
	 * if it is a constructor, {@code final}, or if it is declared in a type that is itself effectively final.
	 *
	 * @param type       the containing type
	 * @param executable the method to check
	 * @return true if this method is effectively final
	 */
	default boolean isEffectivelyFinal(TypeDecl type, ExecutableDecl executable) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(executable);
		return executable.isFinal() || executable instanceof ConstructorDecl || isEffectivelyFinal(type);
	}

	/**
	 * Checks whether this executable is effectively final in the context of its containing type. An executable is
	 * effectively final if it is a constructor, {@code final}, or if it is declared in a type that is itself effectively
	 * final.
	 *
	 * @param executable the method to check
	 * @return true if this method is effectively final
	 */
	default boolean isEffectivelyFinal(ExecutableDecl executable) {
		Preconditions.checkNotNull(executable);
		return executable.isFinal() || executable.isConstructor() || isEffectivelyFinal(executable.getContainingType());
	}

	/**
	 * Returns the subset of this executable's thrown exceptions that are checked exceptions.
	 *
	 * @param executable the executable to check
	 * @return the thrown checked exceptions
	 */
	default Set<ITypeReference> getThrownCheckedExceptions(ExecutableDecl executable) {
		Preconditions.checkNotNull(executable);
		return executable.getThrownExceptions().stream()
			.map(exc -> typeParameter().resolveBound(executable, exc))
			.filter(exc -> subtyping().isCheckedException(exc))
			.collect(Collectors.toUnmodifiableSet());
	}
}
