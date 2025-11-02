package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

public interface ApiDiffer {
	void onMatchedType(TypeDecl v1, TypeDecl v2);

	void onRemovedType(TypeDecl v1);

	void onAddedType(TypeDecl v2);

	void onMatchedField(TypeDecl ownerV1, FieldDecl v1, FieldDecl v2);

	void onRemovedField(TypeDecl ownerV1, FieldDecl v1);

	void onAddedField(TypeDecl ownerV2, FieldDecl v2);

	void onMatchedMethod(TypeDecl ownerV1, MethodDecl v1, MethodDecl v2);

	void onRemovedMethod(TypeDecl ownerV1, MethodDecl v1);

	void onAddedMethod(TypeDecl ownerV2, MethodDecl v2);

	void onMatchedConstructor(ClassDecl ownerV1, ConstructorDecl v1, ConstructorDecl v2);

	void onRemovedConstructor(ClassDecl ownerV1, ConstructorDecl v1);

	void onAddedConstructor(ClassDecl ownerV2, ConstructorDecl v2);
}
