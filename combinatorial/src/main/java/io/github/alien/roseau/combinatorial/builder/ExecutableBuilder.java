package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.reference.ITypeReference;

import java.util.ArrayList;
import java.util.List;

public abstract sealed class ExecutableBuilder extends TypeMemberBuilder permits ConstructorBuilder, MethodBuilder {
	public List<ParameterBuilder> parameters = new ArrayList<>();
	public List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	public List<ITypeReference> thrownExceptions = new ArrayList<>();

	protected void mutateWithDecl(ExecutableDecl decl) {
		super.mutateWithDecl(decl);

		parameters = new ArrayList<>(decl.getParameters().stream().map(ParameterBuilder::from).toList());
		formalTypeParameters = new ArrayList<>(decl.getFormalTypeParameters());
		thrownExceptions = new ArrayList<>(decl.getThrownExceptions());
	}
}
