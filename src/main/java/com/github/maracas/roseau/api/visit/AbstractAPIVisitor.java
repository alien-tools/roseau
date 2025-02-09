package com.github.maracas.roseau.api.visit;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.Annotation;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.ExecutableDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeMemberDecl;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;

public class AbstractAPIVisitor implements APIAlgebra<Visit> {
	public Visit api(API it) {
		return () -> it.getAllTypes().forEach(t -> $(t).visit());
	}

	@Override
	public Visit classDecl(ClassDecl it) {
		return () -> {
			typeDecl(it).visit();
			$(it.getSuperClass()).visit();
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
		return typeMemberDecl(it);
	}

	@Override
	public Visit parameterDecl(ParameterDecl it) {
		return () -> $(it.type()).visit();
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

	@Override
	public Visit wildcardTypeReference(WildcardTypeReference it) {
		return () -> {};
	}

	@Override
	public Visit annotation(Annotation it) {
		return () -> $(it.actualAnnotation()).visit();
	}

	@Override
	public Visit formalTypeParameter(FormalTypeParameter it) {
		return () -> it.bounds().forEach(b -> $(b).visit());
	}

	public Visit symbol(Symbol it) {
		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	public Visit typeDecl(TypeDecl it) {
		return () -> {
			symbol(it).visit();
			it.getFormalTypeParameters().forEach(ftp -> $(ftp).visit());
			it.getImplementedInterfaces().forEach(intf -> $(intf).visit());
			it.getDeclaredFields().forEach(field -> $(field).visit());
			it.getDeclaredMethods().forEach(meth -> $(meth).visit());
			it.getEnclosingType().ifPresent(t -> $(t).visit());
		};
	}

	public Visit typeMemberDecl(TypeMemberDecl it) {
		return () -> {
			symbol(it).visit();
			$(it.getType()).visit();
			$(it.getContainingType()).visit();
		};
	}

	public Visit executableDecl(ExecutableDecl it) {
		return () -> {
			typeMemberDecl(it).visit();
			it.getParameters().forEach(p -> $(p).visit());
			it.getThrownExceptions().forEach(e -> $(e).visit());
			it.getFormalTypeParameters().forEach(ftp -> $(ftp).visit());
		};
	}
}
