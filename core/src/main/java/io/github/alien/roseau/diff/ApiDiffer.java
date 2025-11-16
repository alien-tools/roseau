package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

public interface ApiDiffer<T> {
	T get();

	void onMatchedType(TypeDecl oldType, TypeDecl newType);

	void onRemovedType(TypeDecl type);

	void onAddedType(TypeDecl type);

	void onMatchedField(TypeDecl oldType, TypeDecl newType, FieldDecl oldField, FieldDecl newField);

	void onRemovedField(TypeDecl type, FieldDecl field);

	void onAddedField(TypeDecl type, FieldDecl field);

	void onMatchedMethod(TypeDecl oldType, TypeDecl newType, MethodDecl oldMethod, MethodDecl newMethod);

	void onRemovedMethod(TypeDecl type, MethodDecl method);

	void onAddedMethod(TypeDecl type, MethodDecl method);

	void onMatchedConstructor(ClassDecl oldCls, ClassDecl newCls, ConstructorDecl oldCons, ConstructorDecl newCons);

	void onRemovedConstructor(ClassDecl cls, ConstructorDecl cons);

	void onAddedConstructor(ClassDecl cls, ConstructorDecl cons);
}
