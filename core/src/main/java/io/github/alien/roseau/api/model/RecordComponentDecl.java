package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public final class RecordComponentDecl extends TypeMemberDecl {
	private final boolean isVarargs;

	public RecordComponentDecl(String qualifiedName, List<Annotation> annotations, SourceLocation location,
	                           TypeReference<TypeDecl> containingType, ITypeReference type, boolean isVarargs) {
		super(qualifiedName, AccessModifier.PRIVATE, EnumSet.of(Modifier.FINAL), annotations, location, containingType, type);

		this.isVarargs = isVarargs;
	}

	public boolean isVarargs() {
		return isVarargs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		RecordComponentDecl other = (RecordComponentDecl) o;
		return isVarargs == other.isVarargs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), isVarargs);
	}

	@Override
	public String toString() {
		return "%s %s%s".formatted(type, isVarargs ? "..." : "", simpleName);
	}
}
