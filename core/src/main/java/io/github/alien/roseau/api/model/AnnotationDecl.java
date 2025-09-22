package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.lang.annotation.ElementType;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * An annotation declaration (e.g., {@code public @interface Ann {}}).
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
}
