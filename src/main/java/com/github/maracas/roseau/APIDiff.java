package com.github.maracas.roseau;

import com.github.maracas.roseau.changes.BreakingChange;
import com.github.maracas.roseau.changes.BreakingChangeKind;
import com.github.maracas.roseau.changes.BreakingChangeNature;
import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.Constructor;
import com.github.maracas.roseau.model.Field;
import com.github.maracas.roseau.model.Method;
import com.github.maracas.roseau.model.Modifier;
import com.github.maracas.roseau.model.Type;
import com.github.maracas.roseau.model.DeclarationKind;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class represents Roseau's comparison tool for detecting breaking changes between two API versions.
 */
public class APIDiff {
	/**
	 * The first version of the API to be compared.
	 */
	private final API v1;

	/**
	 * The second version of the API to be compared.
	 */
	private final API v2;

	/**
	 * List of all the breaking changes identified in the comparison.
	 */
	private final List<BreakingChange> breakingChanges;

	private boolean breakingChangesPopulated = false;

	/**
	 * Constructs an APIDiff instance to compare two API versions for breaking changes detection.
	 *
	 * @param v1 The first version of the API to compare.
	 * @param v2 The second version of the API to compare.
	 */
	public APIDiff(API v1, API v2) {
		this.v1 = Objects.requireNonNull(v1);
		this.v2 = Objects.requireNonNull(v2);
		this.breakingChanges = new ArrayList<>();
	}

	private List<Type> checkingForRemovedTypes() {
		return v1.types().stream()
			.filter(type -> v2.types().stream().noneMatch(t -> t.getName().equals(type.getName())))
			.peek(removedType -> {
				if (removedType.getDeclarationType().equals(DeclarationKind.CLASS))
					breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_REMOVED, removedType.getPosition(), BreakingChangeNature.DELETION, removedType));

				if (removedType.getDeclarationType().equals(DeclarationKind.INTERFACE))
					breakingChanges.add(new BreakingChange(BreakingChangeKind.INTERFACE_REMOVED, removedType.getPosition(), BreakingChangeNature.DELETION, removedType));
			})
			.toList();
	}

	private List<List<Type>> getUnremovedTypes() {
		List<Type> unremovedTypes1 = v1.types().stream()
			.filter(type -> v2.types().stream().anyMatch(t -> t.getName().equals(type.getName())))
			.toList();

		List<Type> typesInParallelFrom2 = v2.types().stream()
			.filter(type -> unremovedTypes1.stream().anyMatch(t -> t.getName().equals(type.getName())))
			.toList();

		List<List<Type>> result = new ArrayList<>();
		result.add(unremovedTypes1);
		result.add(typesInParallelFrom2);

		return result;
	}

	private List<Field> checkingForRemovedFields(Type type1, Type type2) {
		return type1.getFields().stream()
			.filter(field1 -> type2.getFields().stream().noneMatch(field2 -> field2.getName().equals(field1.getName())))
			.peek(removedField -> {
				breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_REMOVED, removedField.getPosition(), BreakingChangeNature.DELETION, removedField));
			})
			.toList();
	}


	private List<Method> checkingForRemovedMethods(Type type1, Type type2) {
		return type1.getMethods().stream()
			.filter(method2 -> type2.getMethods().stream()
				.noneMatch(method1 -> method1.getSignature().getName().equals(method2.getSignature().getName()) && method1.getSignature().getParameterTypes().equals(method2.getSignature().getParameterTypes())))
			.peek(removedMethod -> {
				if (type2.getAllSuperclasses() != null) {
					List<Method> allSuperMethodsV2 = Stream.concat(
							type2.getAllSuperclasses().stream().flatMap(superType -> superType.getMethods().stream()),
							type2.getSuperinterfaces().stream().flatMap(superInterface -> superInterface.getMethods().stream())
						)
						.toList();

					boolean overriddenOrMovedMethodExists = allSuperMethodsV2.stream()
						.anyMatch(method -> method.getSignature().getName().equals(removedMethod.getSignature().getName()) &&
							method.getSignature().getParameterTypes().equals(removedMethod.getSignature().getParameterTypes()));

					if (!overriddenOrMovedMethodExists) {
						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_REMOVED, removedMethod.getPosition(), BreakingChangeNature.DELETION, removedMethod));
					}
				} else if (type2.getAllSuperclasses() == null && !type2.getSuperinterfaces().isEmpty()) {
					List<Method> allSuperMethods = type2.getSuperinterfaces().stream()
						.flatMap(superInterface -> superInterface.getMethods().stream())
						.toList();

					boolean overriddenOrMovedMethodExists = allSuperMethods.stream()
						.anyMatch(method -> method.getSignature().getName().equals(removedMethod.getSignature().getName()) &&
							method.getSignature().getParameterTypes().equals(removedMethod.getSignature().getParameterTypes()));

					if (!overriddenOrMovedMethodExists) {
						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_REMOVED, removedMethod.getPosition(), BreakingChangeNature.DELETION, removedMethod));
					}
				} else {
					breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_REMOVED, removedMethod.getPosition(), BreakingChangeNature.DELETION, removedMethod));
				}
			})
			.toList();
	}


	private List<Constructor> checkingForRemovedConstructors(Type type1, Type type2) {
		return type1.getConstructors().stream()
			.filter(constructor1 -> type2.getConstructors().stream()
				.noneMatch(constructor2 -> constructor2.getSignature().getName().equals(constructor1.getSignature().getName()) && constructor2.getSignature().getParameterTypes().equals(constructor1.getSignature().getParameterTypes())))
			.peek(removedConstructor -> {
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_REMOVED, removedConstructor.getPosition(), BreakingChangeNature.DELETION, removedConstructor));
			})
			.toList();
	}

	private List<List<Field>> getUnremovedFields(Type type1, Type type2) {
		List<Field> unremovedFields = type1.getFields().stream()
			.filter(field1 -> type2.getFields().stream().anyMatch(field2 -> field2.getName().equals(field1.getName())))
			.toList();

		List<Field> parallelFieldsFrom2 = type2.getFields().stream()
			.filter(field2 -> unremovedFields.stream().anyMatch(field1 -> field1.getName().equals(field2.getName())))
			.toList();

		List<List<Field>> result = new ArrayList<>();
		result.add(unremovedFields);
		result.add(parallelFieldsFrom2);

		return result;
	}

	private List<List<Method>> getUnremovedMethods(Type type1, Type type2) {
		List<Method> unremovedMethods = type1.getMethods().stream()
			.filter(method1 -> type2.getMethods().stream()
				.anyMatch(method2 -> method2.getSignature().getName().equals(method1.getSignature().getName()) &&
					method2.getSignature().getParameterTypes().equals(method1.getSignature().getParameterTypes())))
			.toList();

		List<Method> parallelMethodsFrom2 = type2.getMethods().stream()
			.filter(method2 -> unremovedMethods.stream()
				.anyMatch(method1 -> method1.getSignature().getName().equals(method2.getSignature().getName()) &&
					method1.getSignature().getParameterTypes().equals(method2.getSignature().getParameterTypes())))
			.toList();

		List<List<Method>> result = new ArrayList<>();
		result.add(unremovedMethods);
		result.add(parallelMethodsFrom2);

		return result;
	}

	private List<List<Constructor>> getUnremovedConstructors(Type type1, Type type2) {
		List<Constructor> unremovedConstructors = type1.getConstructors().stream()
			.filter(constructor1 -> type2.getConstructors().stream()
				.anyMatch(constructor2 -> constructor2.getSignature().getParameterTypes().equals(constructor1.getSignature().getParameterTypes())))
			.toList();

		List<Constructor> parallelConstructorsFrom2 = type2.getConstructors().stream()
			.filter(constructor2 -> unremovedConstructors.stream()
				.anyMatch(constructor1 -> constructor1.getSignature().getParameterTypes().equals(constructor2.getSignature().getParameterTypes())))
			.toList();

		List<List<Constructor>> result = new ArrayList<>();
		result.add(unremovedConstructors);
		result.add(parallelConstructorsFrom2);

		return result;
	}

	private List<Method> getAddedMethods(Type type1, Type type2) {
		return type2.getMethods().stream()
			.filter(method2 -> type1.getMethods().stream()
				.noneMatch(method1 -> method1.getSignature().getName().equals(method2.getSignature().getName()) &&
					method1.getSignature().getParameterTypes().equals(method2.getSignature().getParameterTypes())))
			.peek(addedMethod -> {
				if (type2.getDeclarationType().equals(DeclarationKind.INTERFACE) && !addedMethod.isDefault())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));

				if (type2.getDeclarationType().equals(DeclarationKind.CLASS) && addedMethod.getModifiers().contains(Modifier.ABSTRACT))
					breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));
			})
			.toList();
	}


	private void fieldComparison(Field field1, Field field2) {
		if (!field1.getModifiers().contains(Modifier.FINAL) && field2.getModifiers().contains(Modifier.FINAL))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_NOW_FINAL, field2.getPosition(), BreakingChangeNature.MUTATION, field2));

		if (!field1.getModifiers().contains(Modifier.STATIC) && field2.getModifiers().contains(Modifier.STATIC))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_NOW_STATIC, field2.getPosition(), BreakingChangeNature.MUTATION, field2));

		if (field1.getModifiers().contains(Modifier.STATIC) && !field2.getModifiers().contains(Modifier.STATIC))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_NO_LONGER_STATIC, field2.getPosition(), BreakingChangeNature.MUTATION, field2));

		if (!field1.getType().equals(field2.getType()))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_TYPE_CHANGED, field2.getPosition(), BreakingChangeNature.MUTATION, field2));

		if (field1.getVisibility().equals(AccessModifier.PUBLIC) && field2.getVisibility().equals(AccessModifier.PROTECTED))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_LESS_ACCESSIBLE, field2.getPosition(), BreakingChangeNature.MUTATION, field2));

		if (field1.getType().equals(field2.getType())) {
			List<String> referencedTypes1 = field1.getReferencedTypes();
			List<String> referencedTypes2 = field2.getReferencedTypes();

			Set<String> set1 = new HashSet<>(referencedTypes1);
			Set<String> set2 = new HashSet<>(referencedTypes2);

			if (!set1.equals(set2)) {
				breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_GENERICS_CHANGED, field2.getPosition(), BreakingChangeNature.MUTATION, field2));
			}
		}
	}

	private void methodComparison(Type type1, Type type2, Method method1, Method method2) {
		if (!method1.getModifiers().contains(Modifier.FINAL) && method2.getModifiers().contains(Modifier.FINAL))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_FINAL, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (!method1.getModifiers().contains(Modifier.STATIC) && method2.getModifiers().contains(Modifier.STATIC))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_STATIC, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (!method1.getModifiers().contains(Modifier.NATIVE) && method2.getModifiers().contains(Modifier.NATIVE))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_NATIVE, method2.getPosition(), BreakingChangeNature.MUTATION, method2));


		if (method1.getModifiers().contains(Modifier.STATIC) && !method2.getModifiers().contains(Modifier.STATIC))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NO_LONGER_STATIC, method2.getPosition(), BreakingChangeNature.MUTATION, method2));


		if (method1.getModifiers().contains(Modifier.STRICTFP) && !method2.getModifiers().contains(Modifier.STRICTFP))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NO_LONGER_STRICTFP, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (!method1.getModifiers().contains(Modifier.ABSTRACT) && method2.getModifiers().contains(Modifier.ABSTRACT))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_ABSTRACT, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (method1.getModifiers().contains(Modifier.ABSTRACT) && method2.isDefault()) // Careful
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ABSTRACT_NOW_DEFAULT, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (method1.getVisibility().equals(AccessModifier.PUBLIC) && method2.getVisibility().equals(AccessModifier.PROTECTED))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_LESS_ACCESSIBLE, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (!method1.getReturnType().equals(method2.getReturnType()))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (!method1.getParametersReferencedTypes().equals(method2.getParametersReferencedTypes()))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		List<String> additionalExceptions1 = method1.getThrownExceptions().stream()
			.filter(e -> !method2.getThrownExceptions().contains(e))
			.toList();

		List<String> additionalExceptions2 = method2.getThrownExceptions().stream()
			.filter(e -> !method1.getThrownExceptions().contains(e))
			.toList();

		if (!additionalExceptions1.isEmpty())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (!additionalExceptions2.isEmpty())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		IntStream.range(0, method1.getParametersVarargsCheck().size())
			.filter(i -> method1.getParametersVarargsCheck().get(i) != method2.getParametersVarargsCheck().get(i))
			.forEach(i -> {
				boolean isNowVarargs = !method1.getParametersVarargsCheck().get(i) && method2.getParametersVarargsCheck().get(i);
				BreakingChangeKind kind = isNowVarargs ? BreakingChangeKind.METHOD_NOW_VARARGS : BreakingChangeKind.METHOD_NO_LONGER_VARARGS;
				breakingChanges.add(new BreakingChange(kind, method2.getPosition(), BreakingChangeNature.MUTATION, method2));
			});

		// Handling the formal type parameters additions and deletions

		// In classes
		if (type1.getDeclarationType().equals(DeclarationKind.CLASS)) {
			if (method1.getFormalTypeParameters().size() > method2.getFormalTypeParameters().size() && !method2.getFormalTypeParameters().isEmpty())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, method2.getPosition(), BreakingChangeNature.DELETION, method2));

			if (method1.getFormalTypeParameters().size() < method2.getFormalTypeParameters().size() && !method1.getFormalTypeParameters().isEmpty())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, method2.getPosition(), BreakingChangeNature.ADDITION, method2));
		}

		// In interfaces
		if (type1.getDeclarationType().equals(DeclarationKind.INTERFACE)) {
			if (method1.getFormalTypeParameters().size() > method2.getFormalTypeParameters().size())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, method2.getPosition(), BreakingChangeNature.DELETION, method2));

			if (method1.getFormalTypeParameters().size() < method2.getFormalTypeParameters().size() && !method1.getFormalTypeParameters().isEmpty())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, method2.getPosition(), BreakingChangeNature.ADDITION, method2));
		}


		// Handling changes in the formal type parameters' bounds
		// The order of the FormalTypeParameters matters but the order of their bounds doesn't, this is why
		// I'm transforming the bounds into hashsets
		if (method1.getFormalTypeParameters().size() == method2.getFormalTypeParameters().size()) {
			List<List<String>> boundsV1 = method1.getFormalTypeParamsBounds();
			List<List<String>> boundsV2 = method2.getFormalTypeParamsBounds();

			for (int i = 0; i < boundsV2.size(); i++) {
				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);

				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);

				// Every bound change is breaking in interfaces, no matter the nature
				if (type1.getDeclarationType().equals(DeclarationKind.INTERFACE) && !boundsSetV1.equals(boundsSetV2))
						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

				// In classes
				if (type1.getDeclarationType().equals(DeclarationKind.CLASS)) {
					// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
					if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

					// The addition of a bound is breaking
					if (boundsSetV1.size() < boundsSetV2.size())
						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));
				}
			}
		}
	}

	private void constructorComparison(Constructor constructor1, Constructor constructor2) {
		if (constructor1.getVisibility().equals(AccessModifier.PUBLIC) && constructor2.getVisibility().equals(AccessModifier.PROTECTED))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_LESS_ACCESSIBLE, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

		if (!constructor1.getParametersReferencedTypes().equals(constructor2.getParametersReferencedTypes()))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_PARAMS_GENERICS_CHANGED, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

		if (constructor1.getFormalTypeParameters().size() == constructor2.getFormalTypeParameters().size()) {
			List<List<String>> boundsV1 = constructor1.getFormalTypeParamsBounds();
			List<List<String>> boundsV2 = constructor2.getFormalTypeParamsBounds();

			for (int i = 0; i < boundsV2.size(); i++) {
				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);

				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);

				// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
				if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

				// The addition of a bound is breaking
				if (boundsSetV1.size() < boundsSetV2.size())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));
			}
		}


		if (constructor1.getFormalTypeParameters().size() > constructor2.getFormalTypeParameters().size() && !constructor2.getFormalTypeParameters().isEmpty())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_REMOVED, constructor2.getPosition(), BreakingChangeNature.DELETION, constructor2));

		if (constructor1.getFormalTypeParameters().size() < constructor2.getFormalTypeParameters().size() && !constructor1.getFormalTypeParameters().isEmpty())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_ADDED, constructor2.getPosition(), BreakingChangeNature.ADDITION, constructor2));
	}


	private void typeComparison(Type type1, Type type2) {
		if (type1.getDeclarationType().equals(DeclarationKind.CLASS)) {
			if (!type1.getModifiers().contains(Modifier.FINAL) && type2.getModifiers().contains(Modifier.FINAL))
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_FINAL, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (!type1.getModifiers().contains(Modifier.ABSTRACT) && type2.getModifiers().contains(Modifier.ABSTRACT))
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_ABSTRACT, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (!type1.getModifiers().contains(Modifier.STATIC) && type2.getModifiers().contains(Modifier.STATIC) && type1.isNested() && type2.isNested())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (type1.getModifiers().contains(Modifier.STATIC) && !type2.getModifiers().contains(Modifier.STATIC) && type1.isNested() && type2.isNested())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (!type1.getSuperclassName().equals("java.lang.Exception") && type2.getSuperclassName().equals("java.lang.Exception"))
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, type2.getPosition(), BreakingChangeNature.MUTATION, type2));
		}

		if (type1.getVisibility().equals(AccessModifier.PUBLIC) && type2.getVisibility().equals(AccessModifier.PROTECTED))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_LESS_ACCESSIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

		if (type1.getDeclarationType().equals(DeclarationKind.CLASS)) {
			if (!type1.getSuperclassName().equals("None") && type2.getSuperclassName().equals("None"))
				breakingChanges.add(new BreakingChange(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			// Check for deleted superinterfaces
			List<String> superinterfacesV1 = type1.getSuperinterfacesNames();
			List<String> superinterfacesV2 = type2.getSuperinterfacesNames();

			for (String superinterfaceV1 : superinterfacesV1) {
				if (!superinterfacesV2.contains(superinterfaceV1))
					breakingChanges.add(new BreakingChange(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));
			}
		}

		if (type1.getDeclarationType().equals(DeclarationKind.INTERFACE) && !type1.getSuperinterfacesNames().equals(type2.getSuperinterfacesNames()))
				breakingChanges.add(new BreakingChange(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

		if (!type1.getDeclarationType().equals(type2.getDeclarationType()))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_TYPE_CHANGED, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

		if (type1.getFormalTypeParameters().size() == type2.getFormalTypeParameters().size()) {
			List<List<String>> boundsV1 = type1.getFormalTypeParamsBounds();
			List<List<String>> boundsV2 = type2.getFormalTypeParamsBounds();

			for (int i = 0; i < boundsV2.size(); i++) {
				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);

				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);

				// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
				if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

				if (boundsSetV1.size() < boundsSetV2.size() && !boundsSetV1.isEmpty())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, type2.getPosition(), BreakingChangeNature.ADDITION, type2));
			}
		}

		if (type1.getFormalTypeParameters().size() > type2.getFormalTypeParameters().size())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, type2.getPosition(), BreakingChangeNature.DELETION, type2));

		if (type1.getFormalTypeParameters().size() < type2.getFormalTypeParameters().size() && !type1.getFormalTypeParameters().isEmpty())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, type2.getPosition(), BreakingChangeNature.ADDITION, type2));
	}

	private void detectingBreakingChanges() {
		checkingForRemovedTypes();
		List<List<Type>> commonTypes = getUnremovedTypes();
		List<Type> commonTypesInV1 = commonTypes.get(0);
		List<Type> commonTypesInV2 = commonTypes.get(1);

		IntStream.range(0, commonTypesInV1.size())
			.forEach(i -> {
				Type typeV1 = commonTypesInV1.get(i);
				Type typeV2 = commonTypesInV2.get(i);

				typeComparison(typeV1, typeV2);

				checkingForRemovedFields(typeV1, typeV2);
				checkingForRemovedMethods(typeV1, typeV2);
				checkingForRemovedConstructors(typeV1, typeV2);

				List<List<Method>> remainingMethods = getUnremovedMethods(typeV1, typeV2);
				List<List<Field>> remainingFields = getUnremovedFields(typeV1, typeV2);
				List<List<Constructor>> remainingConstructors = getUnremovedConstructors(commonTypes.get(0).get(i), commonTypes.get(1).get(i));

				getAddedMethods(typeV1, typeV2);

				IntStream.range(0, remainingMethods.get(0).size())
					.forEach(j -> methodComparison(typeV1, typeV2, remainingMethods.get(0).get(j), remainingMethods.get(1).get(j)));

				IntStream.range(0, remainingConstructors.get(0).size())
					.forEach(j -> constructorComparison(remainingConstructors.get(0).get(j), remainingConstructors.get(1).get(j)));

				IntStream.range(0, remainingFields.get(0).size())
					.forEach(j -> fieldComparison(remainingFields.get(0).get(j), remainingFields.get(1).get(j)));
			});

		breakingChangesPopulated = true;
	}


	/**
	 * Retrieves the list of all the breaking changes detected between the two API versions.
	 *
	 * @return List of all the breaking changes
	 */
	public List<BreakingChange> getBreakingChanges() {
		if (!breakingChangesPopulated)
			detectingBreakingChanges();

		return breakingChanges;
	}

	/**
	 * Generates a csv report for the detected breaking changes. This report includes the kind, type name,
	 * <p>
	 * position, associated element, and nature of each detected BC.
	 */
	public void breakingChangesReport() {
		List<BreakingChange> breakingChanges = getBreakingChanges();

		try (FileWriter writer = new FileWriter("breaking_changes_report.csv")) {
			writer.write("Kind,Type Declaration,Element,Nature,Position\n");

			for (BreakingChange breakingChange : breakingChanges) {
				String kind = breakingChange.kind().toString();
				String element = breakingChange.impactedElement().getName();
				String nature = breakingChange.nature().toString();
				String position = breakingChange.position();

				writer.write(kind + "," + element + "," + nature + "," + position + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a string representation of the breaking changes list.
	 *
	 * @return A formatted string containing all the breaking changes and their info.
	 */
	@Override
	public String toString() {
		if (!breakingChangesPopulated)
			detectingBreakingChanges();

		String result = "";
		for (BreakingChange breakingChange : breakingChanges) {
			result = result + breakingChange.toString() + "\n";
			result = result + "    =========================\n\n";
		}

		return result.toString();
	}
}
