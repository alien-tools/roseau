package io.github.alien.roseau.api.visit;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;

/**
 * A default abstract implementation of {@link APIAlgebra} using {@link Visit} as the lambda type that produces no
 * value.
 */
public abstract class AbstractAPIVisitor implements APIAlgebra<Visit> {
	@Override
	public Visit api(API it) {
		return () -> $(it.getLibraryTypes()).visit();
	}

	@Override
	public Visit libraryTypes(LibraryTypes it) {
		return () -> it.getAllTypes().forEach(t -> $(t).visit());
	}

	@Override
	public Visit classDecl(ClassDecl it) {
		return () -> {
			typeDecl(it).visit();
			$(it.getSuperClass()).visit();
			it.getPermittedTypes().forEach(t -> $(t).visit());
			it.getDeclaredConstructors().forEach(cons -> $(cons).visit());
		};
	}

	@Override
	public Visit interfaceDecl(InterfaceDecl it) {
		return () -> {
			typeDecl(it).visit();
			it.getPermittedTypes().forEach(t -> $(t).visit());
		};
	}

	@Override
	public Visit enumDecl(EnumDecl it) {
		return () -> {
			classDecl(it).visit();
			it.getValues().forEach(eV -> $(eV).visit());
		};
	}

	@Override
	public Visit annotationDecl(AnnotationDecl it) {
		return typeDecl(it);
	}

	@Override
	public Visit recordDecl(RecordDecl it) {
		return () -> {
			classDecl(it).visit();
			it.getRecordComponents().forEach(rC -> $(rC).visit());
		};
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
		return () -> it.typeArguments().forEach(ta -> $(ta).visit());
	}

	@Override
	public Visit primitiveTypeReference(PrimitiveTypeReference it) {
		return () -> {
		};
	}

	@Override
	public Visit arrayTypeReference(ArrayTypeReference it) {
		return () -> $(it.componentType()).visit();
	}

	@Override
	public Visit typeParameterReference(TypeParameterReference it) {
		return () -> {
		};
	}

	@Override
	public Visit wildcardTypeReference(WildcardTypeReference it) {
		return () -> it.bounds().forEach(b -> $(b).visit());
	}

	@Override
	public Visit annotation(Annotation it) {
		return () -> $(it.actualAnnotation()).visit();
	}

	@Override
	public Visit formalTypeParameter(FormalTypeParameter it) {
		return () -> it.bounds().forEach(b -> $(b).visit());
	}

	@Override
	public Visit enumValueDecl(EnumValueDecl it) {
		return typeMemberDecl(it);
	}

	@Override
	public Visit recordComponentDecl(RecordComponentDecl it) {
		return typeMemberDecl(it);
	}

	public Visit symbol(Symbol it) {
		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	public Visit typeDecl(TypeDecl it) {
		return () -> {
			symbol(it).visit();
			it.getImplementedInterfaces().forEach(intf -> $(intf).visit());
			it.getFormalTypeParameters().forEach(ftp -> $(ftp).visit());
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
			it.getFormalTypeParameters().forEach(ftp -> $(ftp).visit());
			it.getThrownExceptions().forEach(e -> $(e).visit());
		};
	}
}
