package com.github.maracas.roseau;

import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.Constructor;
import com.github.maracas.roseau.model.DeclarationKind;
import com.github.maracas.roseau.model.Field;
import com.github.maracas.roseau.model.Method;
import com.github.maracas.roseau.model.NonAccessModifiers;
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
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.declaration.ModifierKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This class represents roseau's API extraction tool.
 */
public class APIExtractor {
	private final CtModel model;

	/**
	 * Constructs an APIExtractor instance with the provided CtModel to extract its information.
	 */
	public APIExtractor(CtModel model) {
		this.model = Objects.requireNonNull(model);
	}

	/**
	 * Extracts the library's (model's) structured API.
	 *
	 * @return Library's (model's) API.
	 */
	public API extractingAPI() {
		List<CtPackage> allPackages = model.getAllPackages().stream().toList();
		List<Type> allTypes = new ArrayList<>();

		for (CtPackage pkg : allPackages) {  // Looping over the packages to extract all the library's types
			List<CtType<?>> types = getAccessibleTypes(pkg); // Only returning the packages' accessible types
			List<Type> typesConverted = convertingSpoonTypesToTypeDeclarations(types); // Transforming the spoon's CtTypes into TypeDeclarations

			if (!typesConverted.isEmpty()) {
				int i = 0;
				for (CtType<?> type : types) {  // Looping over spoon's types to fill the TypeDeclarations' fields / methods / constructors
					Type typeDeclaration = typesConverted.get(i);
					List<CtField<?>> fields = getAccessibleFields(type); // Returning the accessible fields of accessible types
					List<Field> fieldsConverted = convertingSpoonFieldsToFieldDeclarations(fields, typeDeclaration); // Transforming them into fieldDeclarations
					typeDeclaration.setFields(fieldsConverted);  // Adding them to the TypeDeclaration they belong to

					// Doing the same thing for methods and constructors
					List<CtMethod<?>> methods = getAccessibleMethods(type);
					List<Method> methodsConverted = convertingSpoonMethodsToMethodDeclarations(methods, typeDeclaration);
					typeDeclaration.setMethods(methodsConverted);

					List<CtConstructor<?>> constructors = getAccessibleConstructors(type);
					List<Constructor> constructorsConverted = convertingSpoonConstructorsToConstructorDeclarations(constructors, typeDeclaration);
					typeDeclaration.setConstructors(constructorsConverted);

					i++;
				}
			}

			allTypes.addAll(typesConverted);
		}

		// Adding the superclasses info
		API api = new API(allTypes);

		allTypes.forEach(typeDeclaration -> {
			String superclassName = typeDeclaration.getSuperclassName();
			if (!superclassName.equals("None")) {
				List<Type> superclasses = getAllSuperclasses(superclassName, allTypes);
				typeDeclaration.setAllSuperclasses(superclasses);
			}

			// Filling the superinterfaces too
			List<String> superinterfaceNames = typeDeclaration.getSuperinterfacesNames();
			List<Type> superinterfaces = new ArrayList<>();

			for (String superinterfaceName : superinterfaceNames) {
				Type superinterface = allTypes.stream()
					.filter(superInterfaceDec -> superInterfaceDec.getName().equals(superinterfaceName))
					.findFirst()
					.orElse(null);
				if (superinterface != null) {
					superinterfaces.add(superinterface);
				}
			}

			typeDeclaration.setSuperinterfaces(superinterfaces);
		});

		return api;  // returning the library's API
	}

	private boolean isAccessible(CtType<?> type) {
		return type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type));
	}

	private boolean isAccessible(CtModifiable member) {
		return member.isPublic() || member.isProtected();
	}

	private boolean isEffectivelyFinal(CtType<?> type) {
		return type.isFinal() || type.hasModifier(ModifierKind.SEALED);
	}

	// Returns all accessible types within a package
	private List<CtType<?>> getAccessibleTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.filter(this::isAccessible)
			.flatMap(type -> Stream.concat(Stream.of(type), getAccessibleTypes(type).stream()))
			.toList();
	}

	// Returns (recursively) the accessible nested types within a type
	private List<CtType<?>> getAccessibleTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.filter(this::isAccessible)
			.flatMap(nestedType -> Stream.concat(Stream.of(nestedType), getAccessibleTypes(nestedType).stream()))
			.toList();
	}

	// Returns the accessible fields of a type
	private List<CtField<?>> getAccessibleFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(this::isAccessible)
			.toList();
	}

	// Returns the accessible methods of a type
	private List<CtMethod<?>> getAccessibleMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(this::isAccessible)
			.toList();
	}

	// Returns the accessible constructors of a type
	private List<CtConstructor<?>> getAccessibleConstructors(CtType<?> type) {
		if (type instanceof CtClass<?> cls) {
			return new ArrayList<>(cls.getConstructors().stream()
				.filter(this::isAccessible)
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

	// Converts Spoon's non-access ModifierKind to Roseau's NonAccessModifier
	private NonAccessModifiers convertNonAccessModifier(ModifierKind modifier) {
		if (modifier == ModifierKind.STATIC) {
			return NonAccessModifiers.STATIC;
		} else if (modifier == ModifierKind.FINAL) {
			return NonAccessModifiers.FINAL;
		} else if (modifier == ModifierKind.ABSTRACT) {
			return NonAccessModifiers.ABSTRACT;
		} else if (modifier == ModifierKind.SYNCHRONIZED) {
			return NonAccessModifiers.SYNCHRONIZED;
		} else if (modifier == ModifierKind.VOLATILE) {
			return NonAccessModifiers.VOLATILE;
		} else if (modifier == ModifierKind.TRANSIENT) {
			return NonAccessModifiers.TRANSIENT;
		} else if (modifier == ModifierKind.SEALED) {
			return NonAccessModifiers.SEALED;
		} else if (modifier == ModifierKind.NON_SEALED) {
			return NonAccessModifiers.NON_SEALED;
		} else if (modifier == ModifierKind.NATIVE) {
			return NonAccessModifiers.NATIVE;
		} else {
			return NonAccessModifiers.STRICTFP;
		}
	}

	// Filtering access modifiers because the convertVisibility() handles them already
	private List<NonAccessModifiers> filterNonAccessModifiers(Set<ModifierKind> modifiers) {
		List<NonAccessModifiers> nonAccessModifiers = new ArrayList<>();

		for (ModifierKind modifier : modifiers) {
			if (modifier != ModifierKind.PUBLIC && modifier != ModifierKind.PRIVATE
				&& modifier != ModifierKind.PROTECTED) {
				nonAccessModifiers.add(convertNonAccessModifier(modifier));
			}
		}

		return nonAccessModifiers;
	}

	// Returning the type's kind ( whether if it's a class/enum/interface/annotation/record )
	private DeclarationKind convertTypeType(CtType<?> type) {
		if (type.isClass())
			return DeclarationKind.CLASS;
		if (type.isInterface())
			return DeclarationKind.INTERFACE;
		if (type.isEnum())
			return DeclarationKind.ENUM;
		if (type.isAnnotationType())
			return DeclarationKind.ANNOTATION;
		else
			return DeclarationKind.RECORD;
	}

	// The conversion functions : Moving from spoon's Ct kinds to roseau's Declaration kinds
	private List<Type> convertingSpoonTypesToTypeDeclarations(List<CtType<?>> spoonTypes) {
		return spoonTypes.stream()
			.map(spoonType -> {
				// Extracting relevant information from the spoonType
				String name = spoonType.getQualifiedName();
				AccessModifier visibility = convertVisibility(spoonType.getVisibility());
				DeclarationKind declarationKind = convertTypeType(spoonType);
				List<NonAccessModifiers> modifiers = filterNonAccessModifiers(spoonType.getModifiers());
				String superclassName = "None";
				if (spoonType.getSuperclass() != null) {
					superclassName = spoonType.getSuperclass().getQualifiedName();
				}
				List<String> superinterfacesNames = spoonType.getSuperInterfaces().stream()
					.map(superinterface -> superinterface.getQualifiedName())
					.toList();
				List<String> referencedTypes = spoonType.getReferencedTypes().stream()
					.map(referencedType -> referencedType.toString())
					.toList();
				List<String> formalTypeParameters = spoonType.getFormalCtTypeParameters().stream()
					.map(formalTypeParameter -> formalTypeParameter.getQualifiedName())
					.toList();
				List<List<String>> formalTypeParamsBounds = spoonType.getFormalCtTypeParameters().stream()
					.map(formalTypeParameter -> formalTypeParameter.getReferencedTypes().stream()
						.map(formalParamReferencedType -> formalParamReferencedType.toString())
						.toList()
					)
					.toList();
				boolean isNested = !spoonType.isTopLevel();
				String position = spoonType.getPosition().toString();

				// Creating a new TypeDeclaration object using the extracted information
				return new Type(name, visibility, declarationKind, modifiers, superclassName, superinterfacesNames, referencedTypes, formalTypeParameters, formalTypeParamsBounds, isNested, position);
			})
			.toList(); // Adding it to the list of TypeDeclarations
	}

	private List<Field> convertingSpoonFieldsToFieldDeclarations(List<CtField<?>> spoonFields, Type type) {
		return spoonFields.stream()
			.map(spoonField -> {
				// Extracting relevant information from the spoonField
				String name = spoonField.getSimpleName();
				AccessModifier visibility = convertVisibility(spoonField.getVisibility());
				String dataType = spoonField.getType().getQualifiedName();
				List<NonAccessModifiers> modifiers = filterNonAccessModifiers(spoonField.getModifiers());
				List<String> referencedTypes = spoonField.getReferencedTypes().stream()
					.map(referencedType -> referencedType.toString())
					.toList();
				String position = spoonField.getPosition().toString();
				// Creating a new FieldDeclaration object using the extracted information
				return new Field(name, type, visibility, dataType, modifiers, referencedTypes, position);
			})
			.toList();  // Adding it to the list of FieldDeclarations
	}

	private List<Method> convertingSpoonMethodsToMethodDeclarations(List<CtMethod<?>> spoonMethods, Type type) {
		return spoonMethods.stream()
			.map(spoonMethod -> {
				// Extracting relevant information from the spoonMethod
				String name = spoonMethod.getSimpleName();
				AccessModifier visibility = convertVisibility(spoonMethod.getVisibility());
				String returnType = spoonMethod.getType().getQualifiedName();
				List<String> returnTypeReferencedType = spoonMethod.getReferencedTypes().stream()
					.map(ReferencedType -> ReferencedType.toString())
					.toList();

				List<NonAccessModifiers> modifiers = filterNonAccessModifiers(spoonMethod.getModifiers());
				List<String> parametersTypes = spoonMethod.getParameters().stream()
					.map(parameterType -> parameterType.getType().getQualifiedName())
					.toList();
				List<List<String>> parametersReferencedTypes = spoonMethod.getParameters().stream()
					.map(parameter -> parameter.getReferencedTypes().stream()
						.map(parametersReferencedType -> parametersReferencedType.toString())
						.toList())
					.toList();
				List<String> formalTypeParameters = spoonMethod.getFormalCtTypeParameters().stream()
					.map(formalTypeParameter -> formalTypeParameter.getQualifiedName())
					.toList();
				List<List<String>> formalTypeParamsBounds = spoonMethod.getFormalCtTypeParameters().stream()
					.map(formalTypeParameter -> formalTypeParameter.getReferencedTypes().stream()
						.map(formalParamReferencedType -> formalParamReferencedType.toString())
						.toList()
					)
					.toList();
				Signature signature = new Signature(name, parametersTypes);

				List<String> exceptions = spoonMethod.getThrownTypes()
					.stream()
					.filter(exception -> !exception.getQualifiedName().equals("java.lang.RuntimeException")
						&& (exception.getSuperclass() == null || !exception.getSuperclass().getQualifiedName().equals("java.lang.RuntimeException")))
					.map(exception -> exception.getQualifiedName())
					.toList();

				List<Boolean> parametersVarargsCheck = spoonMethod.getParameters().stream()
					.map(CtParameter::isVarArgs)
					.toList();
				boolean isDefault = spoonMethod.isDefaultMethod();
				String position = spoonMethod.getPosition().toString();
				// Creating a new MethodDeclaration object using the extracted information
				return new Method(name, type, visibility, returnType, returnTypeReferencedType, parametersTypes, parametersReferencedTypes, formalTypeParameters, formalTypeParamsBounds, modifiers, signature, exceptions, parametersVarargsCheck, isDefault, position);
			})
			.toList();  // Adding it to the list of MethodDeclarations
	}

	private List<Constructor> convertingSpoonConstructorsToConstructorDeclarations(List<CtConstructor<?>> spoonConstructors, Type type) {
		return spoonConstructors.stream()
			.map(spoonConstructor -> {
				// Extracting relevant information from the spoonConstructor
				String name = spoonConstructor.getSimpleName();
				AccessModifier visibility = convertVisibility(spoonConstructor.getVisibility());
				String returnType = spoonConstructor.getType().getQualifiedName();
				List<String> returnTypeReferencedType = spoonConstructor.getReferencedTypes().stream()
					.map(ReferencedType -> ReferencedType.toString())
					.toList();
				List<String> parametersTypes = spoonConstructor.getParameters().stream()
					.map(parameterType -> parameterType.getType().getQualifiedName())
					.toList();
				List<List<String>> parametersReferencedTypes = spoonConstructor.getParameters().stream()
					.map(parameter -> parameter.getReferencedTypes().stream()
						.map(parametersReferencedType -> parametersReferencedType.toString())
						.toList())
					.toList();
				List<String> formalTypeParameters = spoonConstructor.getFormalCtTypeParameters().stream()
					.map(formalTypeParameter -> formalTypeParameter.getQualifiedName())
					.toList();
				List<List<String>> formalTypeParamsBounds = spoonConstructor.getFormalCtTypeParameters().stream()
					.map(formalTypeParameter -> formalTypeParameter.getReferencedTypes().stream()
						.map(formalParamReferencedType -> formalParamReferencedType.toString())
						.toList()
					)
					.toList();
				List<NonAccessModifiers> modifiers = filterNonAccessModifiers(spoonConstructor.getModifiers());
				Signature signature = new Signature(name, parametersTypes);
				List<String> exceptions = spoonConstructor.getThrownTypes().stream()
					.filter(exception -> !exception.getQualifiedName().equals("java.lang.RuntimeException")
						&& (exception.getSuperclass() == null || !exception.getSuperclass().getQualifiedName().equals("java.lang.RuntimeException")))
					.map(CtTypeInformation::getQualifiedName)
					.toList();
				String position = type.getPosition().toString();
				// Creating a new ConstructorDeclaration object using the extracted information
				return new Constructor(name, type, visibility, returnType, returnTypeReferencedType, parametersTypes, parametersReferencedTypes, formalTypeParameters, formalTypeParamsBounds, modifiers, signature, exceptions, position);
			})
			.toList();  // Adding it to the list of ConstructorDeclarations
	}

	// Get all the superclasses of a type, direct or not
	private List<Type> getAllSuperclasses(String className, List<Type> allTypes) {
		List<Type> superclasses = new ArrayList<>();
		Type currentType = getTypeByName(className, allTypes);

		if (currentType != null) {
			String superclassName = currentType.getSuperclassName();
			if (!superclassName.equals("None")) {
				List<Type> directSuperclasses = getAllSuperclasses(superclassName, allTypes);
				superclasses.add(currentType);
				superclasses.addAll(directSuperclasses);
			} else {
				superclasses.add(currentType);
			}
		}

		return superclasses;
	}


	// Helper to get a TypeDeclaration by name
	private Type getTypeByName(String className, List<Type> allTypes) {
		return allTypes.stream()
			.filter(typeDeclaration -> typeDeclaration.getName().equals(className))
			.findFirst()
			.orElse(null);
	}
}
