package com.github.maracas.roseau.api.visit;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.Annotation;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.ExecutableDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

public class AbstractAPIVisitor implements APIAlgebra<Visit> {
	public Visit api(API it) {
		return () -> it.getAllTypes().forEach(t -> $(t).visit());
	}

	@Override
	public Visit classDecl(ClassDecl it) {
		return () -> {
			typeDecl(it).visit();
			it.getSuperClass().ifPresent(sup -> $(sup).visit());
			it.getConstructors().forEach(cons -> $(cons).visit());
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
	public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
		return () -> {};
	}

	@Override
	public Visit primitiveTypeReference(PrimitiveTypeReference it) {
		return () -> {};
	}

	@Override
	public Visit arrayTypeReference(ArrayTypeReference it) {
		return () -> {};
	}

	@Override
	public Visit typeParameterReference(TypeParameterReference it) {
		return () -> {};
	}

	public Visit symbol(Symbol it) {
		return () -> {
			it.getAnnotations().forEach(ann -> $(ann).visit());
		};
	}

	public Visit typeDecl(TypeDecl it) {
		return () -> {
			symbol(it).visit();
			it.getImplementedInterfaces().forEach(intf -> $(intf).visit());
			it.getFields().forEach(field -> $(field).visit());
			it.getMethods().forEach(meth -> $(meth).visit());
		};
	}

	public Visit executableDecl(ExecutableDecl it) {
		return () -> {
			symbol(it).visit();
			if (it.getType() != null)
				$(it.getType()).visit();
			it.getParameters().forEach(p -> $(p).visit());
			it.getThrownExceptions().forEach(e -> $(e).visit());
		};
	}

	public Visit annotation(Annotation it) {
		return () -> {
			$(it.getActualAnnotation()).visit();
		};
	}
}
