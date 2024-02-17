package com.github.maracas.roseau.api.visit;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.Annotation;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;

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
	<U extends TypeDecl> T typeReference(TypeReference<U> it);
	T primitiveTypeReference(PrimitiveTypeReference it);
	T arrayTypeReference(ArrayTypeReference it);
	T typeParameterReference(TypeParameterReference it);
	T wildcardTypeReference(WildcardTypeReference it);
	T annotation(Annotation it);

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

	default T $(ITypeReference it) {
		return switch (it) {
			case TypeReference<?> typeRef -> typeReference(typeRef);
			case PrimitiveTypeReference primitiveRef -> primitiveTypeReference(primitiveRef);
			case ArrayTypeReference arrayRef -> arrayTypeReference(arrayRef);
			case TypeParameterReference tpRef -> typeParameterReference(tpRef);
			case WildcardTypeReference wcRef -> wildcardTypeReference(wcRef);
		};
	}

	default T $(Annotation it) {
		return annotation(it);
	}
}
