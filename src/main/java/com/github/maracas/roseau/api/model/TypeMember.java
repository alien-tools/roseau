package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

/**
 * A member of a type declaration, either a {@link FieldDecl}, {@link MethodDecl} or {@link ConstructorDecl}.
 */
public interface TypeMember {
	TypeReference<TypeDecl> getContainingType();
	ITypeReference getType();
	String getSimpleName();
	boolean isStatic();
	boolean isFinal();
	boolean isPublic();
	boolean isProtected();
}
