package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ReflectiveTypeFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An annotation declaration in the {@link API} (e.g., {@code public @interface Ann {}}).
 */
public final class AnnotationDecl extends TypeDecl {
	public AnnotationDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                      List<Annotation> annotations, SourceLocation location, List<FieldDecl> fields,
	                      List<MethodDecl> methods, TypeReference<TypeDecl> enclosingType) {
		super(qualifiedName, visibility, modifiers, annotations, location, Collections.emptyList(),
			Collections.emptyList(), fields, methods, enclosingType);
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
			""".formatted(visibility, qualifiedName, fields, methods);
	}

	@Override
	public AnnotationDecl deepCopy() {
		return new AnnotationDecl(qualifiedName, visibility, modifiers,
			annotations.stream().map(Annotation::deepCopy).toList(), location,
			fields.stream().map(FieldDecl::deepCopy).toList(), methods.stream().map(MethodDecl::deepCopy).toList(),
			getEnclosingType().map(TypeReference::deepCopy).orElse(null));
	}

	@Override
	public AnnotationDecl deepCopy(ReflectiveTypeFactory factory) {
		return new AnnotationDecl(qualifiedName, visibility, modifiers,
			annotations.stream().map(a -> a.deepCopy(factory)).toList(), location,
			fields.stream().map(f -> f.deepCopy(factory)).toList(), methods.stream().map(m -> m.deepCopy(factory)).toList(),
			getEnclosingType().map(t -> t.deepCopy(factory)).orElse(null));
	}
}
