package com.github.maracas.roseau.api.visit;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;

/**
 * Eating our own dog food with <a href="https://dx.doi.org/10.1109/MODELS.2017.23">Revisitors</a> ;)
 */
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
	T formalTypeParameter(FormalTypeParameter it);
	T enumValueDecl(EnumValueDecl it);
	T recordComponentDecl(RecordComponentDecl it);

	default T $(API it) {
		return api(it);
	}

	default T $(Symbol it) {
		return switch (it) {
			case RecordDecl r      -> recordDecl(r);
			case EnumDecl e        -> enumDecl(e);
			case ClassDecl c       -> classDecl(c);
			case InterfaceDecl i   -> interfaceDecl(i);
			case AnnotationDecl a  -> annotationDecl(a);
			case MethodDecl m      -> methodDecl(m);
			case ConstructorDecl c -> constructorDecl(c);
			case EnumValueDecl eV -> enumValueDecl(eV);
			case RecordComponentDecl rC -> recordComponentDecl(rC);
			case FieldDecl f       -> fieldDecl(f);
		};
	}

	default T $(ParameterDecl it) {
		return parameterDecl(it);
	}

	default T $(ITypeReference it) {
		return switch (it) {
			case TypeReference<?> typeRef     -> typeReference(typeRef);
			case PrimitiveTypeReference pRef  -> primitiveTypeReference(pRef);
			case ArrayTypeReference aRef      -> arrayTypeReference(aRef);
			case TypeParameterReference tpRef -> typeParameterReference(tpRef);
			case WildcardTypeReference wcRef  -> wildcardTypeReference(wcRef);
		};
	}

	default T $(Annotation it) {
		return annotation(it);
	}

	default T $(FormalTypeParameter it) { return formalTypeParameter(it); }
}
