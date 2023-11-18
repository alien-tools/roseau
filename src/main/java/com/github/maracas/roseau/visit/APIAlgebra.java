package com.github.maracas.roseau.visit;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
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
	<U extends TypeDecl> T typeReference(TypeReference<U> it);
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

	default <U extends TypeDecl> T $(TypeReference<U> it) {
		return typeReference(it);
	}

	default T $(AccessModifier it) {
		return accessModifier(it);
	}

	default T $(Modifier it) {
		return modifier(it);
	}
}
