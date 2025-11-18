package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;

public interface ApiDiffer<T> {
	T get();

	void onMatchedType(TypeDecl oldType, TypeDecl newType);

	void onRemovedType(TypeDecl type);

	void onAddedType(TypeDecl type);

	void onMatchedClass(ClassDecl oldCls, ClassDecl newCls);

	void onRemovedClass(ClassDecl cls);

	void onAddedClass(ClassDecl cls);

	void onMatchedEnum(EnumDecl oldEnum, EnumDecl newEnum);

	void onRemovedEnum(EnumDecl enm);

	void onAddedEnum(EnumDecl enm);

	void onMatchedRecord(RecordDecl oldRecord, RecordDecl newRecord);

	void onRemovedRecord(RecordDecl rcrd);

	void onAddedRecord(RecordDecl rcrd);

	void onMatchedInterface(InterfaceDecl oldInterface, InterfaceDecl newInterface);

	void onRemovedInterface(InterfaceDecl intf);

	void onAddedInterface(InterfaceDecl intf);

	void onMatchedAnnotation(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation);

	void onRemovedAnnotation(AnnotationDecl annotation);

	void onAddedAnnotation(AnnotationDecl annotation);

	void onMatchedField(TypeDecl oldType, TypeDecl newType, FieldDecl oldField, FieldDecl newField);

	void onRemovedField(TypeDecl type, FieldDecl field);

	void onAddedField(TypeDecl type, FieldDecl field);

	void onMatchedMethod(TypeDecl oldType, TypeDecl newType, MethodDecl oldMethod, MethodDecl newMethod);

	void onRemovedMethod(TypeDecl type, MethodDecl method);

	void onAddedMethod(TypeDecl type, MethodDecl method);

	void onMatchedConstructor(ClassDecl oldCls, ClassDecl newCls, ConstructorDecl oldCons, ConstructorDecl newCons);

	void onRemovedConstructor(ClassDecl cls, ConstructorDecl cons);

	void onAddedConstructor(ClassDecl cls, ConstructorDecl cons);

	void onMatchedAnnotationMethod(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation,
	                               AnnotationMethodDecl oldMethod, AnnotationMethodDecl newMethod);

	void onRemovedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method);

	void onAddedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method);
}
