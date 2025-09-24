package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A record declaration is a special {@link ClassDecl} within an {@link LibraryTypes}.
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
		Preconditions.checkNotNull(recordComponents);
		this.recordComponents = List.copyOf(recordComponents);
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
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		RecordDecl other = (RecordDecl) obj;
		return Objects.equals(recordComponents, other.recordComponents);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), recordComponents);
	}
}
