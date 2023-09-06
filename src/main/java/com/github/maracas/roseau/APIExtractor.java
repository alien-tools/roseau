package com.github.maracas.roseau;

import com.github.maracas.roseau.model.ConstructorDeclaration;
import com.github.maracas.roseau.model.FieldDeclaration;
import com.github.maracas.roseau.model.MethodDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;
import com.github.maracas.roseau.model.TypeType;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.NonAccessModifiers;
import com.github.maracas.roseau.model.Signature;
import com.github.maracas.roseau.model.API;

import spoon.reflect.CtModel;

import spoon.reflect.declaration.*;
import spoon.reflect.declaration.ModifierKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

	// Returning the packages as spoon CtPackages
	private List<CtPackage> rawSpoonPackages() {
		return model.getAllPackages().stream()
				.peek(packageDeclaration -> {
					//System.out.println("Package: " + packageDeclaration.getQualifiedName());
				})
				.toList();
	}


	private boolean typeIsAccessible(CtType<?> type) {
		if (type.getVisibility() == ModifierKind.PUBLIC) {
			return true;
		} else if (type.getVisibility() == ModifierKind.PROTECTED) {
			return !type.isFinal() && !type.getModifiers().contains(ModifierKind.SEALED);
		} else {
			return false;
		}
	}

	private boolean memberIsAccessible(CtModifiable member) {
		return member.isPublic() || member.isProtected();
	}

	// Returning the accessible types of a package as spoon CtTypes

	private List<CtType<?>> extractedSpoonTypes(CtPackage pkg) {
		List<CtType<?>> types = new ArrayList<>();
		pkg.getTypes().stream()
				.filter(this::typeIsAccessible)
				.forEach(type -> {

					//if (type.getSuperclass() != null) {
						// System.out.println("Type: " + type.getPosition().toString());
						// System.out.println("Type: " + type.getSuperInterfaces());
					//}

					types.add(type);
					extractingSpoonNestedTypes(type, types);
				});
		return types;
	}

	// Handling nested types
	private void extractingSpoonNestedTypes(CtType<?> parentType, List<CtType<?>> types) {
		parentType.getNestedTypes().stream()
				.filter(this::typeIsAccessible)
				.forEach(type -> {
					types.add(type);
					extractingSpoonNestedTypes(type, types);
				});
	}


	// Returning the accessible fields of a type as spoon CtFields
	private List<CtField<?>> extractedSpoonFields(CtType<?> type) {
		return type.getFields().stream()
				.filter(this::memberIsAccessible)
				.peek(field -> {
					//System.out.println("Field: " + field.getSimpleName().toString() );


				})
				.toList();
	}

    // Returning the accessible methods of a type as spoon CtMethods
	private List<CtMethod<?>> extractedSpoonMethods(CtType<?> type) {
		return type.getMethods().stream()
				.filter(this::memberIsAccessible)
				.peek(method -> {
					//System.out.println("Method: " + method.isDefaultMethod());
				})
				.toList();
	}

	// Returning the accessible constructors of a type as spoon CtConstructors
	private List<CtConstructor<?>> extractedSpoonConstructors(CtType<?> type) {
		if (type instanceof CtClass<?> cls) {
			return new ArrayList<>(cls.getConstructors().stream()
					.filter(this::memberIsAccessible)
					.peek(constructor -> {
						//System.out.println("Constructor: " + type.getPosition().toString());
					})
					.toList());
		}
		return Collections.emptyList();

	}



	// Converting spoon's access ModifierKind to roseau's enum : AccessModifier
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

	// Converting spoon's Non-access ModifierKind to roseau's enum : NonAccessModifier
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
					&& modifier != ModifierKind.PROTECTED ) {
				nonAccessModifiers.add(convertNonAccessModifier(modifier));
			}
		}

		return nonAccessModifiers;
	}




	// Returning the type's kind ( whether if it's a class/enum/interface/annotation/record )
	private TypeType convertTypeType(CtType<?> type) {
		if (type.isClass())
			return TypeType.CLASS;
		if (type.isInterface())
			return TypeType.INTERFACE;
		if (type.isEnum())
			return TypeType.ENUM;
		if (type.isAnnotationType())
			return TypeType.ANNOTATION;
		else
			return TypeType.RECORD;
	}


	// The conversion functions : Moving from spoon's Ct kinds to roseau's Declaration kinds

	private List<TypeDeclaration> convertingSpoonTypesToTypeDeclarations(List<CtType<?>> spoonTypes) {
		return spoonTypes.stream()
				.map(spoonType -> {
					// Extracting relevant information from the spoonType
					String name = spoonType.getQualifiedName();
					AccessModifier visibility = convertVisibility(spoonType.getVisibility());
					TypeType typeType = convertTypeType(spoonType);
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
							.map(formalTypeParameter -> formalTypeParameter.toString())
							.toList();
					boolean isNested = !spoonType.isTopLevel();
					String position = spoonType.getPosition().toString();

					// Creating a new TypeDeclaration object using the extracted information
					return new TypeDeclaration(name, visibility, typeType, modifiers, superclassName, superinterfacesNames,referencedTypes, formalTypeParameters, isNested, position);

					})

				.toList(); // Adding it to the list of TypeDeclarations
	}


	private List<FieldDeclaration> convertingSpoonFieldsToFieldDeclarations(List<CtField<?>> spoonFields, TypeDeclaration type) {
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
					return new FieldDeclaration(name, type, visibility, dataType,modifiers, referencedTypes, position );
				})

				.toList();  // Adding it to the list of FieldDeclarations
	}

	private List<MethodDeclaration> convertingSpoonMethodsToMethodDeclarations(List<CtMethod<?>> spoonMethods, TypeDeclaration type) {
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
							.map(formalTypeParameter -> formalTypeParameter.toString())
							.toList();
					Signature signature = new Signature(name, parametersTypes);
					List<String> exceptions = spoonMethod.getThrownTypes().stream()
							.map(exception-> exception.getQualifiedName())
							.toList();
					List<Boolean> parametersVarargsCheck = spoonMethod.getParameters().stream()
							.map(parameter -> parameter.isVarArgs())
							.toList();
					boolean isDefault = spoonMethod.isDefaultMethod();
					String position = spoonMethod.getPosition().toString();
					// Creating a new MethodDeclaration object using the extracted information
					return new MethodDeclaration(name, type, visibility, returnType, returnTypeReferencedType, parametersTypes, parametersReferencedTypes, formalTypeParameters, modifiers, signature, exceptions, parametersVarargsCheck, isDefault, position);
				})

				.toList();  // Adding it to the list of MethodDeclarations
	}

	private List<ConstructorDeclaration> convertingSpoonConstructorsToConstructorDeclarations(List<CtConstructor<?>> spoonConstructors, TypeDeclaration type) {
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
							.map(formalTypeParameter -> formalTypeParameter.toString())
							.toList();
					List<NonAccessModifiers> modifiers = filterNonAccessModifiers(spoonConstructor.getModifiers());
					Signature signature = new Signature(name, parametersTypes);
					List<String> exceptions = spoonConstructor.getThrownTypes().stream()
							.map(exception-> exception.getQualifiedName())
							.toList();
					String position = type.getPosition().toString();
					// Creating a new ConstructorDeclaration object using the extracted information
					return new ConstructorDeclaration(name, type, visibility, returnType, returnTypeReferencedType, parametersTypes, parametersReferencedTypes, formalTypeParameters, modifiers,signature, exceptions, position);
				})

				.toList();  // Adding it to the list of ConstructorDeclarations
	}





	/**
	 * Extracts the library's (model's) structured API.

	 * @return Library's (model's) API.
	 */

	public API extractingAPI() {
		List<CtPackage> packages = rawSpoonPackages(); // Returning packages
		List<TypeDeclaration> AllTheTypes = new ArrayList<>();

		for (CtPackage pkg : packages) {  // Looping over the packages to extract all the library's types

			List<CtType<?>> types = extractedSpoonTypes(pkg); // Only returning the packages' accessible types
			List<TypeDeclaration> typesConverted = convertingSpoonTypesToTypeDeclarations(types); // Transforming the spoon's CtTypes into TypeDeclarations

			if (!typesConverted.isEmpty()) {
				int i=0;
				for (CtType<?> type : types) {  // Looping over spoon's types to fill the TypeDeclarations' fields / methods / constructors

					TypeDeclaration typeDeclaration = typesConverted.get(i);

					List<CtField<?>> fields = extractedSpoonFields(type); // Returning the accessible fields of accessible types
					List<FieldDeclaration> fieldsConverted = convertingSpoonFieldsToFieldDeclarations(fields, typeDeclaration); // Transforming them into fieldDeclarations
					typeDeclaration.setFields(fieldsConverted);  // Adding them to the TypeDeclaration they belong to

					// Doing the same thing for methods and constructors

					List<CtMethod<?>> methods = extractedSpoonMethods(type);
					List<MethodDeclaration> methodsConverted = convertingSpoonMethodsToMethodDeclarations(methods, typeDeclaration);
					typeDeclaration.setMethods(methodsConverted);

					List<CtConstructor<?>> constructors = extractedSpoonConstructors(type);
					List<ConstructorDeclaration> constructorsConverted = convertingSpoonConstructorsToConstructorDeclarations(constructors, typeDeclaration);
					typeDeclaration.setConstructors(constructorsConverted);

					i++;

				};
			}

			AllTheTypes.addAll(typesConverted);

		}


		// Adding the superclasses info

		API api = new API(AllTheTypes);

		AllTheTypes.forEach(typeDeclaration -> {
			String superclassName = typeDeclaration.getSuperclassName();
			if (superclassName.equals("None")) {
				typeDeclaration.setSuperclass(null);
			} else {
				TypeDeclaration superclass = AllTheTypes.stream()
						.filter(superClassDec -> superClassDec.getName().equals(superclassName))
						.findFirst()
						.orElse(null);
				typeDeclaration.setSuperclass(superclass);
			}
		});


		return api;  // returning the library's API

	}





}
