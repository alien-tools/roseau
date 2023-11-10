package com.github.maracas.roseau;

import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.Constructor;
import com.github.maracas.roseau.model.DeclarationKind;
import com.github.maracas.roseau.model.Field;
import com.github.maracas.roseau.model.Method;
import com.github.maracas.roseau.model.Modifier;
import com.github.maracas.roseau.model.Signature;
import com.github.maracas.roseau.model.Type;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

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

		// Step 1: mapping all exported types
		Map<CtTypeReference<?>, Type> allTypes = allPackages.stream()
			.map(this::getExportedTypes)
			.flatMap(Collection::stream)
			.collect(Collectors.toMap(
				t -> t,
				t -> convertSpoonTypeToTypeDeclaration(t.getTypeDeclaration())
			));

		// Step 2: re-creating type hierarchy
		allTypes.forEach((spoonTypeRef, typeDeclaration) -> {
			if (spoonTypeRef.getSuperclass() != null && allTypes.containsKey(spoonTypeRef.getSuperclass()))
				typeDeclaration.setSuperClass(allTypes.get(spoonTypeRef.getSuperclass()));

			List<Type> superInterfaces = spoonTypeRef.getSuperInterfaces().stream()
				.filter(allTypes::containsKey)
				.map(allTypes::get)
				.toList();

			typeDeclaration.setSuperInterfaces(superInterfaces);
		});

		return new API(allTypes.values().stream().toList());
	}

	private boolean isExported(CtType<?> type) {
		return type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type));
	}

	private boolean isExported(CtModifiable member) {
		return member.isPublic() || member.isProtected();
	}

	private boolean isEffectivelyFinal(CtType<?> type) {
		return type.isFinal() || type.hasModifier(ModifierKind.SEALED);
	}

	// Returns all exported types within a package
	private List<CtTypeReference<?>> getExportedTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.filter(this::isExported)
			.flatMap(type -> Stream.concat(Stream.of(type.getReference()), getExportedTypes(type).stream()))
			.toList();
	}

	// Returns (recursively) the exported nested types within a type
	private List<CtTypeReference<?>> getExportedTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.filter(this::isExported)
			.flatMap(nestedType -> Stream.concat(Stream.of(nestedType.getReference()), getExportedTypes(nestedType).stream()))
			.toList();
	}

	// Returns the exported fields of a type
	private List<CtField<?>> getExportedFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns the exported methods of a type
	private List<CtMethod<?>> getExportedMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns the exported constructors of a type
	private List<CtConstructor<?>> getExportedConstructors(CtType<?> type) {
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
			return AccessModifier.DEFAULT;
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
	private List<Modifier> filterNonAccessModifiers(Set<ModifierKind> modifiers) {
		return modifiers.stream()
			.filter(mod ->
				   mod != ModifierKind.PUBLIC
				&& mod != ModifierKind.PROTECTED
				&& mod != ModifierKind.PRIVATE)
			.map(this::convertNonAccessModifier)
			.toList();
	}

	// Maps a CtType to its kind (class/enum/interface/annotation/record)
	private DeclarationKind convertDeclarationKind(CtType<?> type) {
		if (type.isInterface())
			return DeclarationKind.INTERFACE;
		if (type.isEnum())
			return DeclarationKind.ENUM;
		if (type.isAnnotationType())
			return DeclarationKind.ANNOTATION;
		if (type instanceof CtRecord)
			return DeclarationKind.RECORD;
		if (type.isClass()) // Keep last; record.isClass() == true
			return DeclarationKind.CLASS;

		throw new IllegalStateException("Unknown type kind " + type.getQualifiedName());
	}

	// Converts a CtType to a Type declaration
	private Type convertSpoonTypeToTypeDeclaration(CtType<?> spoonType) {
		String name = spoonType.getQualifiedName();
		AccessModifier visibility = convertVisibility(spoonType.getVisibility());
		DeclarationKind declarationKind = convertDeclarationKind(spoonType);
		List<Modifier> modifiers = filterNonAccessModifiers(spoonType.getModifiers());

		List<String> referencedTypes = spoonType.getReferencedTypes().stream()
			.map(CtTypeInformation::getQualifiedName)
			.toList();
		List<String> formalTypeParameters = spoonType.getFormalCtTypeParameters().stream()
			.map(CtTypeInformation::getQualifiedName)
			.toList();
		List<List<String>> formalTypeParamsBounds = spoonType.getFormalCtTypeParameters().stream()
			.map(formalTypeParameter -> formalTypeParameter.getReferencedTypes().stream()
				.map(CtTypeInformation::getQualifiedName)
				.toList()
			)
			.toList();
		boolean isNested = !spoonType.isTopLevel();
		List<CtTypeReference<?>> allSuperClasses = getAllSuperclasses(spoonType.getReference());
		boolean isCheckedException =
			   allSuperClasses.stream().anyMatch(superClass -> superClass.getQualifiedName().equals("java.lang.Exception"))
			&& allSuperClasses.stream().noneMatch(superClass -> superClass.getQualifiedName().equals("java.lang.RuntimeException"));
		String position = spoonType.getPosition().toString();

		List<Field> convertedFields =
			getExportedFields(spoonType).stream()
				.map(this::convertSpoonFieldToFieldDeclaration)
				.toList();

		List<Method> convertedMethods =
			getExportedMethods(spoonType).stream()
				.map(this::convertSpoonMethodToMethodDeclaration)
				.toList();

		List<Constructor> convertedConstructors =
			getExportedConstructors(spoonType).stream()
				.map(this::convertSpoonConstructorToConstructorDeclaration)
				.toList();

		return new Type(name, visibility, declarationKind, modifiers,
			referencedTypes, formalTypeParameters, formalTypeParamsBounds, isNested, isCheckedException, position,
			convertedFields, convertedMethods, convertedConstructors);
	}

	// Converts a CtField to a Field declaration
	private Field convertSpoonFieldToFieldDeclaration(CtField<?> spoonField) {
		String name = spoonField.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonField.getVisibility());
		String dataType = spoonField.getType().getQualifiedName();
		List<Modifier> modifiers = filterNonAccessModifiers(spoonField.getModifiers());
		List<String> referencedTypes = spoonField.getReferencedTypes().stream()
			.map(CtTypeInformation::getQualifiedName)
			.toList();
		String position = spoonField.getPosition().toString();

		return new Field(name, visibility, dataType, modifiers, referencedTypes, position);
	}

	// Converts a CtMethod to a Method declaration
	private Method convertSpoonMethodToMethodDeclaration(CtMethod<?> spoonMethod) {
		String name = spoonMethod.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonMethod.getVisibility());
		String returnType = spoonMethod.getType().getQualifiedName();
		List<String> returnTypeReferencedType = spoonMethod.getReferencedTypes().stream()
			.map(CtTypeInformation::getQualifiedName)
			.toList();

		List<Modifier> modifiers = filterNonAccessModifiers(spoonMethod.getModifiers());
		List<String> parametersTypes = spoonMethod.getParameters().stream()
			.map(parameterType -> parameterType.getType().getQualifiedName())
			.toList();
		List<List<String>> parametersReferencedTypes = spoonMethod.getParameters().stream()
			.map(parameter -> parameter.getReferencedTypes().stream()
				.map(CtTypeInformation::getQualifiedName)
				.toList())
			.toList();
		List<String> formalTypeParameters = spoonMethod.getFormalCtTypeParameters().stream()
			.map(CtTypeInformation::getQualifiedName)
			.toList();
		List<List<String>> formalTypeParamsBounds = spoonMethod.getFormalCtTypeParameters().stream()
			.map(formalTypeParameter -> formalTypeParameter.getReferencedTypes().stream()
				.map(CtTypeInformation::getQualifiedName)
				.toList()
			)
			.toList();
		Signature signature = new Signature(name, parametersTypes);

		List<String> exceptions = spoonMethod.getThrownTypes()
			.stream()
			.filter(exception -> !exception.getQualifiedName().equals("java.lang.RuntimeException")
				&& (exception.getSuperclass() == null || !exception.getSuperclass().getQualifiedName().equals("java.lang.RuntimeException")))
			.map(CtTypeInformation::getQualifiedName)
			.toList();

		List<Boolean> parametersVarargsCheck = spoonMethod.getParameters().stream()
			.map(CtParameter::isVarArgs)
			.toList();
		boolean isDefault = spoonMethod.isDefaultMethod();
		String position = spoonMethod.getPosition().toString();

		return new Method(name, visibility, returnType, returnTypeReferencedType, parametersTypes, parametersReferencedTypes, formalTypeParameters, formalTypeParamsBounds, modifiers, signature, exceptions, parametersVarargsCheck, isDefault, position);
	}

	// Converts a CtConstructor to a Constructor declaration
	private Constructor convertSpoonConstructorToConstructorDeclaration(CtConstructor<?> spoonConstructor) {
		String name = spoonConstructor.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonConstructor.getVisibility());
		String returnType = spoonConstructor.getType().getQualifiedName();
		List<String> returnTypeReferencedType = spoonConstructor.getReferencedTypes().stream()
			.map(CtTypeInformation::getQualifiedName)
			.toList();
		List<String> parametersTypes = spoonConstructor.getParameters().stream()
			.map(parameterType -> parameterType.getType().getQualifiedName())
			.toList();
		List<List<String>> parametersReferencedTypes = spoonConstructor.getParameters().stream()
			.map(parameter -> parameter.getReferencedTypes().stream()
				.map(CtTypeInformation::getQualifiedName)
				.toList())
			.toList();
		List<String> formalTypeParameters = spoonConstructor.getFormalCtTypeParameters().stream()
			.map(CtTypeInformation::getQualifiedName)
			.toList();
		List<List<String>> formalTypeParamsBounds = spoonConstructor.getFormalCtTypeParameters().stream()
			.map(formalTypeParameter -> formalTypeParameter.getReferencedTypes().stream()
				.map(CtTypeInformation::getQualifiedName)
				.toList()
			)
			.toList();
		List<Modifier> modifiers = filterNonAccessModifiers(spoonConstructor.getModifiers());
		Signature signature = new Signature(name, parametersTypes);
		List<String> exceptions = spoonConstructor.getThrownTypes().stream()
			.filter(exception -> !exception.getQualifiedName().equals("java.lang.RuntimeException")
				&& (exception.getSuperclass() == null || !exception.getSuperclass().getQualifiedName().equals("java.lang.RuntimeException")))
			.map(CtTypeInformation::getQualifiedName)
			.toList();
		List<Boolean> parametersVarargsCheck = spoonConstructor.getParameters().stream()
			.map(CtParameter::isVarArgs)
			.toList();
		String position = spoonConstructor.getPosition().toString();

		return new Constructor(name, visibility, returnType, returnTypeReferencedType, parametersTypes, parametersReferencedTypes, formalTypeParameters, formalTypeParamsBounds, modifiers, signature, exceptions, parametersVarargsCheck, position);
	}

	private List<CtTypeReference<?>> getAllSuperclasses(CtTypeReference<?> type) {
		if (type.getSuperclass() != null) {
			return Stream.concat(Stream.of(type.getSuperclass()), getAllSuperclasses(type.getSuperclass()).stream()).toList();
		} else return Collections.emptyList();
	}
}
