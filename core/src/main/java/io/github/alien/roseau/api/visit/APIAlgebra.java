package io.github.alien.roseau.api.visit;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;

/**
 * An {@link API} visitor.
 * <br>
 * Eating our own dog food with <a href="https://dx.doi.org/10.1109/MODELS.2017.23">Revisitors</a> ;)
 *
 * @param <T> the lambda type returned by each visit method
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
