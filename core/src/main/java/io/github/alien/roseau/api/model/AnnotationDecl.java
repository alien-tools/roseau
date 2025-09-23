package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.lang.annotation.ElementType;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An annotation interface declaration (e.g., {@code public @interface Ann {}}).
 */
public final class AnnotationDecl extends TypeDecl {
	/**
	 * The methods declared in this annotation, which correspond to annotation elements, possibly with a default value
	 */
	private final List<AnnotationMethodDecl> annotationMethods;
	/**
	 * The {@link ElementType} this annotation can be used on
	 */
	private final Set<ElementType> targets;

	public AnnotationDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                      List<Annotation> annotations, SourceLocation location, List<FieldDecl> fields,
	                      List<AnnotationMethodDecl> annotationMethods, TypeReference<TypeDecl> enclosingType,
	                      Set<ElementType> targets) {
		super(qualifiedName, visibility, modifiers, annotations, location, Collections.emptyList(),
			Collections.emptyList(), fields, Collections.emptyList(), enclosingType);
		this.annotationMethods = List.copyOf(annotationMethods);
		if (hasAnnotation(TypeReference.ANNOTATION_TARGET)) {
			this.targets = Collections.unmodifiableSet(targets.isEmpty()
				// If @Target({}), the annotation cannot be placed on anything (cf. @Target's javadoc)
				? EnumSet.noneOf(ElementType.class)
				: EnumSet.copyOf(targets));
		} else {
			// §9.6.4.1: if no explicit @Target annotation, defaults to everything but TYPE_USE
			this.targets = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(ElementType.TYPE_USE)));
		}
	}

	public List<AnnotationMethodDecl> getAnnotationMethods() {
		return annotationMethods;
	}

	public Set<ElementType> getTargets() {
		return targets;
	}

	public boolean isRepeatable() {
		return hasAnnotation(TypeReference.ANNOTATION_REPEATABLE);
	}

	@Override
	public boolean isAnnotation() {
		return true;
	}

	@Override
	public String toString() {
		return """
			%s annotation %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, annotationMethods);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		AnnotationDecl that = (AnnotationDecl) o;
		return Objects.equals(annotationMethods, that.annotationMethods) &&
			Objects.equals(targets, that.targets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), annotationMethods, targets);
	}
}
