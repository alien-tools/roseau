package io.github.alien.roseau.api.model.factory;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;

import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ApiFactory {
	TypeReferenceFactory references();

	SourceLocation location(Path file, int line);

	SourceLocation unknownLocation();

	ClassDecl createClass(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		Set<TypeReference<InterfaceDecl>> implementedInterfaces,
		List<FormalTypeParameter> formalTypeParameters,
		Set<FieldDecl> fields,
		Set<MethodDecl> methods,
		TypeReference<TypeDecl> enclosingType,
		TypeReference<ClassDecl> superClass,
		Set<ConstructorDecl> constructors,
		Set<TypeReference<TypeDecl>> permittedTypes);

	InterfaceDecl createInterface(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		Set<TypeReference<InterfaceDecl>> implementedInterfaces,
		List<FormalTypeParameter> formalTypeParameters,
		Set<FieldDecl> fields,
		Set<MethodDecl> methods,
		TypeReference<TypeDecl> enclosingType,
		Set<TypeReference<TypeDecl>> permittedTypes);

	EnumDecl createEnum(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		Set<TypeReference<InterfaceDecl>> implementedInterfaces,
		Set<FieldDecl> fields,
		Set<MethodDecl> methods,
		TypeReference<TypeDecl> enclosingType,
		Set<ConstructorDecl> constructors);

	RecordDecl createRecord(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		Set<TypeReference<InterfaceDecl>> implementedInterfaces,
		List<FormalTypeParameter> formalTypeParameters,
		Set<FieldDecl> fields,
		Set<MethodDecl> methods,
		TypeReference<TypeDecl> enclosingType,
		Set<ConstructorDecl> constructors);

	AnnotationDecl createAnnotation(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		Set<FieldDecl> fields,
		Set<AnnotationMethodDecl> annotationMethods,
		TypeReference<TypeDecl> enclosingType,
		Set<ElementType> targets);

	FieldDecl createField(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		TypeReference<TypeDecl> containingType,
		ITypeReference type,
		boolean compileTimeConstant);

	MethodDecl createMethod(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		TypeReference<TypeDecl> containingType,
		ITypeReference returnType,
		List<ParameterDecl> parameters,
		List<FormalTypeParameter> formalTypeParameters,
		Set<ITypeReference> thrownExceptions);

	AnnotationMethodDecl createAnnotationMethod(
		String qualifiedName,
		Set<Annotation> annotations,
		SourceLocation location,
		TypeReference<TypeDecl> containingType,
		ITypeReference returnType,
		boolean hasDefaultValue);

	ConstructorDecl createConstructor(
		String qualifiedName,
		AccessModifier visibility,
		Set<Modifier> modifiers,
		Set<Annotation> annotations,
		SourceLocation location,
		TypeReference<TypeDecl> containingType,
		ITypeReference type,
		List<ParameterDecl> parameters,
		List<FormalTypeParameter> formalTypeParameters,
		Set<ITypeReference> thrownExceptions);

	ModuleDecl createModule(String qualifiedName, Set<String> exports);

	FormalTypeParameter createFormalTypeParameter(String name, List<ITypeReference> bounds);

	ParameterDecl createParameter(String name, ITypeReference type, boolean isVarargs);

	Annotation createAnnotation(
		TypeReference<AnnotationDecl> actualAnnotation,
	  Map<String, String> values);
}
