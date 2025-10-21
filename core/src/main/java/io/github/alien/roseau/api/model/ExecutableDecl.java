package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract executable is either a {@link MethodDecl} or a {@link ConstructorDecl}. Executables can declare a list of
 * parameters, formal type parameters, and potentially thrown exceptions.
 */
public abstract sealed class ExecutableDecl extends TypeMemberDecl permits MethodDecl, ConstructorDecl {
	protected final List<ParameterDecl> parameters;
	protected final List<FormalTypeParameter> formalTypeParameters;
	// Thrown exceptions aren't necessarily TypeReference<ClassDecl>
	// e.g.: <X extends Throwable> m() throws X
	protected final List<ITypeReference> thrownExceptions;

	protected ExecutableDecl(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers,
	                         List<Annotation> annotations, SourceLocation location,
	                         TypeReference<TypeDecl> containingType, ITypeReference type, List<ParameterDecl> parameters,
	                         List<FormalTypeParameter> formalTypeParameters,
	                         List<ITypeReference> thrownExceptions) {
		super(qualifiedName, visibility, modifiers, annotations, location, containingType, type);
		Preconditions.checkNotNull(parameters);
		Preconditions.checkNotNull(formalTypeParameters);
		Preconditions.checkNotNull(thrownExceptions);
		this.parameters = List.copyOf(parameters);
		this.formalTypeParameters = List.copyOf(formalTypeParameters);
		this.thrownExceptions = List.copyOf(thrownExceptions);
	}

	@Override
	public String getQualifiedName() {
		return String.format("%s.%s", getContainingType().getQualifiedName(), getSignature());
	}

	/**
	 * Checks whether this executable is a method.
	 *
	 * @return true if this executable is a method and not a constructor.
	 */
	public boolean isMethod() {
		return false;
	}

	/**
	 * Checks whether this executable is a constructor.
	 *
	 * @return true if this executable is a constructor and not a method.
	 */
	public boolean isConstructor() {
		return false;
	}

	/**
	 * The unqualified signature of an executable as specified in JLS §8.4.2. Types in a signature are not erased.
	 *
	 * @return the executable's signature
	 */
	public String getSignature() {
		StringBuilder sb = new StringBuilder();
		sb.append(simpleName);
		sb.append('(');
		for (int i = 0; i < parameters.size(); i++) {
			ParameterDecl p = parameters.get(i);
			sb.append(p.type());
			if (p.isVarargs()) {
				sb.append("[]");
			}
			if (i < parameters.size() - 1) {
				sb.append(',');
			}
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Checks whether this executable accepts a variable number of arguments.
	 *
	 * @return true if this executable is varargs
	 */
	public boolean isVarargs() {
		return !parameters.isEmpty() && parameters.getLast().isVarargs();
	}

	public List<ParameterDecl> getParameters() {
		return parameters;
	}

	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	public List<ITypeReference> getThrownExceptions() {
		return thrownExceptions;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		ExecutableDecl other = (ExecutableDecl) obj;
		return Objects.equals(parameters, other.parameters)
			&& Objects.equals(formalTypeParameters, other.formalTypeParameters)
			&& Objects.equals(thrownExceptions, other.thrownExceptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), parameters, formalTypeParameters, thrownExceptions);
	}
}
