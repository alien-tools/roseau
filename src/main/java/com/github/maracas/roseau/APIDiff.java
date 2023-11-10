package com.github.maracas.roseau;

import com.github.maracas.roseau.changes.BreakingChange;
import com.github.maracas.roseau.changes.BreakingChangeKind;
import com.github.maracas.roseau.changes.BreakingChangeNature;
import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.ClassDecl;
import com.github.maracas.roseau.model.ConstructorDecl;
import com.github.maracas.roseau.model.FieldDecl;
import com.github.maracas.roseau.model.MethodDecl;
import com.github.maracas.roseau.model.Modifier;
import com.github.maracas.roseau.model.TypeDecl;
import com.github.maracas.roseau.model.TypeReference;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

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

	private List<TypeDecl> checkingForRemovedTypes() {
		return v1.types().stream()
			.filter(type -> v2.types().stream().noneMatch(t -> t.getQualifiedName().equals(type.getQualifiedName())))
			.peek(removedType -> {
				if (removedType.isClass())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_REMOVED, removedType.getPosition(), BreakingChangeNature.DELETION, removedType));

				if (removedType.isInterface())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.INTERFACE_REMOVED, removedType.getPosition(), BreakingChangeNature.DELETION, removedType));
			})
			.toList();
	}

	private List<List<TypeDecl>> getUnremovedTypes() {
		List<TypeDecl> unremovedTypes1 = v1.types().stream()
			.filter(type -> v2.types().stream().anyMatch(t -> t.getQualifiedName().equals(type.getQualifiedName())))
			.toList();

		List<TypeDecl> typesInParallelFrom2 = v2.types().stream()
			.filter(type -> unremovedTypes1.stream().anyMatch(t -> t.getQualifiedName().equals(type.getQualifiedName())))
			.toList();

		List<List<TypeDecl>> result = new ArrayList<>();
		result.add(unremovedTypes1);
		result.add(typesInParallelFrom2);

		return result;
	}

	private void checkRemovedFields(TypeDecl type1, TypeDecl type2) {
		type1.getFields().stream()
			.filter(field1 -> type2.getFields().stream().noneMatch(field2 -> field2.getQualifiedName().equals(field1.getQualifiedName())))
			.forEach(removedField ->
				breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_REMOVED, removedField.getPosition(), BreakingChangeNature.DELETION, removedField))
			);
	}

	private void checkRemovedMethods(TypeDecl type1, TypeDecl type2) {
		type1.getMethods().stream()
			.filter(method1 -> type2.getAllMethods().stream().noneMatch(method2 -> method2.hasSameSignature(method1)))
			.forEach(removedMethod ->
					breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_REMOVED, removedMethod.getPosition(), BreakingChangeNature.DELETION, removedMethod))
			);
	}

	private void checkRemovedConstructors(ClassDecl type1, ClassDecl type2) {
		type1.getConstructors().stream()
			.filter(cons1 -> type2.getConstructors().stream().noneMatch(cons2 -> cons2.hasSameSignature(cons1)))
			.forEach(removedCons ->
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_REMOVED, removedCons.getPosition(), BreakingChangeNature.DELETION, removedCons))
			);
	}

	private List<List<FieldDecl>> getUnremovedFields(TypeDecl type1, TypeDecl type2) {
		List<FieldDecl> unremovedFields = type1.getFields().stream()
			.filter(field1 -> type2.getFields().stream().anyMatch(field2 -> field2.getQualifiedName().equals(field1.getQualifiedName())))
			.toList();

		List<FieldDecl> parallelFieldsFrom2 = type2.getFields().stream()
			.filter(field2 -> unremovedFields.stream().anyMatch(field1 -> field1.getQualifiedName().equals(field2.getQualifiedName())))
			.toList();

		List<List<FieldDecl>> result = new ArrayList<>();
		result.add(unremovedFields);
		result.add(parallelFieldsFrom2);

		return result;
	}

	private List<List<MethodDecl>> getUnremovedMethods(TypeDecl type1, TypeDecl type2) {
		List<MethodDecl> unremovedMethods = type1.getMethods().stream()
			.filter(method1 -> type2.getMethods().stream()
				.anyMatch(method2 -> method2.hasSameSignature(method1)))
			.toList();

		List<MethodDecl> parallelMethodsFrom2 = type2.getMethods().stream()
			.filter(method2 -> unremovedMethods.stream()
				.anyMatch(method1 -> method1.hasSameSignature(method2)))
			.toList();

		List<List<MethodDecl>> result = new ArrayList<>();
		result.add(unremovedMethods);
		result.add(parallelMethodsFrom2);

		return result;
	}

	private List<List<ConstructorDecl>> getUnremovedConstructors(ClassDecl type1, ClassDecl type2) {
		List<ConstructorDecl> unremovedConstructors = type1.getConstructors().stream()
			.filter(constructor1 -> type2.getConstructors().stream()
				.anyMatch(constructor2 -> constructor2.hasSameSignature(constructor1)))
			.toList();

		List<ConstructorDecl> parallelConstructorsFrom2 = type2.getConstructors().stream()
			.filter(constructor2 -> unremovedConstructors.stream()
				.anyMatch(constructor1 -> constructor1.hasSameSignature(constructor2)))
			.toList();

		List<List<ConstructorDecl>> result = new ArrayList<>();
		result.add(unremovedConstructors);
		result.add(parallelConstructorsFrom2);

		return result;
	}

	private List<MethodDecl> getAddedMethods(TypeDecl type1, TypeDecl type2) {
		return type2.getMethods().stream()
			.filter(method2 -> type1.getMethods().stream()
				.noneMatch(method1 -> method1.hasSameSignature(method2)))
			.peek(addedMethod -> {
				if (type2.isInterface() && !addedMethod.isDefault())
					breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));

				if (type2.isClass() && addedMethod.getModifiers().contains(Modifier.ABSTRACT))
					breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));
			})
			.toList();
	}

	class X<AA> {
		List<AA> ff;
	}

	private void fieldComparison(FieldDecl field1, FieldDecl field2) {
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
//			List<String> referencedTypes1 = field1.getReferencedTypes();
//			List<String> referencedTypes2 = field2.getReferencedTypes();
//
//			Set<String> set1 = new HashSet<>(referencedTypes1);
//			Set<String> set2 = new HashSet<>(referencedTypes2);
//
//			if (!set1.equals(set2)) {
//				breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_GENERICS_CHANGED, field2.getPosition(), BreakingChangeNature.MUTATION, field2));
//			}
		}
	}

	private void methodComparison(TypeDecl type1, TypeDecl type2, MethodDecl method1, MethodDecl method2) {
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

//		if (!method1.getParametersReferencedTypes().equals(method2.getParametersReferencedTypes()))
//			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		List<TypeReference> additionalExceptions1 = method1.getThrownExceptions().stream()
			.filter(e -> e.getActualType().isCheckedException())
			.filter(e -> !method2.getThrownExceptions().contains(e))
			.toList();

		List<TypeReference> additionalExceptions2 = method2.getThrownExceptions().stream()
			.filter(e -> !method1.getThrownExceptions().contains(e))
			.toList();

		if (!additionalExceptions1.isEmpty())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, method2.getPosition(), BreakingChangeNature.MUTATION, method2));

		if (!additionalExceptions2.isEmpty())
			breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, method2.getPosition(), BreakingChangeNature.MUTATION, method2));



//		IntStream.range(0, method1.getParametersVarargsCheck().size())
//			.filter(i -> method1.getParametersVarargsCheck().get(i) != method2.getParametersVarargsCheck().get(i))
//			.forEach(i -> {
//				boolean isNowVarargs = !method1.getParametersVarargsCheck().get(i) && method2.getParametersVarargsCheck().get(i);
//				BreakingChangeKind kind = isNowVarargs ? BreakingChangeKind.METHOD_NOW_VARARGS : BreakingChangeKind.METHOD_NO_LONGER_VARARGS;
//				breakingChanges.add(new BreakingChange(kind, method2.getPosition(), BreakingChangeNature.MUTATION, method2));
//			});

		// Handling the formal type parameters additions and deletions

		// In classes
		if (type1.isClass()) {
			if (method1.getFormalTypeParameters().size() > method2.getFormalTypeParameters().size() && !method2.getFormalTypeParameters().isEmpty())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, method2.getPosition(), BreakingChangeNature.DELETION, method2));

			if (method1.getFormalTypeParameters().size() < method2.getFormalTypeParameters().size() && !method1.getFormalTypeParameters().isEmpty())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, method2.getPosition(), BreakingChangeNature.ADDITION, method2));
		}

		// In interfaces
		if (type1.isInterface()) {
			if (method1.getFormalTypeParameters().size() > method2.getFormalTypeParameters().size())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, method2.getPosition(), BreakingChangeNature.DELETION, method2));

			if (method1.getFormalTypeParameters().size() < method2.getFormalTypeParameters().size() && !method1.getFormalTypeParameters().isEmpty())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, method2.getPosition(), BreakingChangeNature.ADDITION, method2));
		}


		// Handling changes in the formal type parameters' bounds
		// The order of the FormalTypeParameters matters but the order of their bounds doesn't, this is why
		// I'm transforming the bounds into hashsets
//		if (method1.getFormalTypeParameters().size() == method2.getFormalTypeParameters().size()) {
//			List<List<String>> boundsV1 = method1.getFormalTypeParamsBounds();
//			List<List<String>> boundsV2 = method2.getFormalTypeParamsBounds();
//
//			for (int i = 0; i < boundsV2.size(); i++) {
//				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
//				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);
//
//				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
//				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);
//
//				// Every bound change is breaking in interfaces, no matter the nature
//				if (type1.getDeclarationType().equals(TypeKind.INTERFACE) && !boundsSetV1.equals(boundsSetV2))
//						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));
//
//				// In classes
//				if (type1.getDeclarationType().equals(TypeKind.CLASS)) {
//					// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
//					if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
//						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));
//
//					// The addition of a bound is breaking
//					if (boundsSetV1.size() < boundsSetV2.size())
//						breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getPosition(), BreakingChangeNature.MUTATION, method2));
//				}
//			}
//		}
	}

	private void constructorComparison(ConstructorDecl constructor1, ConstructorDecl constructor2) {
		if (constructor1.getVisibility().equals(AccessModifier.PUBLIC) && constructor2.getVisibility().equals(AccessModifier.PROTECTED))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_LESS_ACCESSIBLE, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

//		if (!constructor1.getParametersReferencedTypes().equals(constructor2.getParametersReferencedTypes()))
//			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_PARAMS_GENERICS_CHANGED, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

//		if (constructor1.getFormalTypeParameters().size() == constructor2.getFormalTypeParameters().size()) {
//			List<List<String>> boundsV1 = constructor1.getFormalTypeParamsBounds();
//			List<List<String>> boundsV2 = constructor2.getFormalTypeParamsBounds();
//
//			for (int i = 0; i < boundsV2.size(); i++) {
//				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
//				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);
//
//				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
//				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);
//
//				// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
//				if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
//					breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));
//
//				// The addition of a bound is breaking
//				if (boundsSetV1.size() < boundsSetV2.size())
//					breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED, constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));
//			}
//		}


//		if (constructor1.getFormalTypeParameters().size() > constructor2.getFormalTypeParameters().size() && !constructor2.getFormalTypeParameters().isEmpty())
//			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_REMOVED, constructor2.getPosition(), BreakingChangeNature.DELETION, constructor2));
//
//		if (constructor1.getFormalTypeParameters().size() < constructor2.getFormalTypeParameters().size() && !constructor1.getFormalTypeParameters().isEmpty())
//			breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_ADDED, constructor2.getPosition(), BreakingChangeNature.ADDITION, constructor2));
	}


	private void compareTypes(TypeDecl type1, TypeDecl type2) {
		if (type1.isClass()) {
			if (!type1.getModifiers().contains(Modifier.FINAL) && type2.getModifiers().contains(Modifier.FINAL))
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_FINAL, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (!type1.getModifiers().contains(Modifier.ABSTRACT) && type2.getModifiers().contains(Modifier.ABSTRACT))
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_ABSTRACT, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (!type1.getModifiers().contains(Modifier.STATIC) && type2.getModifiers().contains(Modifier.STATIC) && type1.isNested() && type2.isNested())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (type1.getModifiers().contains(Modifier.STATIC) && !type2.getModifiers().contains(Modifier.STATIC) && type1.isNested() && type2.isNested())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

			if (!type1.isCheckedException() && type2.isCheckedException())
				breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, type2.getPosition(), BreakingChangeNature.MUTATION, type2));
		}

		if (type1.getVisibility().equals(AccessModifier.PUBLIC) && type2.getVisibility().equals(AccessModifier.PROTECTED))
			breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_LESS_ACCESSIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

//		if (type1.isClass()) {
//			if (type1.getSuperclass() != null && type2.getSuperclass() == null)
//				breakingChanges.add(new BreakingChange(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));
//
//			// Check for deleted super-interfaces
//			if (type1.getSuperInterfaces().stream()
//				.anyMatch(intf1 -> type2.getSuperInterfaces().stream().noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))))
//					breakingChanges.add(new BreakingChange(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));
//		}
//
//		if (type1.getDeclarationType().equals(TypeKind.INTERFACE) && type1.getSuperInterfaces().stream()
//			.anyMatch(intf1 -> type2.getSuperInterfaces().stream().noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))))
//				breakingChanges.add(new BreakingChange(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getPosition(), BreakingChangeNature.MUTATION, type2));
//
//		if (!type1.getDeclarationType().equals(type2.getDeclarationType()))
//			breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_TYPE_CHANGED, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

//		if (type1.getFormalTypeParameters().size() == type2.getFormalTypeParameters().size()) {
//			List<List<String>> boundsV1 = type1.getFormalTypeParamsBounds();
//			List<List<String>> boundsV2 = type2.getFormalTypeParamsBounds();
//
//			for (int i = 0; i < boundsV2.size(); i++) {
//				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
//				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);
//
//				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
//				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);
//
//				// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
//				if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
//					breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, type2.getPosition(), BreakingChangeNature.MUTATION, type2));
//
//				if (boundsSetV1.size() < boundsSetV2.size() && !boundsSetV1.isEmpty())
//					breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, type2.getPosition(), BreakingChangeNature.ADDITION, type2));
//			}
//		}

//		if (type1.getFormalTypeParameters().size() > type2.getFormalTypeParameters().size())
//			breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, type2.getPosition(), BreakingChangeNature.DELETION, type2));
//
//		if (type1.getFormalTypeParameters().size() < type2.getFormalTypeParameters().size() && !type1.getFormalTypeParameters().isEmpty())
//			breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, type2.getPosition(), BreakingChangeNature.ADDITION, type2));
	}

	private void detectingBreakingChanges() {
		checkingForRemovedTypes();
		List<List<TypeDecl>> commonTypes = getUnremovedTypes();
		List<TypeDecl> commonTypesInV1 = commonTypes.get(0);
		List<TypeDecl> commonTypesInV2 = commonTypes.get(1);

		IntStream.range(0, commonTypesInV1.size())
			.forEach(i -> {
				TypeDecl typeV1 = commonTypesInV1.get(i);
				TypeDecl typeV2 = commonTypesInV2.get(i);

				compareTypes(typeV1, typeV2);
				checkRemovedFields(typeV1, typeV2);
				checkRemovedMethods(typeV1, typeV2);

				if (typeV1 instanceof ClassDecl clsV1 && typeV2 instanceof ClassDecl clsV2) {
					checkRemovedConstructors(clsV1, clsV2);
				}

				List<List<MethodDecl>> remainingMethods = getUnremovedMethods(typeV1, typeV2);
				List<List<FieldDecl>> remainingFields = getUnremovedFields(typeV1, typeV2);

				List<List<ConstructorDecl>> remainingConstructors = getUnremovedConstructors((ClassDecl) commonTypes.get(0).get(i), (ClassDecl) commonTypes.get(1).get(i));

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
				String element = breakingChange.impactedSymbol().getQualifiedName();
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
