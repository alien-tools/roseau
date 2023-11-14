package com.github.maracas.roseau.visit;

import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.AnnotationDecl;
import com.github.maracas.roseau.model.ClassDecl;
import com.github.maracas.roseau.model.ConstructorDecl;
import com.github.maracas.roseau.model.EnumDecl;
import com.github.maracas.roseau.model.FieldDecl;
import com.github.maracas.roseau.model.FormalTypeParameter;
import com.github.maracas.roseau.model.InterfaceDecl;
import com.github.maracas.roseau.model.MethodDecl;
import com.github.maracas.roseau.model.Modifier;
import com.github.maracas.roseau.model.ParameterDecl;
import com.github.maracas.roseau.model.RecordDecl;
import com.github.maracas.roseau.model.Symbol;
import com.github.maracas.roseau.model.TypeReference;

public interface APIAlgebra<T> {
	T api(API it);
	T classDecl(ClassDecl it);
	T interfaceDecl(InterfaceDecl it);
	T enumDecl(EnumDecl it);
	T annotationDecl(AnnotationDecl it);
	T recordDecl(RecordDecl it);
	T methodDecl(MethodDecl it);
	T constructorDecl(ConstructorDecl it);
	T fieldDecl(FieldDecl it);
	T parameterDecl(ParameterDecl it);
	T formalTypeParameter(FormalTypeParameter it);
	T typeReference(TypeReference it);
	T accessModifier(AccessModifier it);
	T modifier(Modifier it);

	default T $(API it) {
		return api(it);
	}

	default T $(Symbol it) {
		return switch (it) {
			case RecordDecl r -> recordDecl(r);
			case EnumDecl e -> enumDecl(e);
			case ClassDecl c -> classDecl(c);
			case InterfaceDecl i -> interfaceDecl(i);
			case AnnotationDecl a -> annotationDecl(a);
			case MethodDecl m -> methodDecl(m);
			case ConstructorDecl c -> constructorDecl(c);
			case FieldDecl f -> fieldDecl(f);
		};
	}

	default T $(ParameterDecl it) {
		return parameterDecl(it);
	}

	default T $(FormalTypeParameter it) {
		return formalTypeParameter(it);
	}

	default T $(TypeReference it) {
		return typeReference(it);
	}

	default T $(AccessModifier it) {
		return accessModifier(it);
	}

	default T $(Modifier it) {
		return modifier(it);
	}
}
