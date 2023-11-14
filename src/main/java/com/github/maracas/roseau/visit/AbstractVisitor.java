package com.github.maracas.roseau.visit;

import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.AnnotationDecl;
import com.github.maracas.roseau.model.ClassDecl;
import com.github.maracas.roseau.model.ConstructorDecl;
import com.github.maracas.roseau.model.EnumDecl;
import com.github.maracas.roseau.model.ExecutableDecl;
import com.github.maracas.roseau.model.FieldDecl;
import com.github.maracas.roseau.model.FormalTypeParameter;
import com.github.maracas.roseau.model.InterfaceDecl;
import com.github.maracas.roseau.model.MethodDecl;
import com.github.maracas.roseau.model.Modifier;
import com.github.maracas.roseau.model.ParameterDecl;
import com.github.maracas.roseau.model.RecordDecl;
import com.github.maracas.roseau.model.Symbol;
import com.github.maracas.roseau.model.TypeDecl;
import com.github.maracas.roseau.model.TypeReference;

public class AbstractVisitor implements APIAlgebra<Visit> {
	public Visit api(API it) {
		return () -> it.types().forEach(t -> $(t).visit());
	}

	@Override
	public Visit classDecl(ClassDecl it) {
		return () -> {
			typeDecl(it).visit();
			if (it.getSuperClass() != null)
				$(it.getSuperClass()).visit();
			it.getConstructors().forEach(c -> $(c).visit());
		};
	}

	@Override
	public Visit interfaceDecl(InterfaceDecl it) {
		return typeDecl(it);
	}

	@Override
	public Visit enumDecl(EnumDecl it) {
		return classDecl(it);
	}

	@Override
	public Visit annotationDecl(AnnotationDecl it) {
		return typeDecl(it);
	}

	@Override
	public Visit recordDecl(RecordDecl it) {
		return classDecl(it);
	}

	@Override
	public Visit methodDecl(MethodDecl it) {
		return executableDecl(it);
	}

	@Override
	public Visit constructorDecl(ConstructorDecl it) {
		return executableDecl(it);
	}

	@Override
	public Visit fieldDecl(FieldDecl it) {
		return () -> {
			symbol(it).visit();
			if (it.getType() != null)
				$(it.getType()).visit();
		};
	}

	@Override
	public Visit parameterDecl(ParameterDecl it) {
		return () -> {
			if (it.type() != null)
				$(it.type()).visit();
		};
	}

	@Override
	public Visit formalTypeParameter(FormalTypeParameter it) {
		return () -> it.bounds().forEach(b -> $(b).visit());
	}

	@Override
	public Visit typeReference(TypeReference it) {
		return Visit.NOP;
	}

	@Override
	public Visit accessModifier(AccessModifier it) {
		return Visit.NOP;
	}

	@Override
	public Visit modifier(Modifier it) {
		return Visit.NOP;
	}

	public Visit symbol(Symbol it) {
		return () -> {
			$(it.getVisibility()).visit();
			it.getModifiers().forEach(m -> $(m).visit());
			if (it.getContainingType() != null)
				$(it.getContainingType()).visit();
		};
	}

	public Visit typeDecl(TypeDecl it) {
		return () -> {
			symbol(it).visit();
			it.getSuperInterfaces().forEach(i -> $(i).visit());
			it.getFormalTypeParameters().forEach(p -> $(p).visit());
			it.getFields().forEach(f -> $(f).visit());
			it.getMethods().forEach(m -> $(m).visit());
		};
	}

	public Visit executableDecl(ExecutableDecl it) {
		return () -> {
			symbol(it).visit();
			if (it.getReturnType() != null)
				$(it.getReturnType()).visit();
			it.getParameters().forEach(p -> $(p).visit());
			it.getFormalTypeParameters().forEach(p -> $(p).visit());
			it.getThrownExceptions().forEach(e -> $(e).visit());
		};
	}
}
