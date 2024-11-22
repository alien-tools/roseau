package com.github.maracas.roseau.combinatorial.client.visit;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.*;
import com.github.maracas.roseau.api.visit.APIAlgebra;

import java.util.HashMap;

public class ClientGenerator implements APIAlgebra<Generate> {
	@Override
	public Generate api(API it) {
		return () -> {
			System.out.println("In api: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate classDecl(ClassDecl it) {
		return () -> {
			System.out.println("In classDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate interfaceDecl(InterfaceDecl it) {
		return () -> {
			System.out.println("In interfaceDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate enumDecl(EnumDecl it) {
		return () -> {
			System.out.println("In enumDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate annotationDecl(AnnotationDecl it) {
		return () -> {
			System.out.println("In annotationDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate recordDecl(RecordDecl it) {
		return () -> {
			System.out.println("In recordDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate methodDecl(MethodDecl it) {
		return () -> {
			System.out.println("In methodDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate constructorDecl(ConstructorDecl it) {
		return () -> {
			System.out.println("In constructorDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate fieldDecl(FieldDecl it) {
		return () -> {
			System.out.println("In fieldDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate parameterDecl(ParameterDecl it) {
		return () -> {
			System.out.println("In parameterDecl: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public <U extends TypeDecl> Generate typeReference(TypeReference<U> it) {
		return () -> {
			System.out.println("In <: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate primitiveTypeReference(PrimitiveTypeReference it) {
		return () -> {
			System.out.println("In primitiveTypeReference: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate arrayTypeReference(ArrayTypeReference it) {
		return () -> {
			System.out.println("In arrayTypeReference: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate typeParameterReference(TypeParameterReference it) {
		return () -> {
			System.out.println("In typeParameterReference: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate wildcardTypeReference(WildcardTypeReference it) {
		return () -> {
			System.out.println("In wildcardTypeReference: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate annotation(Annotation it) {
		return () -> {
			System.out.println("In annotation: " + it.toString());
			return new HashMap<>();
		};
	}

	@Override
	public Generate formalTypeParameter(FormalTypeParameter it) {
		return () -> {
			System.out.println("In formalTypeParameter: " + it.toString());
			return new HashMap<>();
		};
	}
}
