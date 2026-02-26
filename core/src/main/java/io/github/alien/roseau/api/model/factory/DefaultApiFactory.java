package io.github.alien.roseau.api.model.factory;

import com.google.common.base.Preconditions;
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

public final class DefaultApiFactory implements ApiFactory {
	private final TypeReferenceFactory references;

	public DefaultApiFactory(TypeReferenceFactory references) {
		this.references = Preconditions.checkNotNull(references);
	}

	@Override
	public TypeReferenceFactory references() {
		return references;
	}

	@Override
	public SourceLocation location(Path file, int line) {
		return new SourceLocation(file, line);
	}

	@Override
	public SourceLocation unknownLocation() {
		return SourceLocation.NO_LOCATION;
	}

	@Override
	public ClassDecl createClass(String qualifiedName,
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
	                             Set<TypeReference<TypeDecl>> permittedTypes) {
		return new ClassDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
			formalTypeParameters, fields, methods, enclosingType, superClass, constructors, permittedTypes);
	}

	@Override
	public InterfaceDecl createInterface(String qualifiedName,
	                                     AccessModifier visibility,
	                                     Set<Modifier> modifiers,
	                                     Set<Annotation> annotations,
	                                     SourceLocation location,
	                                     Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                                     List<FormalTypeParameter> formalTypeParameters,
	                                     Set<FieldDecl> fields,
	                                     Set<MethodDecl> methods,
	                                     TypeReference<TypeDecl> enclosingType,
	                                     Set<TypeReference<TypeDecl>> permittedTypes) {
		return new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
			formalTypeParameters, fields, methods, enclosingType, permittedTypes);
	}

	@Override
	public EnumDecl createEnum(String qualifiedName,
	                           AccessModifier visibility,
	                           Set<Modifier> modifiers,
	                           Set<Annotation> annotations,
	                           SourceLocation location,
	                           Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                           Set<FieldDecl> fields,
	                           Set<MethodDecl> methods,
	                           TypeReference<TypeDecl> enclosingType,
	                           Set<ConstructorDecl> constructors) {
		// We only store enum values as fields for now
		return new EnumDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
			fields, methods, enclosingType, constructors, Set.of());
	}

	@Override
	public RecordDecl createRecord(String qualifiedName,
	                               AccessModifier visibility,
	                               Set<Modifier> modifiers,
	                               Set<Annotation> annotations,
	                               SourceLocation location,
	                               Set<TypeReference<InterfaceDecl>> implementedInterfaces,
	                               List<FormalTypeParameter> formalTypeParameters,
	                               Set<FieldDecl> fields,
	                               Set<MethodDecl> methods,
	                               TypeReference<TypeDecl> enclosingType,
	                               Set<ConstructorDecl> constructors) {
		// We only store record components as fields for now
		return new RecordDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
			formalTypeParameters, fields, methods, enclosingType, constructors, List.of());
	}

	@Override
	public AnnotationDecl createAnnotation(String qualifiedName,
	                                       AccessModifier visibility,
	                                       Set<Modifier> modifiers,
	                                       Set<Annotation> annotations,
	                                       SourceLocation location,
	                                       Set<FieldDecl> fields,
	                                       Set<AnnotationMethodDecl> annotationMethods,
	                                       TypeReference<TypeDecl> enclosingType,
	                                       Set<ElementType> targets) {
		return new AnnotationDecl(qualifiedName, visibility, modifiers, annotations, location, fields,
			annotationMethods, enclosingType, targets);
	}

	@Override
	public FieldDecl createField(String qualifiedName,
	                             AccessModifier visibility,
	                             Set<Modifier> modifiers,
	                             Set<Annotation> annotations,
	                             SourceLocation location,
	                             TypeReference<TypeDecl> containingType,
	                             ITypeReference type,
	                             boolean compileTimeConstant) {
		return new FieldDecl(qualifiedName, visibility, modifiers, annotations, location, containingType, type, compileTimeConstant);
	}

	@Override
	public MethodDecl createMethod(String qualifiedName,
	                               AccessModifier visibility,
	                               Set<Modifier> modifiers,
	                               Set<Annotation> annotations,
	                               SourceLocation location,
	                               TypeReference<TypeDecl> containingType,
	                               ITypeReference returnType,
	                               List<ParameterDecl> parameters,
	                               List<FormalTypeParameter> formalTypeParameters,
	                               Set<ITypeReference> thrownExceptions) {
		return new MethodDecl(qualifiedName, visibility, modifiers, annotations, location, containingType, returnType,
			parameters, formalTypeParameters, thrownExceptions);
	}

	@Override
	public AnnotationMethodDecl createAnnotationMethod(String qualifiedName,
	                                                   Set<Annotation> annotations,
	                                                   SourceLocation location,
	                                                   TypeReference<TypeDecl> containingType,
	                                                   ITypeReference returnType,
	                                                   boolean hasDefaultValue) {
		return new AnnotationMethodDecl(qualifiedName, annotations, location, containingType, returnType, hasDefaultValue);
	}

	@Override
	public ConstructorDecl createConstructor(String qualifiedName,
	                                         AccessModifier visibility,
	                                         Set<Modifier> modifiers,
	                                         Set<Annotation> annotations,
	                                         SourceLocation location,
	                                         TypeReference<TypeDecl> containingType,
	                                         ITypeReference type,
	                                         List<ParameterDecl> parameters,
	                                         List<FormalTypeParameter> formalTypeParameters,
	                                         Set<ITypeReference> thrownExceptions) {
		return new ConstructorDecl(qualifiedName, visibility, modifiers, annotations, location, containingType, type,
			parameters, formalTypeParameters, thrownExceptions);
	}

	@Override
	public ModuleDecl createModule(String qualifiedName, Set<String> exports) {
		return new ModuleDecl(qualifiedName, exports);
	}

	@Override
	public FormalTypeParameter createFormalTypeParameter(String name, List<ITypeReference> bounds) {
		return new FormalTypeParameter(name, bounds);
	}

	@Override
	public ParameterDecl createParameter(String name, ITypeReference type, boolean isVarargs) {
		return new ParameterDecl(name, type, isVarargs);
	}

	@Override
	public Annotation createAnnotation(TypeReference<AnnotationDecl> actualAnnotation, Map<String, String> values) {
		return new Annotation(actualAnnotation, values);
	}
}
