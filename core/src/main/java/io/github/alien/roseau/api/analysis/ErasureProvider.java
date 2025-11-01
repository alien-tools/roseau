package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;

import java.util.Objects;

public interface ErasureProvider {
	TypeParameterProvider typeParameter();

	/**
	 * Returns the unqualified erasure of the signature of an executable as specified in JLS §4.6. Parameter types are
	 * replaced with their erasure.
	 *
	 * @param executable the executable
	 * @return the executable's erasure
	 */
	default String getErasure(ExecutableDecl executable) {
		Preconditions.checkNotNull(executable);
		StringBuilder sb = new StringBuilder(100);
		sb.append(executable.getSimpleName());
		sb.append('(');
		for (int i = 0; i < executable.getParameters().size(); i++) {
			ParameterDecl p = executable.getParameters().get(i);
			sb.append(getErasedType(executable, p.type()).getQualifiedName());
			if (p.isVarargs()) {
				sb.append("[]");
			}
			if (i < executable.getParameters().size() - 1) {
				sb.append(',');
			}
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Checks whether two executables have the same erasure.
	 *
	 * @param e1 the first executable
	 * @param e2 the second executable
	 * @return true if both executables have the same erasure
	 */
	default boolean haveSameErasure(ExecutableDecl e1, ExecutableDecl e2) {
		Preconditions.checkNotNull(e1);
		Preconditions.checkNotNull(e2);
		return Objects.equals(getErasure(e1), getErasure(e2));
	}

	private ITypeReference getErasedType(ExecutableDecl executable, ITypeReference reference) {
		Preconditions.checkNotNull(executable);
		Preconditions.checkNotNull(reference);
		return switch (reference) {
			case TypeParameterReference tpr -> typeParameter().resolveTypeParameterBound(executable, tpr);
			case ArrayTypeReference(var t, var dimension) -> new ArrayTypeReference(getErasedType(executable, t), dimension);
			default -> reference;
		};
	}
}
