package com.github.maracas.roseau.visit;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.ExecutableDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeReference;

public class AbstractAPIVisitor implements APIAlgebra<Visit> {
	public Visit api(API it) {
		return () -> it.types().forEach(t -> $(t).visit());
	}

	@Override
	public Visit classDecl(ClassDecl it) {
		return () -> {
			typeDecl(it).visit();
			if (it.getSuperClass() != null)
				class$Reference(it.getSuperClass()).visit();
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
				type$Reference(it.getType()).visit();
		};
	}

	@Override
	public Visit parameterDecl(ParameterDecl it) {
		return () -> {
			if (it.type() != null)
				type$Reference(it.type()).visit();
		};
	}

	@Override
	public Visit formalTypeParameter(FormalTypeParameter it) {
		return () -> it.bounds().forEach(b -> type$Reference(b).visit());
	}

	@Override
	public Visit typeReference(TypeReference<TypeDecl> it) {
		return Visit.NOP;
	}

	@Override
	public Visit classReference(TypeReference<ClassDecl> it) {
		return Visit.NOP;
	}

	@Override
	public Visit interfaceReference(TypeReference<InterfaceDecl> it) {
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
				type$Reference(it.getContainingType()).visit();
		};
	}

	public Visit typeDecl(TypeDecl it) {
		return () -> {
			symbol(it).visit();
			it.getSuperInterfaces().forEach(i -> interface$Reference(i).visit());
			it.getFormalTypeParameters().forEach(p -> $(p).visit());
			it.getFields().forEach(f -> $(f).visit());
			it.getMethods().forEach(m -> $(m).visit());
		};
	}

	public Visit executableDecl(ExecutableDecl it) {
		return () -> {
			symbol(it).visit();
			if (it.getReturnType() != null)
				type$Reference(it.getReturnType()).visit();
			it.getParameters().forEach(p -> $(p).visit());
			it.getFormalTypeParameters().forEach(p -> $(p).visit());
			it.getThrownExceptions().forEach(e -> class$Reference(e).visit());
		};
	}
}