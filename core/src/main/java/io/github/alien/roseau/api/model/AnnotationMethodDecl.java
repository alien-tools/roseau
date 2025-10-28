package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An annotation method, possibly holding a default value expression.
 */
public final class AnnotationMethodDecl extends MethodDecl {
	/**
	 * Whether this annotation method has a default value expression.
	 */
	private final boolean hasDefault;

	public AnnotationMethodDecl(String qualifiedName, Set<Annotation> annotations, SourceLocation location,
	                            TypeReference<TypeDecl> containingType, ITypeReference type, boolean hasDefault) {
		super(qualifiedName, AccessModifier.PUBLIC, Set.of(), annotations, location, containingType,
			type, List.of(), List.of(), Set.of());
		this.hasDefault = hasDefault;
	}

	public boolean hasDefault() {
		return hasDefault;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		return obj instanceof AnnotationMethodDecl other
			&& hasDefault == other.hasDefault;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), hasDefault);
	}
}
