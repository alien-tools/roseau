package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

public interface ApiDiffer<T> {
	T get();

	void onMatchedType(TypeDecl type1, TypeDecl type2);

	void onRemovedType(TypeDecl type);

	void onAddedType(TypeDecl type);

	void onMatchedField(TypeDecl type1, TypeDecl type2, FieldDecl field2, FieldDecl field1);

	void onRemovedField(TypeDecl type, FieldDecl field);

	void onAddedField(TypeDecl type, FieldDecl field);

	void onMatchedMethod(TypeDecl type1, TypeDecl type2, MethodDecl method2, MethodDecl method1);

	void onRemovedMethod(TypeDecl type, MethodDecl method);

	void onAddedMethod(TypeDecl type, MethodDecl method);

	void onMatchedConstructor(ClassDecl cls1, ClassDecl cls2, ConstructorDecl cons2, ConstructorDecl cons1);

	void onRemovedConstructor(ClassDecl cls, ConstructorDecl cons);

	void onAddedConstructor(ClassDecl cls, ConstructorDecl cons);
}
