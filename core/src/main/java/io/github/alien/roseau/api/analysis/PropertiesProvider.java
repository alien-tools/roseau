package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;

public interface PropertiesProvider {
	// Dependencies
	TypeResolver resolver();
	SubtypingResolver subtyping();
	TypeParameterResolver typeParameter();

	default boolean isExported(Symbol symbol) {
		Preconditions.checkNotNull(symbol);
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
		boolean isExported = type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type));
		boolean isParentExported = type.getEnclosingType().map(this::isExported).orElse(true);

		return isExported && isParentExported;
	}

	/**
	 * Checks whether this field or executable is exported.
	 *
	 * @param typeMember the field or executable to check
	 * @return true if this type member is exported
	 */
	default boolean isExported(TypeMemberDecl typeMember) {
		Preconditions.checkNotNull(typeMember);
		return isExported(typeMember.getContainingType()) &&
			(typeMember.isPublic() || (typeMember.isProtected() && !isEffectivelyFinal(typeMember.getContainingType())));
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
	 * either because it is explicitly declared {@code final}, or {@code sealed} and not {@code non-sealed}, or (in the
	 * case of {@link ClassDecl}) because it has no subclass-accessible constructor.
	 *
	 * @param type the type to check
	 * @return whether this type is effectively final
	 */
	default boolean isEffectivelyFinal(TypeDecl type) {
		Preconditions.checkNotNull(type);

		// FIXME: in fact, a sealed class may not be final if one of its permitted subclass
		//        is explicitly marked as non-sealed...
		boolean isExplicitlyFinal = (type.isFinal() || type.isSealed()) && !type.isNonSealed();

		if (type instanceof ClassDecl cls) {
			return isExplicitlyFinal || cls.getDeclaredConstructors().isEmpty();
		}

		return isExplicitlyFinal;
	}

	/**
	 * Checks whether this method is effectively final. A method is effectively final if it is {@code final} or if it is
	 * declared in a type that is itself effectively final.
	 *
	 * @param method the method to check
	 * @return true if this method is effectively final
	 */
	default boolean isEffectivelyFinal(MethodDecl method) {
		Preconditions.checkNotNull(method);
		return method.isFinal() ||
			resolver().resolve(method.getContainingType())
				.map(this::isEffectivelyFinal)
				.orElse(false);
	}

	/**
	 * Returns the subset of this executable's thrown exceptions that are checked exceptions.
	 *
	 * @param executable the executable to check
	 * @return the thrown checked exceptions
	 */
	default List<ITypeReference> getThrownCheckedExceptions(ExecutableDecl executable) {
		Preconditions.checkNotNull(executable);
		return executable.getThrownExceptions().stream()
			.map(exc -> typeParameter().resolveBound(executable, exc))
			.filter(exc -> subtyping().isCheckedException(exc))
			.toList();
	}
}
