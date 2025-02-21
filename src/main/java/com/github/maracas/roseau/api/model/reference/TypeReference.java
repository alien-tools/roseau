package com.github.maracas.roseau.api.model.reference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.TypeDecl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TypeReference<T extends TypeDecl> implements ITypeReference {
	private final String qualifiedName;
	private final List<ITypeReference> typeArguments;
	@JsonIgnore
	private ReflectiveTypeFactory factory;
	@JsonIgnore
	private boolean resolutionAttempted;
	// Would intuitively make sense as WeakReference but:
	//   - There should not be any TypeReference outside the API, so they're gc'd together
	//   - These are the only references towards types outside the API, which would get randomly gc'd
	@JsonIgnore
	private T resolvedApiType;

	public static final TypeReference<ClassDecl> OBJECT = new TypeReference<>("java.lang.Object");
	public static final TypeReference<ClassDecl> RECORD = new TypeReference<>("java.lang.Record");
	public static final TypeReference<ClassDecl> ENUM = new TypeReference<>("java.lang.Enum");
	public static final TypeReference<ClassDecl> EXCEPTION = new TypeReference<>("java.lang.Exception");
	public static final TypeReference<ClassDecl> RUNTIME_EXCEPTION = new TypeReference<>("java.lang.RuntimeException");

	private static final Logger LOGGER = LogManager.getLogger(TypeReference.class);

	@JsonCreator
	TypeReference(String qualifiedName, List<ITypeReference> typeArguments) {
		this.qualifiedName = Objects.requireNonNull(qualifiedName);
		this.typeArguments = Objects.requireNonNull(typeArguments);
		this.resolutionAttempted = false;
		this.resolvedApiType = null;
	}

	TypeReference(String qualifiedName, List<ITypeReference> typeArguments, ReflectiveTypeFactory factory) {
		this(qualifiedName, typeArguments);
		this.factory = Objects.requireNonNull(factory);
	}

	private TypeReference(String qualifiedName) {
		this(qualifiedName, Collections.emptyList(), new ReflectiveTypeFactory(new CachedTypeReferenceFactory()));
	}

	public void setFactory(ReflectiveTypeFactory factory) {
		this.factory = factory;
	}

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	public List<ITypeReference> getTypeArguments() {
		return Collections.unmodifiableList(typeArguments);
	}

	/**
	 * Returns the {@link TypeDecl} pointed by this reference, constructed on-the-fly if necessary,
	 * or {@link Optional<T>.empty()} if it cannot be resolved.
	 */
	public Optional<T> getResolvedApiType() {
		if (resolutionAttempted) {
			return Optional.ofNullable(resolvedApiType);
		}

		// Safe as long as we don't have two types of different kinds (eg. class vs interface) with same FQN
		resolve((T) factory.convertCtType(qualifiedName));

		if (resolvedApiType == null) {
			LOGGER.warn("Warning: {} couldn't be resolved, results may be inaccurate", qualifiedName);
		}

		return Optional.ofNullable(resolvedApiType);
	}

	public void resolve(T type) {
		resolvedApiType = type;
		resolutionAttempted = true;
	}

	@Override
	public boolean isSubtypeOf(ITypeReference other) {
		// Always a subtype of Object
		if (other.equals(OBJECT)) {
			return true;
		}

		return switch (other) {
			// FIXME: what if upper() or !upper()?
			case WildcardTypeReference wtr -> wtr.bounds().stream().allMatch(this::isSubtypeOf);
			case TypeReference<?> tr -> Stream.concat(Stream.of(this), getAllSuperTypes())
				.anyMatch(sup -> Objects.equals(sup.qualifiedName, tr.qualifiedName) && hasCompatibleTypeParameters(tr));
			default -> false;
		};
	}

	private boolean hasCompatibleTypeParameters(TypeReference<?> other) {
		if (typeArguments.size() != other.typeArguments.size()) {
			return false;
		}

		return IntStream.range(0, typeArguments.size())
			.allMatch(i -> typeArguments.get(i).isSubtypeOf(other.typeArguments.get(i)));
	}

	public boolean isSameHierarchy(TypeReference<T> other) {
		return isSubtypeOf(other) || other.isSubtypeOf(this);
	}

	public boolean isExported() {
		return getResolvedApiType().map(TypeDecl::isExported).orElse(false);
	}

	public boolean isEffectivelyFinal() {
		return getResolvedApiType().map(TypeDecl::isEffectivelyFinal).orElse(false);
	}

	public Stream<TypeReference<? extends TypeDecl>> getAllSuperTypes() {
		return getResolvedApiType().map(TypeDecl::getAllSuperTypes).orElseGet(Stream::empty);
	}

	public static <T extends TypeDecl> List<TypeReference<T>> deepCopy(List<TypeReference<T>> refs) {
		return refs.stream()
			.map(TypeReference::deepCopy)
			.toList();
	}

	@Override
	public String toString() {
		if (typeArguments.isEmpty()) {
			return qualifiedName;
		}
		return "%s<%s>".formatted(qualifiedName,
			typeArguments.stream().map(ITypeReference::toString).collect(Collectors.joining(",")));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TypeReference<?> other = (TypeReference<?>) o;
		return Objects.equals(qualifiedName, other.qualifiedName) && Objects.equals(typeArguments, other.typeArguments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifiedName, typeArguments);
	}

	@Override
	public TypeReference<T> deepCopy() {
		return new TypeReference<>(qualifiedName, ITypeReference.deepCopy(typeArguments));
	}
}
