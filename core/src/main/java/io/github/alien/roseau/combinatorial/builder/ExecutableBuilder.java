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

	public static void mutateExecutableBuilderWithExecutable(ExecutableBuilder builder, ExecutableDecl decl) {
		TypeMemberBuilder.mutateTypeMemberBuilderWithTypeMember(builder, decl);

		builder.parameters = decl.getParameters().stream().map(ParameterBuilder::from).toList();
		builder.formalTypeParameters = new ArrayList<>(decl.getFormalTypeParameters());
		builder.thrownExceptions = new ArrayList<>(decl.getThrownExceptions());
	}
}
