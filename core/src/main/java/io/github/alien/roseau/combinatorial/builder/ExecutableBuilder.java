package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;

import java.util.ArrayList;
import java.util.List;

abstract sealed class ExecutableBuilder extends TypeMemberBuilder permits ConstructorBuilder, MethodBuilder {
	public List<ParameterDecl> parameters = new ArrayList<>();
	public List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	public List<ITypeReference> thrownExceptions = new ArrayList<>();

	public static void mutateExecutableBuilderWithExecutable(ExecutableBuilder builder, ExecutableDecl executableDecl) {
		TypeMemberBuilder.mutateTypeMemberBuilderWithTypeMember(builder, executableDecl);

		builder.parameters = new ArrayList<>(executableDecl.getParameters());
		builder.formalTypeParameters = new ArrayList<>(executableDecl.getFormalTypeParameters());
		builder.thrownExceptions = new ArrayList<>(executableDecl.getThrownExceptions());
	}
}
