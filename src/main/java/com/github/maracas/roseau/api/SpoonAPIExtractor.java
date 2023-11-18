package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeReference;
import com.github.maracas.roseau.visit.Visit;
import spoon.reflect.CtModel;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents roseau's API extraction tool.
 * <br/>
 * Types are resolved within the universe of API types (exported or not).
 * We don't know anything about the outside world.
 */
public class SpoonAPIExtractor implements APIExtractor {
	private final CtModel model;

	/**
	 * Constructs an APIExtractor instance with the provided CtModel to extract its information.
	 */
	public SpoonAPIExtractor(CtModel model) {
		this.model = Objects.requireNonNull(model);
	}

	/**
	 * Extracts the library's (model's) structured API.
	 *
	 * @return Library's (model's) API.
	 */
	public API extractAPI() {
		List<CtPackage> allPackages = model.getAllPackages().stream().toList();

		Map<CtTypeReference<?>, TypeDecl> allTypes = allPackages.stream()
			.map(this::getAllTypes)
			.flatMap(Collection::stream)
			.collect(Collectors.toMap(
				t -> t,
				t -> convertSpoonTypeToTypeDeclaration(t.getTypeDeclaration())
			));

		// Within-library type resolution
		API api = new API(allTypes.values().stream().toList());
		Visit v = new TypeResolver(api).$(api);
		v.visit();

		return api;
	}

	private boolean isExported(CtType<?> type) {
		return
			   (type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type)))
			&& (type.getDeclaringType() == null || isExported(type.getDeclaringType()));
	}

	private boolean isExported(CtModifiable member) {
		return member.isPublic() || member.isProtected();
	}

	private boolean isEffectivelyFinal(CtType<?> type) {
		return type.isFinal() || type.hasModifier(ModifierKind.SEALED);
	}

	// Returns all types within a package
	private List<CtTypeReference<?>> getAllTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.flatMap(type -> Stream.concat(Stream.of(type.getReference()), getAllTypes(type).stream()))
			.toList();
	}

	// Returns (recursively) nested types
	private List<CtTypeReference<?>> getAllTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.flatMap(nestedType -> Stream.concat(Stream.of(nestedType.getReference()), getAllTypes(nestedType).stream()))
			.toList();
	}

	// Returns all fields in a type
	private List<CtField<?>> getFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns all methods in a type
	private List<CtMethod<?>> getMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns all constructors in a type
	private List<CtConstructor<?>> getConstructors(CtType<?> type) {
		if (type instanceof CtClass<?> cls) {
			return new ArrayList<>(cls.getConstructors().stream()
				.filter(this::isExported)
				.toList());
		}
		return Collections.emptyList();
	}

	// Converts Spoon's ModifierKind to Roseau's AccessModifier
	private AccessModifier convertVisibility(ModifierKind visibility) {
		if (visibility == ModifierKind.PUBLIC) {
			return AccessModifier.PUBLIC;
		} else if (visibility == ModifierKind.PRIVATE) {
			return AccessModifier.PRIVATE;
		} else if (visibility == ModifierKind.PROTECTED) {
			return AccessModifier.PROTECTED;
		} else {
			return AccessModifier.PACKAGE_PRIVATE;
		}
	}

	// Converts Spoon's non-access ModifierKind to Roseau's Modifier
	private Modifier convertNonAccessModifier(ModifierKind modifier) {
		if (modifier == ModifierKind.STATIC) {
			return Modifier.STATIC;
		} else if (modifier == ModifierKind.FINAL) {
			return Modifier.FINAL;
		} else if (modifier == ModifierKind.ABSTRACT) {
			return Modifier.ABSTRACT;
		} else if (modifier == ModifierKind.SYNCHRONIZED) {
			return Modifier.SYNCHRONIZED;
		} else if (modifier == ModifierKind.VOLATILE) {
			return Modifier.VOLATILE;
		} else if (modifier == ModifierKind.TRANSIENT) {
			return Modifier.TRANSIENT;
		} else if (modifier == ModifierKind.SEALED) {
			return Modifier.SEALED;
		} else if (modifier == ModifierKind.NON_SEALED) {
			return Modifier.NON_SEALED;
		} else if (modifier == ModifierKind.NATIVE) {
			return Modifier.NATIVE;
		} else {
			return Modifier.STRICTFP;
		}
	}

	// Filtering access modifiers because the convertVisibility() handles them already
	private List<Modifier> convertNonAccessModifiers(Set<ModifierKind> modifiers) {
		return modifiers.stream()
			.filter(mod ->
				   mod != ModifierKind.PUBLIC
				&& mod != ModifierKind.PROTECTED
				&& mod != ModifierKind.PRIVATE)
			.map(this::convertNonAccessModifier)
			.toList();
	}

	private TypeReference<TypeDecl> makeTypeReference(CtTypeReference<?> spoonType) {
		return new TypeReference<>(spoonType.getQualifiedName());
	}

	private TypeReference<ClassDecl> makeClassReference(CtTypeReference<?> spoonType) {
		return new TypeReference<>(spoonType.getQualifiedName());
	}

	private TypeReference<InterfaceDecl> makeInterfaceReference(CtTypeReference<?> spoonType) {
		return new TypeReference<>(spoonType.getQualifiedName());
	}

	private SourceLocation convertSourcePosition(SourcePosition position) {
		return !position.isValidPosition()
			? SourceLocation.NO_LOCATION
			: new SourceLocation(
					position.getFile() != null ? position.getFile().toPath() : Path.of("<unknown>"),
					position.getLine()
				);
	}

	// Converts a CtType to a Type declaration
	private TypeDecl convertSpoonTypeToTypeDeclaration(CtType<?> spoonType) {
		String qualifiedName = spoonType.getQualifiedName();
		AccessModifier visibility = convertVisibility(spoonType.getVisibility());
		boolean isExported = isExported(spoonType);
		List<Modifier> modifiers = convertNonAccessModifiers(spoonType.getModifiers());
		List<FormalTypeParameter> formalTypeParameters = spoonType.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();
		TypeReference<TypeDecl> containingType = spoonType.getDeclaringType() != null
			? makeTypeReference(spoonType.getDeclaringType().getReference())
			: null;
		SourceLocation location = convertSourcePosition(spoonType.getPosition());
		String position = spoonType.getPosition().toString();
		List<TypeReference<InterfaceDecl>> superInterfaces = spoonType.getSuperInterfaces().stream()
			.map(this::makeInterfaceReference)
			.toList();

		List<FieldDecl> convertedFields =
			getFields(spoonType).stream()
				.map(this::convertSpoonFieldToFieldDeclaration)
				.toList();

		List<MethodDecl> convertedMethods =
			getMethods(spoonType).stream()
				.map(this::convertSpoonMethodToMethodDeclaration)
				.toList();

		return switch (spoonType) {
			case CtAnnotationType<?> a ->
				new AnnotationDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, convertedFields, convertedMethods);
			case CtInterface<?> i ->
				new InterfaceDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods);
			case CtClass<?> c -> {
				TypeReference<ClassDecl> superClass = spoonType.getSuperclass() != null
					? makeClassReference(spoonType.getSuperclass())
					: null;
				List<ConstructorDecl> convertedConstructors =
					getConstructors(spoonType).stream()
						.map(this::convertSpoonConstructorToConstructorDeclaration)
						.toList();

				yield switch (c) {
					case CtRecord r ->
						new RecordDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, convertedConstructors);
					case CtEnum<?> e ->
						new EnumDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, convertedFields, convertedMethods, convertedConstructors);
					case CtClass<?> cc ->
						new ClassDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, superClass, convertedConstructors);
				};
			}
			default -> throw new IllegalStateException("Unexpected value: " + spoonType);
		};
	}

	// Converts a CtField to a Field declaration
	private FieldDecl convertSpoonFieldToFieldDeclaration(CtField<?> spoonField) {
		String qualifiedName = spoonField.getDeclaringType().getQualifiedName() + "." + spoonField.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonField.getVisibility());
		boolean isExported = isExported(spoonField);
		List<Modifier> modifiers = convertNonAccessModifiers(spoonField.getModifiers());
		SourceLocation location = convertSourcePosition(spoonField.getPosition());
		TypeReference<TypeDecl> containingType = makeTypeReference(spoonField.getDeclaringType().getReference());
		TypeReference<TypeDecl> type = makeTypeReference(spoonField.getType());

		return new FieldDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, type);
	}

	// Converts a CtMethod to a Method declaration
	private MethodDecl convertSpoonMethodToMethodDeclaration(CtMethod<?> spoonMethod) {
		String qualifiedName = spoonMethod.getDeclaringType().getQualifiedName() + "." + spoonMethod.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonMethod.getVisibility());
		boolean isExported = isExported(spoonMethod);
		List<Modifier> modifiers = convertNonAccessModifiers(spoonMethod.getModifiers());
		SourceLocation location = convertSourcePosition(spoonMethod.getPosition());
		TypeReference<TypeDecl> containingType = makeTypeReference(spoonMethod.getDeclaringType().getReference());
		TypeReference<TypeDecl> returnType = makeTypeReference(spoonMethod.getType());
		List<ParameterDecl> parameters = spoonMethod.getParameters().stream()
			.map(this::convertSpoonParameterToParameter)
			.toList();

		List<TypeReference<ClassDecl>> exceptions = spoonMethod.getThrownTypes()
			.stream()
			.map(this::makeClassReference)
			.toList();

		List<FormalTypeParameter> formalTypeParameters = spoonMethod.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();
		boolean isDefault = spoonMethod.isDefaultMethod();
		boolean isAbstract = spoonMethod.isAbstract();

		return new MethodDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, returnType, parameters, formalTypeParameters, exceptions, isDefault, isAbstract);
	}

	// Converts a CtConstructor to a Constructor declaration
	private ConstructorDecl convertSpoonConstructorToConstructorDeclaration(CtConstructor<?> spoonConstructor) {
		String qualifiedName = spoonConstructor.getDeclaringType().getQualifiedName() + "." + spoonConstructor.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonConstructor.getVisibility());
		boolean isExported = isExported(spoonConstructor);
		List<Modifier> modifiers = convertNonAccessModifiers(spoonConstructor.getModifiers());
		SourceLocation location = convertSourcePosition(spoonConstructor.getPosition());
		TypeReference<TypeDecl> containingType = makeTypeReference(spoonConstructor.getDeclaringType().getReference());
		TypeReference<TypeDecl> returnType = makeTypeReference(spoonConstructor.getType());
		List<ParameterDecl> parameters = spoonConstructor.getParameters().stream()
			.map(this::convertSpoonParameterToParameter)
			.toList();

		List<TypeReference<ClassDecl>> exceptions = spoonConstructor.getThrownTypes()
			.stream()
			.map(this::makeClassReference)
			.toList();

		List<FormalTypeParameter> formalTypeParameters = spoonConstructor.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();

		return new ConstructorDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, returnType, parameters, formalTypeParameters, exceptions);
	}

	private ParameterDecl convertSpoonParameterToParameter(CtParameter<?> parameter) {
		return new ParameterDecl(parameter.getSimpleName(), makeTypeReference(parameter.getType()), parameter.isVarArgs());
	}

	private FormalTypeParameter convertCtTypeParameter(CtTypeParameter parameter) {
		return new FormalTypeParameter(
			parameter.getSimpleName(),
			parameter.getSuperInterfaces().stream()
				.map(this::makeTypeReference)
				.toList()
		);
	}
}
