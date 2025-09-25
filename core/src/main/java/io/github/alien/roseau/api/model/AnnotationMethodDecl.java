package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An annotation method, possibly holding a default value expression.
 */
public final class AnnotationMethodDecl extends MethodDecl {
	/**
	 * Whether this annotation method has a default value expression.
	 */
	private final boolean hasDefault;

	public AnnotationMethodDecl(String qualifiedName, List<Annotation> annotations, SourceLocation location,
	                            TypeReference<TypeDecl> containingType, ITypeReference type, boolean hasDefault) {
		super(qualifiedName, AccessModifier.PUBLIC, Collections.emptySet(), annotations, location, containingType,
			type, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
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
		AnnotationMethodDecl other = (AnnotationMethodDecl) obj;
		return hasDefault == other.hasDefault;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), hasDefault);
	}
}
