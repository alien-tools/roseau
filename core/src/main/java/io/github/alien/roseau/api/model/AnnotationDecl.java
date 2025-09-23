package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An annotation interface declaration (e.g., {@code public @interface Ann {}}).
 */
public final class AnnotationDecl extends TypeDecl {
	private final List<AnnotationMethodDecl> annotationMethods;
	private final Set<ElementType> targets;

	public AnnotationDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                      List<Annotation> annotations, SourceLocation location, List<FieldDecl> fields,
	                      List<AnnotationMethodDecl> annotationMethods, TypeReference<TypeDecl> enclosingType,
	                      Set<ElementType> targets) {
		super(qualifiedName, visibility, modifiers, annotations, location, Collections.emptyList(),
			Collections.emptyList(), fields, Collections.emptyList(), enclosingType);
		this.annotationMethods = List.copyOf(annotationMethods);
		this.targets = Collections.unmodifiableSet(
			targets.isEmpty()
				? EnumSet.noneOf(ElementType.class)
				: EnumSet.copyOf(targets));
	}

	public List<AnnotationMethodDecl> getAnnotationMethods() {
		return annotationMethods;
	}

	public Set<ElementType> getTargets() {
		return targets;
	}

	public boolean isRepeatable() {
		return annotations.stream()
			.anyMatch(ann -> Repeatable.class.getCanonicalName().equals(ann.actualAnnotation().getQualifiedName()));
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
