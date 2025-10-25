package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A record declaration is a special {@link ClassDecl} within an {@link LibraryTypes}.
 */
public final class RecordDecl extends ClassDecl {
	private final List<RecordComponentDecl> recordComponents;

	public RecordDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                  Set<Annotation> annotations, SourceLocation location,
	                  Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                  List<FormalTypeParameter> formalTypeParameters, Set<FieldDecl> fields, Set<MethodDecl> methods,
	                  TypeReference<TypeDecl> enclosingType, Set<ConstructorDecl> constructors,
	                  List<RecordComponentDecl> recordComponents) {
		super(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces, formalTypeParameters,
			fields, methods, enclosingType, TypeReference.RECORD, constructors, Set.of());
		Preconditions.checkNotNull(recordComponents);
		this.recordComponents = List.copyOf(recordComponents);
	}

	@Override
	public boolean isRecord() {
		return true;
	}

	public List<RecordComponentDecl> getRecordComponents() {
		return recordComponents;
	}

	@Override
	public Set<FieldDecl> getDeclaredFields() {
		return super.getDeclaredFields().stream().filter(FieldDecl::isStatic).collect(ImmutableSet.toImmutableSet());
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
