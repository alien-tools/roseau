package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
		// §8.10: records are implicitly final
		super(qualifiedName, visibility, Sets.union(modifiers, Set.of(Modifier.FINAL)), annotations, location,
			implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, TypeReference.RECORD, constructors,
			Set.of());
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
		return super.getDeclaredFields().stream().filter(FieldDecl::isStatic).collect(Collectors.toUnmodifiableSet());
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
		return obj instanceof RecordDecl other
			&& Objects.equals(recordComponents, other.recordComponents);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), recordComponents);
	}
}
