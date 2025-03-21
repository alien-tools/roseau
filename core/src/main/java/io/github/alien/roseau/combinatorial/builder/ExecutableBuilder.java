package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;

import java.util.ArrayList;
import java.util.List;

abstract sealed class ExecutableBuilder extends TypeMemberBuilder permits ConstructorBuilder, MethodBuilder {
	public List<ParameterDecl> parameters = new ArrayList<>();
	public List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	public List<ITypeReference> thrownExceptions = new ArrayList<>();

	public void resetParameters() {
		parameters = new ArrayList<>();
	}
}
