
package com.github.maracas.roseau;

import com.github.maracas.roseau.changes.*;
import com.github.maracas.roseau.model.*;

import java.util.List;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * This class represents Roseau's comparison tool for detecting breaking changes between two API versions.
 */

public class APIDiff {

    /** The first version of the API to be compared. */
    private API v1;

    /** The second version of the API to be compared. */
    private API v2;
    /** List of all the breaking changes identified in the comparison. */
    private List<BreakingChange> breakingChanges;

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





    private List<TypeDeclaration> checkingForRemovedTypes() {

        return v1.getAllTheTypes().stream()
                .filter(type -> v2.getAllTheTypes().stream()
                        .noneMatch(t -> t.getName().equals(type.getName())))
                .peek(removedType -> {
                    //System.out.println("Type removed: " + removedType.getName());
                    if (removedType.getTypeType().equals(TypeType.CLASS)) {

                        breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_REMOVED, removedType, removedType.getPosition(), BreakingChangeNature.DELETION, removedType));

                    }
                    if (removedType.getTypeType().equals(TypeType.INTERFACE)) {
                        breakingChanges.add(new BreakingChange(BreakingChangeKind.INTERFACE_REMOVED, removedType, removedType.getPosition(), BreakingChangeNature.DELETION, removedType));

                    }

                })
                .toList();
    }

    private List<List<TypeDeclaration>> getUnremovedTypes() {

        List<TypeDeclaration> unremovedTypes1 = v1.getAllTheTypes().stream()
                .filter(type -> v2.getAllTheTypes().stream()
                        .anyMatch(t -> t.getName().equals(type.getName())))
                //.peek(removedType -> System.out.println("Type remaining: " + removedType.getName()))

                .toList();


        List<TypeDeclaration> typesInParallelFrom2 = v2.getAllTheTypes().stream()
                .filter(type -> unremovedTypes1.stream()
                        .anyMatch(t -> t.getName().equals(type.getName())))
                //.peek(type -> System.out.println("Types from v2 : " + type.getName()))
                .toList();

        List<List<TypeDeclaration>> result = new ArrayList<>();
        result.add(unremovedTypes1);
        result.add(typesInParallelFrom2);

        return result;

    }


    private List<FieldDeclaration> checkingForRemovedFields(TypeDeclaration type1, TypeDeclaration type2) {
        return type1.getFields().stream()
                .filter(field1 -> type2.getFields().stream()
                        .noneMatch(field2 -> field2.getName().equals(field1.getName())))
                .peek(removedField -> {
                    //System.out.println("Field removed: " + removedField.getName());
                    breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_REMOVED, removedField.getType(), removedField.getPosition(), BreakingChangeNature.DELETION, removedField));

                })
                .toList();
    }


    private List<MethodDeclaration> checkingForRemovedMethods(TypeDeclaration type1, TypeDeclaration type2) {
        return type1.getMethods().stream()
                .filter(method2 -> type2.getMethods().stream()
                        .noneMatch(method1 -> method1.getSignature().getName().equals(method2.getSignature().getName()) && method1.getSignature().getParameterTypes().equals(method2.getSignature().getParameterTypes())))
                .peek(removedMethod -> {
                    //System.out.println("Method removed: " + removedMethod.getName());
                    breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_REMOVED, removedMethod.getType(), removedMethod.getPosition(), BreakingChangeNature.DELETION, removedMethod));

                })
                .toList();

    }



    private List<ConstructorDeclaration> checkingForRemovedConstructors(TypeDeclaration type1, TypeDeclaration type2) {
        return type1.getConstructors().stream()
                .filter(constructor1 -> type2.getConstructors().stream()
                        .noneMatch(constructor2 -> constructor2.getSignature().getName().equals(constructor1.getSignature().getName()) && constructor2.getSignature().getParameterTypes().equals(constructor1.getSignature().getParameterTypes())))
                .peek(removedConstructor -> {
                    //System.out.println("Constructor removed: " + removedConstructor.getName());
                    breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_REMOVED, removedConstructor.getType(), removedConstructor.getPosition(), BreakingChangeNature.DELETION, removedConstructor));


                })
                .toList();
    }



    private List<List<FieldDeclaration>> getUnremovedFields(TypeDeclaration type1, TypeDeclaration type2) {
        List<FieldDeclaration> unremovedFields = type1.getFields().stream()
                .filter( field1 -> type2.getFields().stream()
                        .anyMatch(field2 -> field2.getName().equals(field1.getName()) ))
                //.peek(remainingField -> System.out.println("Field Left: " + remainingField.getName()))
                .toList();

        List<FieldDeclaration> parallelFieldsFrom2 = type2.getFields().stream()
                .filter(field2 -> unremovedFields.stream()
                        .anyMatch(field1 -> field1.getName().equals(field2.getName()) ))
                //.peek(parallelField -> System.out.println("Parallel Field from type2: " + parallelField.getName()))
                .toList();

        List<List<FieldDeclaration>> result = new ArrayList<>();
        result.add(unremovedFields);
        result.add(parallelFieldsFrom2);

        return result;
    }


    private List<List<MethodDeclaration>> getUnremovedMethods(TypeDeclaration type1, TypeDeclaration type2) {
        List<MethodDeclaration> unremovedMethods = type1.getMethods().stream()
                .filter(method1 -> type2.getMethods().stream()
                        .anyMatch(method2 -> method2.getSignature().getName().equals(method1.getSignature().getName()) &&
                                method2.getSignature().getParameterTypes().equals(method1.getSignature().getParameterTypes())))
                //.peek(remainingMethod -> System.out.println("Method Left: " + remainingMethod.getName()))
                .toList();

        List<MethodDeclaration> parallelMethodsFrom2 = type2.getMethods().stream()
                .filter(method2 -> unremovedMethods.stream()
                        .anyMatch(method1 -> method1.getSignature().getName().equals(method2.getSignature().getName()) &&
                                method1.getSignature().getParameterTypes().equals(method2.getSignature().getParameterTypes())))
                //.peek(parallelMethod -> System.out.println("Parallel Method from type2: " + parallelMethod.getName()))
                .toList();

        List<List<MethodDeclaration>> result = new ArrayList<>();
        result.add(unremovedMethods);
        result.add(parallelMethodsFrom2);

        return result;
    }



    private List<List<ConstructorDeclaration>> getUnremovedConstructors(TypeDeclaration type1, TypeDeclaration type2) {
        List<ConstructorDeclaration> unremovedConstructors = type1.getConstructors().stream()
                .filter(constructor1 -> type2.getConstructors().stream()
                        .anyMatch(constructor2 -> constructor2.getSignature().getParameterTypes().equals(constructor1.getSignature().getParameterTypes())))
                //.peek(remainingConstructor -> System.out.println("Constructor Left: " + remainingConstructor.getName()))
                .toList();

        List<ConstructorDeclaration> parallelConstructorsFrom2 = type2.getConstructors().stream()
                .filter(constructor2 -> unremovedConstructors.stream()
                        .anyMatch(constructor1 -> constructor1.getSignature().getParameterTypes().equals(constructor2.getSignature().getParameterTypes())))
                //.peek(parallelConstructor -> System.out.println("Parallel Constructor from type2: " + parallelConstructor.getName()))
                .toList();

        List<List<ConstructorDeclaration>> result = new ArrayList<>();
        result.add(unremovedConstructors);
        result.add(parallelConstructorsFrom2);

        return result;
    }


    private List<MethodDeclaration> getAddedMethods(TypeDeclaration type1, TypeDeclaration type2) {
        return type2.getMethods().stream()
                .filter(method2 -> type1.getMethods().stream()
                        .noneMatch(method1 -> method1.getSignature().getName().equals(method2.getSignature().getName()) &&
                                method1.getSignature().getParameterTypes().equals(method2.getSignature().getParameterTypes())))
                .peek(addedMethod -> {

                    if (type2.getTypeType().equals(TypeType.INTERFACE)) {
                        breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, addedMethod.getType(), addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));


                    }
                    if (type2.getTypeType().equals(TypeType.CLASS) && addedMethod.getModifiers().contains(NonAccessModifiers.ABSTRACT)) {
                        breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, addedMethod.getType(), addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));


                    }
                    if (type1.getSuperclass() != null && type2.getSuperclass() != null ){

                        List<MethodDeclaration> superclassMethodsInV1 = type1.getSuperclass().getMethods();
                        List<MethodDeclaration> superclassMethodsInV2 = type2.getSuperclass().getMethods();

                        MethodDeclaration superMethodInV1 = superclassMethodsInV1.stream()
                                .filter(method -> method.getSignature().getName().equals(addedMethod.getSignature().getName())
                                        && method.getSignature().getParameterTypes().equals(addedMethod.getSignature().getParameterTypes()))
                                .findFirst()
                                .orElse(null);

                        MethodDeclaration superMethodInV2 = superclassMethodsInV2.stream()
                                .filter(method -> method.getSignature().getName().equals(addedMethod.getSignature().getName())
                                        && method.getSignature().getParameterTypes().equals(addedMethod.getSignature().getParameterTypes()))
                                .findFirst()
                                .orElse(null);

                        if (superMethodInV2 != null && superMethodInV1 != null){   // if the method actually overrides another
                            if (addedMethod.getModifiers().contains(NonAccessModifiers.STATIC) && !superMethodInV2.getModifiers().contains(NonAccessModifiers.STATIC)) {
                                breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_IS_STATIC_AND_OVERRIDES_NOT_STATIC, addedMethod.getType(), addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));


                            }
                            if (!addedMethod.getModifiers().contains(NonAccessModifiers.STATIC) && superMethodInV2.getModifiers().contains(NonAccessModifiers.STATIC)) {
                                breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_IS_NOT_STATIC_AND_OVERRIDES_STATIC, addedMethod.getType(), addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));


                            }
                            if (superMethodInV2.getVisibility().equals(AccessModifier.PUBLIC) && addedMethod.getVisibility().equals(AccessModifier.PROTECTED)) {
                                breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_LESS_ACCESSIBLE_THAN_IN_SUPERCLASS, addedMethod.getType(), addedMethod.getPosition(), BreakingChangeNature.ADDITION, addedMethod));


                            }
                        }

                    }
                })
                .toList();

    }

    private List<FieldDeclaration> getAddedFields(TypeDeclaration type1, TypeDeclaration type2) {
        return type2.getFields().stream()
                .filter(field2 -> type1.getFields().stream()
                        .noneMatch(field1 -> field1.getName().equals(field2.getName())))
                .peek(addedField -> {

                    if (type1.getSuperclass() != null && type2.getSuperclass() != null) {

                        List<FieldDeclaration> superclassFieldsInV1 = type1.getSuperclass().getFields();
                        List<FieldDeclaration> superclassFieldsInV2 = type2.getSuperclass().getFields();

                        FieldDeclaration superFieldInV1 = superclassFieldsInV1.stream()
                                .filter(field -> field.getName().equals(addedField.getName()))
                                .findFirst()
                                .orElse(null);

                        FieldDeclaration superFieldInV2 = superclassFieldsInV2.stream()
                                .filter(field -> field.getName().equals(addedField.getName()))
                                .findFirst()
                                .orElse(null);

                        if (superFieldInV2 != null && superFieldInV1 != null) { // if the field exists in both superclasses
                            if (addedField.getModifiers().contains(NonAccessModifiers.STATIC) && !superFieldInV2.getModifiers().contains(NonAccessModifiers.STATIC)) {
                                breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_STATIC_AND_OVERRIDES_NON_STATIC, addedField.getType(), addedField.getPosition(), BreakingChangeNature.ADDITION, addedField));


                            }
                            if (!addedField.getModifiers().contains(NonAccessModifiers.STATIC) && superFieldInV2.getModifiers().contains(NonAccessModifiers.STATIC)) {
                                breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_NON_STATIC_AND_OVERRIDES_STATIC, addedField.getType(), addedField.getPosition(), BreakingChangeNature.ADDITION, addedField));

                            }
                            if (superFieldInV2.getVisibility().equals(AccessModifier.PUBLIC) && addedField.getVisibility().equals(AccessModifier.PROTECTED)) {
                                breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_LESS_ACCESSIBLE_THAN_IN_SUPERCLASS, addedField.getType(), addedField.getPosition(), BreakingChangeNature.ADDITION, addedField));

                            }
                        }
                    }


                })
                .toList();
    }



    private void fieldComparison(FieldDeclaration field1, FieldDeclaration field2) {
        if (!field1.getModifiers().contains(NonAccessModifiers.FINAL) && field2.getModifiers().contains(NonAccessModifiers.FINAL)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_NOW_FINAL, field2.getType(), field2.getPosition(), BreakingChangeNature.MUTATION, field2));

        }

        if (!field1.getModifiers().contains(NonAccessModifiers.STATIC) && field2.getModifiers().contains(NonAccessModifiers.STATIC)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_NOW_STATIC, field2.getType(), field2.getPosition(), BreakingChangeNature.MUTATION, field2));

        }

        if (field1.getModifiers().contains(NonAccessModifiers.STATIC) && !field2.getModifiers().contains(NonAccessModifiers.STATIC)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_NO_LONGER_STATIC, field2.getType(), field2.getPosition(), BreakingChangeNature.MUTATION, field2));

        }

        if (!field1.getDataType().equals(field2.getDataType())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_TYPE_CHANGED, field2.getType(), field2.getPosition(), BreakingChangeNature.MUTATION, field2));

        }


        if (field1.getVisibility().equals(AccessModifier.PUBLIC) && field2.getVisibility().equals(AccessModifier.PROTECTED)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_LESS_ACCESSIBLE, field2.getType(), field2.getPosition(), BreakingChangeNature.MUTATION, field2));

        }

        if (field1.getDataType().equals(field2.getDataType()) && !field1.getReferencedTypes().equals(field2.getReferencedTypes())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FIELD_GENERICS_CHANGED, field2.getType(), field2.getPosition(), BreakingChangeNature.MUTATION, field2));

        }

    }



    private void methodComparison(MethodDeclaration method1, MethodDeclaration method2) {
        if (!method1.getModifiers().contains(NonAccessModifiers.FINAL) && method2.getModifiers().contains(NonAccessModifiers.FINAL)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_FINAL, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (!method1.getModifiers().contains(NonAccessModifiers.STATIC) && method2.getModifiers().contains(NonAccessModifiers.STATIC)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_STATIC, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (!method1.getModifiers().contains(NonAccessModifiers.NATIVE) && method2.getModifiers().contains(NonAccessModifiers.NATIVE)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_NATIVE, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }


        if (method1.getModifiers().contains(NonAccessModifiers.STATIC) && !method2.getModifiers().contains(NonAccessModifiers.STATIC)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NO_LONGER_STATIC, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }


        if (method1.getModifiers().contains(NonAccessModifiers.STRICTFP) && !method2.getModifiers().contains(NonAccessModifiers.STRICTFP)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NO_LONGER_STRICTFP, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (!method1.getModifiers().contains(NonAccessModifiers.ABSTRACT) && method2.getModifiers().contains(NonAccessModifiers.ABSTRACT)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_ABSTRACT, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (method1.getModifiers().contains(NonAccessModifiers.ABSTRACT) && method2.isDefault()) { /// Careful
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_ABSTRACT_NOW_DEFAULT, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (method1.getVisibility().equals(AccessModifier.PUBLIC) && method2.getVisibility().equals(AccessModifier.PROTECTED)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_LESS_ACCESSIBLE, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (!method1.getReturnType().equals(method2.getReturnType())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (method1.getReturnType().equals(method2.getReturnType()) && !method1.getReferencedTypes().equals(method2.getReferencedTypes())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_RETURN_TYPE_GENERICS_CHANGED, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (!method1.getParametersReferencedTypes().equals(method2.getParametersReferencedTypes())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        List<String> additionalExceptions1 = method1.getExceptions().stream()
                .filter(e -> !method2.getExceptions().contains(e))
                .toList();

        List<String> additionalExceptions2 = method2.getExceptions().stream()
                .filter(e -> !method1.getExceptions().contains(e))
                .toList();


        if (!additionalExceptions1.isEmpty()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }

        if (!additionalExceptions2.isEmpty()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));;

        }

        IntStream.range(0, method1.getParametersVarargsCheck().size())
                .filter(i -> method1.getParametersVarargsCheck().get(i) != method2.getParametersVarargsCheck().get(i))
                .forEach(i -> {
                    boolean isNowVarargs = !method1.getParametersVarargsCheck().get(i) && method2.getParametersVarargsCheck().get(i);
                    BreakingChangeKind kind = isNowVarargs ? BreakingChangeKind.METHOD_NOW_VARARGS : BreakingChangeKind.METHOD_NO_LONGER_VARARGS;
                    breakingChanges.add(new BreakingChange(kind, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

                });



        if (method1.getFormalTypeParameters().size() > method2.getFormalTypeParameters().size()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }
        if (method1.getFormalTypeParameters().size() < method2.getFormalTypeParameters().size()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, method2.getType(), method2.getPosition(), BreakingChangeNature.MUTATION, method2));

        }




    }

    private void constructorComparison(ConstructorDeclaration constructor1, ConstructorDeclaration constructor2) {
        if (constructor1.getVisibility().equals(AccessModifier.PUBLIC) && constructor2.getVisibility().equals(AccessModifier.PROTECTED)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_LESS_ACCESSIBLE, constructor2.getType(), constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

        }

        if (!constructor1.getParametersReferencedTypes().equals(constructor2.getParametersReferencedTypes())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_PARAMS_GENERICS_CHANGED, constructor2.getType(), constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

        }
        if (!constructor1.getReferencedTypes().equals(constructor2.getReferencedTypes())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_GENERICS_CHANGED, constructor2.getType(), constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

        }
        if (constructor1.getFormalTypeParameters().size() > constructor2.getFormalTypeParameters().size()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_REMOVED, constructor2.getType(), constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

        }
        if (constructor1.getFormalTypeParameters().size() < constructor2.getFormalTypeParameters().size()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_ADDED, constructor2.getType(), constructor2.getPosition(), BreakingChangeNature.MUTATION, constructor2));

        }

    }


    private void typeComparison(TypeDeclaration type1, TypeDeclaration type2) {
        if (type1.getTypeType().equals(TypeType.CLASS)) {
            if (!type1.getModifiers().contains(NonAccessModifiers.FINAL) && type2.getModifiers().contains(NonAccessModifiers.FINAL)) {
                breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_FINAL, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

            }

            if (!type1.getModifiers().contains(NonAccessModifiers.ABSTRACT) && type2.getModifiers().contains(NonAccessModifiers.ABSTRACT)) {
                breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_ABSTRACT, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

            }

            if (!type1.getModifiers().contains(NonAccessModifiers.STATIC) && type2.getModifiers().contains(NonAccessModifiers.STATIC) && type1.isNested()  && type2.isNested()) {
                breakingChanges.add(new BreakingChange(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

            }

            if (type1.getModifiers().contains(NonAccessModifiers.STATIC) && !type2.getModifiers().contains(NonAccessModifiers.STATIC) && type1.isNested()  && type2.isNested()) {
                breakingChanges.add(new BreakingChange(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

            }


            if (!type1.getTypeType().equals(type2.getTypeType())) {
                breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_TYPE_CHANGED, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

            }

            if (!type1.getSuperclassName().equals("java.lang.Exception") && type2.getSuperclassName().equals("java.lang.Exception")) {
                breakingChanges.add(new BreakingChange(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

            }

            if (!type1.getSuperclassName().equals(type2.getSuperclassName()) || !type1.getSuperinterfacesNames().equals(type2.getSuperinterfacesNames())) {
                breakingChanges.add(new BreakingChange(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

            }

        }

        if (type1.getVisibility().equals(AccessModifier.PUBLIC) && type2.getVisibility().equals(AccessModifier.PROTECTED)) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_LESS_ACCESSIBLE, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

        }

        if (!type1.getReferencedTypes().equals(type2.getReferencedTypes())) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.TYPE_GENERICS_CHANGED, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

        }


        if (type1.getFormalTypeParameters().size() > type2.getFormalTypeParameters().size()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FORMAL_TYPE_PARAMETERS_REMOVED, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

        }
        if (type1.getFormalTypeParameters().size() < type2.getFormalTypeParameters().size()) {
            breakingChanges.add(new BreakingChange(BreakingChangeKind.FORMAL_TYPE_PARAMETERS_ADDED, type2, type2.getPosition(), BreakingChangeNature.MUTATION, type2));

        }
    }

    private void detectingBreakingChanges() {
        checkingForRemovedTypes();
        List<List<TypeDeclaration>> commonTypes = getUnremovedTypes();
        List<TypeDeclaration> commonTypesInV1 = commonTypes.get(0);
        List<TypeDeclaration> commonTypesInV2 = commonTypes.get(1);

        IntStream.range(0, commonTypesInV1.size())
                .forEach(i -> {

                    typeComparison(commonTypesInV1.get(i), commonTypesInV2.get(i));

                    checkingForRemovedFields(commonTypesInV1.get(i), commonTypesInV2.get(i));
                    checkingForRemovedMethods(commonTypesInV1.get(i), commonTypesInV2.get(i));
                    checkingForRemovedConstructors(commonTypesInV1.get(i), commonTypesInV2.get(i));

                    List<List<MethodDeclaration>> remainingMethods = getUnremovedMethods(commonTypesInV1.get(i), commonTypesInV2.get(i));
                    List<List<FieldDeclaration>> remainingFields = getUnremovedFields(commonTypesInV1.get(i), commonTypesInV2.get(i));
                    List<List<ConstructorDeclaration>> remainingConstructors = getUnremovedConstructors(commonTypes.get(0).get(i), commonTypes.get(1).get(i));

                    getAddedMethods(commonTypesInV1.get(i), commonTypesInV2.get(i));
                    getAddedFields(commonTypesInV1.get(i), commonTypesInV2.get(i));

                    IntStream.range(0, remainingMethods.get(0).size())
                            .forEach(j -> {
                                methodComparison(remainingMethods.get(0).get(j), remainingMethods.get(1).get(j));
                            });

                    IntStream.range(0, remainingConstructors.get(0).size())
                            .forEach(j -> {
                                constructorComparison(remainingConstructors.get(0).get(j), remainingConstructors.get(1).get(j));
                            });

                    IntStream.range(0, remainingFields.get(0).size())
                            .forEach(j -> {
                                fieldComparison(remainingFields.get(0).get(j), remainingFields.get(1).get(j));
                            });


                });

        breakingChangesPopulated = true;
    }


    /**
     * Retrieves the list of all the breaking changes detected between the two API versions.

     * @return List of all the breaking changes
     */

    public List<BreakingChange> getBreakingChanges() {
        if (!breakingChangesPopulated) {
            detectingBreakingChanges();

        }
        return breakingChanges;
    }

    /**
     * Retrieves the list of method-related breaking changes detected between the two API versions.

     * @return List of method-related breaking changes
     */
    public List<BreakingChange> getMethodBreakingChanges() {
        if (!breakingChangesPopulated) {
            detectingBreakingChanges();

        }
        return breakingChanges
                .stream()
                .filter(breakingChange -> breakingChange.getBreakingChangeElement() instanceof MethodDeclaration)
                .toList();
    }

    /**
     * Retrieves the list of constructor-related breaking changes detected between the two API versions.

     * @return List of constructor-related breaking changes
     */
    public List<BreakingChange> getConstructorBreakingChanges() {
        if (!breakingChangesPopulated) {
            detectingBreakingChanges();

        }
        return breakingChanges
                .stream()
                .filter(breakingChange -> breakingChange.getBreakingChangeElement() instanceof ConstructorDeclaration)
                .toList();
    }


    /**
     * Retrieves the list of type-related breaking changes detected between the two API versions.

     * @return List of type-related breaking changes
     */
    public List<BreakingChange> getTypeBreakingChanges() {
        if (!breakingChangesPopulated) {
            detectingBreakingChanges();

        }
        return breakingChanges
                .stream()
                .filter(breakingChange -> breakingChange.getBreakingChangeElement() instanceof TypeDeclaration)
                .toList();
    }

    /**
     * Retrieves the list of field-related breaking changes detected between the two API versions.
     *
     * @return List of field-related breaking changes
     */
    public List<BreakingChange> getFieldBreakingChanges() {
        if (!breakingChangesPopulated) {
            detectingBreakingChanges();
        }
        return breakingChanges
                .stream()
                .filter(breakingChange -> breakingChange.getBreakingChangeElement() instanceof FieldDeclaration)
                .toList();
    }





    /**
     * Retrieves a list containing all the types inside which breaking changes were detected between the two API versions.
     *
     * @return List of all the breaking changes' types
     */
    public List<TypeDeclaration> getBreakingChangesTypeDeclarations() {
        if (!breakingChangesPopulated) {
            detectingBreakingChanges();
        }
        return breakingChanges.stream()
                .map(breakingChange -> breakingChange.getBreakingChangeTypeDeclaration())
                .distinct()
                .toList();
    }



    /**
     * Generates a csv report for the detected breaking changes. This report includes the kind, type name,
     *
     * position, associated element, and nature of each detected BC.
     */


    public void breakingChangesReport() {
        List<BreakingChange> breakingChanges = getBreakingChanges();

        try (FileWriter writer = new FileWriter("breaking_changes_report.csv")) {

            writer.write("Kind,Type Declaration,Element,Nature,Position\n");

            for (BreakingChange breakingChange : breakingChanges) {

                String kind = breakingChange.getBreakingChangeKind().toString();
                String typeDeclaration = breakingChange.getBreakingChangeTypeDeclaration().getName();
                String element = breakingChange.getBreakingChangeElement().getName();
                String nature = breakingChange.getBreakingChangeNature().toString();
                String position = breakingChange.getBreakingChangePosition();



                writer.write(kind + "," + typeDeclaration + "," + element  + "," + nature + "," + position + "\n");
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
        if (!breakingChangesPopulated) {
            detectingBreakingChanges();
        }
        String result = "";

        for (BreakingChange breakingChange : breakingChanges) {
            result = result + breakingChange.toString() + "\n";
            result = result + "    =========================\n\n";
        }

        return result.toString();
    }















}
