package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ReflectiveTypeFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A record declaration is a special {@link ClassDecl} within an {@link API}.
 */
public final class RecordDecl extends ClassDecl {
	private final List<RecordComponentDecl> recordComponents;

	public RecordDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                  List<Annotation> annotations, SourceLocation location,
	                  List<TypeReference<InterfaceDecl>> implementedInterfaces,
	                  List<FormalTypeParameter> formalTypeParameters, List<FieldDecl> fields, List<MethodDecl> methods,
	                  TypeReference<TypeDecl> enclosingType, List<ConstructorDecl> constructors,
	                  List<RecordComponentDecl> recordComponents) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType, TypeReference.RECORD, constructors, List.of());
		this.recordComponents = Objects.requireNonNull(recordComponents);
	}

	@Override
	public boolean isRecord() {
		return true;
	}

	public List<RecordComponentDecl> getRecordComponents() {
		return Collections.unmodifiableList(recordComponents);
	}

	@Override
	public List<FieldDecl> getDeclaredFields() {
		return super.getDeclaredFields().stream().filter(FieldDecl::isStatic).toList();
	}

	@Override
	public String toString() {
		return """
			%s record %s
			  %s
			  %s
			""".formatted(visibility, qualifiedName, fields, methods);
	}

	@Override
	public RecordDecl deepCopy() {
		return new RecordDecl(qualifiedName, visibility, modifiers, annotations.stream().map(Annotation::deepCopy).toList(),
			location, TypeReference.deepCopy(implementedInterfaces),
			formalTypeParameters.stream().map(FormalTypeParameter::deepCopy).toList(),
			fields.stream().map(FieldDecl::deepCopy).toList(), methods.stream().map(MethodDecl::deepCopy).toList(),
			getEnclosingType().map(TypeReference::deepCopy).orElse(null),
			constructors.stream().map(ConstructorDecl::deepCopy).toList(),
			recordComponents.stream().map(RecordComponentDecl::deepCopy).toList());
	}

	@Override
	public RecordDecl deepCopy(ReflectiveTypeFactory factory) {
		return new RecordDecl(qualifiedName, visibility, modifiers, annotations.stream().map(a -> a.deepCopy(factory)).toList(),
			location, TypeReference.deepCopy(implementedInterfaces, factory),
			formalTypeParameters.stream().map(fT -> fT.deepCopy(factory)).toList(),
			fields.stream().map(f -> f.deepCopy(factory)).toList(), methods.stream().map(m -> m.deepCopy(factory)).toList(),
			getEnclosingType().map(t -> t.deepCopy(factory)).orElse(null),
			constructors.stream().map(c -> c.deepCopy(factory)).toList(),
			recordComponents.stream().map(rC -> rC.deepCopy(factory)).toList());
	}
}
