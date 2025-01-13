package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.combinatorial.api.builder.*;
import com.google.common.collect.Sets;
import org.javatuples.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.maracas.roseau.api.model.AccessModifier.PACKAGE_PRIVATE;
import static com.github.maracas.roseau.api.model.AccessModifier.PRIVATE;
import static com.github.maracas.roseau.api.model.AccessModifier.PROTECTED;
import static com.github.maracas.roseau.api.model.AccessModifier.PUBLIC;
import static com.github.maracas.roseau.api.model.Modifier.ABSTRACT;
import static com.github.maracas.roseau.api.model.Modifier.DEFAULT;
import static com.github.maracas.roseau.api.model.Modifier.FINAL;
import static com.github.maracas.roseau.api.model.Modifier.NATIVE;
import static com.github.maracas.roseau.api.model.Modifier.NON_SEALED;
import static com.github.maracas.roseau.api.model.Modifier.SEALED;
import static com.github.maracas.roseau.api.model.Modifier.STATIC;
import static com.github.maracas.roseau.api.model.Modifier.SYNCHRONIZED;

public class CombinatorialApi {
    static final List<AccessModifier> topLevelVisibilities = List.of(PUBLIC, PACKAGE_PRIVATE);

    // STATIC handled separately for nested types only
    static final Set<Set<Modifier>> classModifiers = powerSet(FINAL, ABSTRACT, SEALED, NON_SEALED)
            .stream()
            .filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
            .filter(mods -> !mods.containsAll(Set.of(ABSTRACT, FINAL)))
            .filter(mods -> !mods.contains(FINAL) || Sets.intersection(mods, Set.of(ABSTRACT, SEALED, NON_SEALED)).isEmpty())
            .collect(Collectors.toSet());
    static final Set<Set<Modifier>> interfaceModifiers = powerSet(ABSTRACT, SEALED, NON_SEALED)
            .stream()
            .filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
            .collect(Collectors.toSet());
    static final Set<Set<Modifier>> recordModifiers = powerSet(FINAL);
    static final Set<Set<Modifier>> enumModifiers = powerSet();

    static final List<ITypeReference> fieldTypes = List.of(
            new PrimitiveTypeReference("int"), // Primitive
            new TypeReference<>("java.lang.Integer"), // Boxed
            new TypeReference<>("java.lang.Thread"), // Object reference
            new ArrayTypeReference(new PrimitiveTypeReference("int"), 1) // Array
    );

    static final List<List<ITypeReference>> methodParamsTypes = powerSet(fieldTypes)
            .stream()
            .map(setTypes -> setTypes.stream().toList())
            .toList();
    static final List<ITypeReference> methodReturnTypes = Stream
            .concat(fieldTypes.stream(), Stream.of(new PrimitiveTypeReference("void")))
            .toList();

    static final List<List<TypeReference<ClassDecl>>> thrownExceptions = powerSet(TypeReference.EXCEPTION /* No throws for unchecked: TypeReference.RUNTIME_EXCEPTION*/)
            .stream()
            .map(set -> set.stream().toList())
            .toList();

    static final List<Boolean> isOverridings = List.of(true, false);
    static final List<Boolean> isHidings = List.of(true, false);

    static final int typeHierarchyDepth = 2;
    static final int typeHierarchyWidth = 2;
    static final int enumValuesCount = 5;
    static final int paramsCount = 2;
    static int symbolCounter = 0;
    static int methodCounter = 0;

    Map<String, TypeDeclBuilder> typeStore = new HashMap<>();

    public void build() {
        createTypes();

        weaveFields();
        weaveMethods();

        createHierarchies();
    }

    public API getAPI() {
        return new API(typeStore.values().stream().map(TypeDeclBuilder::make).toList(), new SpoonAPIFactory());
    }

    private void createTypes() {
        createInterfaces();
        createClasses();
        createRecords();
        createEnums();
    }

    private void weaveFields() {
        typeStore.forEach((fqn, t) ->
                fieldVisibilities(t).forEach(visibility ->
                        fieldModifiers(t).forEach(modifiers ->
                                fieldTypes.forEach(type -> {
                                    var builder = new FieldBuilder();
                                    builder.qualifiedName = t.qualifiedName + ".f" + ++symbolCounter;
                                    builder.visibility = visibility;
                                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                                    builder.type = type;
                                    builder.containingType = new TypeReference<>(t.qualifiedName);
                                    t.fields.add(builder.make());
                                })
                        )
                )
        );
    }

    private void weaveMethods() {
        Map<Integer, List<List<ITypeReference>>> paramsCountToMethodsParamsTypes = new HashMap<>();
        IntStream.range(0, paramsCount + 1).forEach(methodParamsCount -> {
            var methodsArgsTypes = methodParamsTypes.stream()
                    .filter(types -> types.size() == methodParamsCount)
                    .toList();

            paramsCountToMethodsParamsTypes.put(methodParamsCount, methodsArgsTypes);
        });

        typeStore.forEach((fqn, t) ->
                methodVisibilitiesAndModifiers(t).forEach(visibilityAndModifiers -> {
                    var visibility = visibilityAndModifiers.getValue0();
                    var modifiers = visibilityAndModifiers.getValue1();

                    var methodBuilder = new MethodBuilder();
                    methodBuilder.visibility = visibility;
                    methodBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
                    methodBuilder.containingType = new TypeReference<>(t.qualifiedName);

                    // Parameters different types and count
                    IntStream.range(0, paramsCount + 1).forEach(methodParamsCount -> {
                        var methodsParamsTypesForParamsCount = paramsCountToMethodsParamsTypes.get(methodParamsCount);
                        if (methodParamsCount > 1) {
                            methodsParamsTypesForParamsCount = List.of(methodsParamsTypesForParamsCount.get(methodCounter % methodsParamsTypesForParamsCount.size()));
                        }

                        methodsParamsTypesForParamsCount.forEach(methodParamsTypes -> {
                            IntStream.range(0, methodParamsTypes.size()).forEach(fieldIndex -> {
                                var field = methodParamsTypes.get(fieldIndex % methodParamsTypes.size());
                                methodBuilder.parameters.add(new ParameterDecl("p" + fieldIndex, field, false));
                            });

                            addMethodToType(t, methodBuilder);

                            methodBuilder.resetParameters();
                        });
                    });

                    // Varargs
                    IntStream.range(1, paramsCount + 1).forEach(methodParamsCount ->
                            fieldTypes.forEach(varArgsParamType -> {
                                var methodsParamsTypesForParamsCount = paramsCountToMethodsParamsTypes.get(methodParamsCount - 1);
                                var methodParamsTypes = methodsParamsTypesForParamsCount.get(methodCounter % methodsParamsTypesForParamsCount.size());

                                IntStream.range(0, methodParamsTypes.size()).forEach(fieldIndex -> {
                                    var field = methodParamsTypes.get(fieldIndex % methodParamsTypes.size());
                                    methodBuilder.parameters.add(new ParameterDecl("p" + fieldIndex, field, false));
                                });

                                methodBuilder.parameters.add(new ParameterDecl("p" + methodParamsCount, varArgsParamType, true));

                                addMethodToType(t, methodBuilder);

                                methodBuilder.resetParameters();
                            })
                    );

                    // Overloading
                    var overloadedQualifiedName = t.qualifiedName + ".m" + ++symbolCounter;
                    IntStream.range(0, paramsCount + 1).forEach(methodParamsCount -> {
                        var methodsParamsTypesForParamsCount = paramsCountToMethodsParamsTypes.get(methodParamsCount);
                        var methodParamsTypes = methodsParamsTypesForParamsCount.get(methodCounter % methodsParamsTypesForParamsCount.size());

                        methodBuilder.resetParameters();

                        IntStream.range(0, methodParamsTypes.size()).forEach(fieldIndex -> {
                            var field = methodParamsTypes.get(fieldIndex % methodParamsTypes.size());
                            methodBuilder.parameters.add(new ParameterDecl("p" + fieldIndex, field, false));
                        });

                        addMethodToType(t, methodBuilder, overloadedQualifiedName);
                    });
                })
        );
    }

    private void createHierarchies() {
        List.copyOf(typeStore.values()).forEach(t -> {
            if (t instanceof ClassBuilder c)
                createSubtypes(c, typeHierarchyDepth - 1);
            if (t instanceof InterfaceBuilder i)
                createSubtypes(i, typeHierarchyDepth - 1);
        });
    }

    private void createInterfaces() {
        topLevelVisibilities.forEach(visibility ->
                interfaceModifiers.forEach(modifiers -> {
                    // First level of hierarchy can't have non-sealed interfaces
                    if (modifiers.contains(NON_SEALED)) return;

                    var builder = new InterfaceBuilder();
                    builder.qualifiedName = "I" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    store(builder);
                })
        );
    }

    private void createClasses() {
        topLevelVisibilities.forEach(visibility ->
                classModifiers.forEach(modifiers -> {
                    // First level of hierarchy can't have non-sealed classes
                    if (modifiers.contains(NON_SEALED)) return;

                    var builder = new ClassBuilder();
                    builder.qualifiedName = "C" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    store(builder);
                })
        );
    }

    private void createRecords() {
        topLevelVisibilities.forEach(visibility ->
                recordModifiers.forEach(modifiers -> {
                    var builder = new RecordBuilder();
                    builder.qualifiedName = "R" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    store(builder);
                })
        );
    }

    private void createEnums() {
        topLevelVisibilities.forEach(visibility ->
                enumModifiers.forEach(modifiers -> {
                    var builder = new EnumBuilder();
                    builder.qualifiedName = "E" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    for (int i = 0; i < enumValuesCount; i++)
                        builder.values.add("V" + ++symbolCounter);
                    store(builder);
                })
        );
    }

    private void createSubtypes(ClassBuilder clsBuilder, int depth) {
        if (!clsBuilder.modifiers.contains(FINAL) && !(clsBuilder instanceof RecordBuilder) && !(clsBuilder instanceof EnumBuilder)) {
            createNewClassesExtendingClass(clsBuilder, depth);
        }
    }

    private void createSubtypes(InterfaceBuilder intfBuilder, int depth) {
        createNewInterfacesExtendingInterface(intfBuilder, depth);
        createNewClassesImplementingInterface(intfBuilder, depth);
        createNewRecordsImplementingInterface(intfBuilder, depth);
        createNewEnumsImplementingInterface(intfBuilder, depth);
    }

    private void createNewClassesExtendingClass(ClassBuilder parentClsBuilder, int depth) {
        var parentCls = parentClsBuilder.make();

        classModifiers.forEach(modifiers -> {
            // Last level of hierarchy can't have sealed classes
            if (depth == 0 && modifiers.contains(SEALED)) return;
            // Class extending sealed class must be sealed, non-sealed or final
            if (parentCls.isSealed() && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED, FINAL)).isEmpty())
                return;
            // Class extending non-sealed class can't be non-sealed
            if (!parentCls.isSealed() && modifiers.contains(NON_SEALED)) return;

            topLevelVisibilities.forEach(visibility ->
                    isHidings.forEach(isHiding ->
                            isOverridings.forEach(isOverriding -> {
                                if (!isOverriding && parentCls.isAbstract()) return;

                                var childClsBuilder = new ClassBuilder();
                                childClsBuilder.qualifiedName = "C" + ++symbolCounter;
                                childClsBuilder.visibility = visibility;
                                childClsBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
                                childClsBuilder.superClass = new TypeReference<>(parentCls);
                                if (isHiding) {
                                    parentCls.getAllFields()
                                            .filter(f -> !f.isFinal())
                                            .forEach(f -> childClsBuilder.fields.add(generateFieldForTypeDeclBuilder(f, childClsBuilder)));
                                }

                                if (isOverriding) {
                                    parentCls.getAllMethods()
                                            .filter(m -> !m.isFinal())
                                            .forEach(m -> childClsBuilder.methods.add(generateMethodForTypeDeclBuilder(m, childClsBuilder)));
                                }

                                if (parentCls.isSealed()) {
                                    parentClsBuilder.permittedTypes.add(childClsBuilder.qualifiedName);
                                }

                                // TODO: Field hiding
                                store(childClsBuilder);
                                if (depth > 0)
                                    createSubtypes(childClsBuilder, depth - 1);
                            })
                    )
            );
        });
    }

    private void createNewInterfacesExtendingInterface(InterfaceBuilder parentIntfBuilder, int depth) {
        var parentIntf = parentIntfBuilder.make();

        topLevelVisibilities.forEach(visibility ->
                interfaceModifiers.forEach(modifiers -> {
                    // Last level of hierarchy can't have sealed interfaces
                    if (depth == 0 && modifiers.contains(SEALED)) return;
                    // Interface extending sealed interface must be sealed or non-sealed
                    if (parentIntf.isSealed() && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED)).isEmpty())
                        return;
                    // Interface extending non-sealed interface can't be non-sealed
                    if (!parentIntf.isSealed() && modifiers.contains(NON_SEALED)) return;

                    var childIntfBuilder = new InterfaceBuilder();
                    childIntfBuilder.qualifiedName = "I" + ++symbolCounter;
                    childIntfBuilder.visibility = visibility;
                    childIntfBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
                    childIntfBuilder.implementedInterfaces.add(new TypeReference<>(parentIntf));
                    parentIntf.getAllMethods()
                            .forEach(m -> childIntfBuilder.methods.add(generateMethodForTypeDeclBuilder(m, childIntfBuilder)));

                    if (parentIntf.isSealed()) {
                        parentIntfBuilder.permittedTypes.add(childIntfBuilder.qualifiedName);
                    }

                    // TODO: Field hiding
                    store(childIntfBuilder);
                    if (depth > 0)
                        createSubtypes(childIntfBuilder, depth - 1);
                })
        );
    }

    private void createNewClassesImplementingInterface(InterfaceBuilder parentIntfBuilder, int depth) {
        var parentIntf = parentIntfBuilder.make();

        topLevelVisibilities.forEach(visibility ->
                classModifiers.forEach(modifiers -> {
                    // Last level of hierarchy can't have sealed classes
                    if (depth == 0 && modifiers.contains(SEALED)) return;
                    // Class implementing sealed interface must be sealed, non-sealed or final
                    if (parentIntf.isSealed() && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED, FINAL)).isEmpty())
                        return;
                    // Class implementing non-sealed interface can't be non-sealed
                    if (!parentIntf.isSealed() && modifiers.contains(NON_SEALED)) return;

                    var childClsBuilder = new ClassBuilder();
                    childClsBuilder.qualifiedName = "C" + ++symbolCounter;
                    childClsBuilder.visibility = visibility;
                    childClsBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
                    childClsBuilder.implementedInterfaces.add(new TypeReference<>(parentIntf));
                    parentIntf.getAllMethods()
                            .forEach(m -> childClsBuilder.methods.add(generateMethodForTypeDeclBuilder(m, childClsBuilder)));

                    if (parentIntf.isSealed()) {
                        parentIntfBuilder.permittedTypes.add(childClsBuilder.qualifiedName);
                    }

                    // TODO: Field hiding
                    store(childClsBuilder);
                    if (depth > 0)
                        createSubtypes(childClsBuilder, depth - 1);
                })
        );
    }

    private void createNewRecordsImplementingInterface(InterfaceBuilder parentIntfBuilder, int depth) {
        var parentIntf = parentIntfBuilder.make();

        topLevelVisibilities.forEach(visibility ->
                recordModifiers.forEach(modifiers -> {
                    var childRcdBuilder = new RecordBuilder();
                    childRcdBuilder.qualifiedName = "R" + ++symbolCounter;
                    childRcdBuilder.visibility = visibility;
                    childRcdBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
                    childRcdBuilder.implementedInterfaces.add(new TypeReference<>(parentIntf));
                    parentIntf.getAllMethods()
                            .forEach(m -> childRcdBuilder.methods.add(generateMethodForTypeDeclBuilder(m, childRcdBuilder)));

                    if (parentIntf.isSealed()) {
                        parentIntfBuilder.permittedTypes.add(childRcdBuilder.qualifiedName);
                    }

                    // TODO: Field hiding
                    store(childRcdBuilder);
                    if (depth > 0)
                        createSubtypes(childRcdBuilder, depth - 1);
                })
        );
    }

    private void createNewEnumsImplementingInterface(InterfaceBuilder parentIntfBuilder, int depth) {
        var parentIntf = parentIntfBuilder.make();

        topLevelVisibilities.forEach(visibility ->
                enumModifiers.forEach(modifiers -> {
                    var childEnmBuilder = new EnumBuilder();
                    childEnmBuilder.qualifiedName = "E" + ++symbolCounter;
                    childEnmBuilder.visibility = visibility;
                    childEnmBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
                    for (int i = 0; i < enumValuesCount; i++)
                        childEnmBuilder.values.add("V" + ++symbolCounter);
                    childEnmBuilder.implementedInterfaces.add(new TypeReference<>(parentIntf));
                    parentIntf.getAllMethods()
                            .forEach(m -> childEnmBuilder.methods.add(generateMethodForTypeDeclBuilder(m, childEnmBuilder)));

                    if (parentIntf.isSealed()) {
                        parentIntfBuilder.permittedTypes.add(childEnmBuilder.qualifiedName);
                    }

                    // TODO: Field hiding
                    store(childEnmBuilder);
                    if (depth > 0)
                        createSubtypes(childEnmBuilder, depth - 1);
                })
        );
    }

    private void store(TypeDeclBuilder type) {
        typeStore.put(type.qualifiedName, type);
    }

    private static void addMethodToType(TypeDeclBuilder type, MethodBuilder methodBuilder) {
        addMethodToType(type, methodBuilder, type.qualifiedName + ".m" + ++symbolCounter);
    }

    private static void addMethodToType(TypeDeclBuilder type, MethodBuilder methodBuilder, String qualifiedName) {
        methodBuilder.qualifiedName = qualifiedName;
        methodBuilder.type = methodReturnTypes.get(methodCounter % methodReturnTypes.size());
        methodBuilder.thrownExceptions = thrownExceptions.get(methodCounter % thrownExceptions.size());

        type.methods.add(methodBuilder.make());
        methodCounter++;
    }

    private static FieldDecl generateFieldForTypeDeclBuilder(FieldDecl field, TypeDeclBuilder builder) {
        var typeDecl = builder.make();
        var fieldBuilder = new FieldBuilder();

        fieldBuilder.qualifiedName = builder.qualifiedName + "." + field.getSimpleName();
        fieldBuilder.visibility = field.getVisibility();
        fieldBuilder.modifiers = toEnumSet(field.getModifiers(), Modifier.class);
        fieldBuilder.containingType = new TypeReference<>(typeDecl);
        fieldBuilder.type = field.getType();

        return fieldBuilder.make();
    }

    private static MethodDecl generateMethodForTypeDeclBuilder(MethodDecl method, TypeDeclBuilder builder) {
        // @Override ann?
        var typeDecl = builder.make();
        var methodBuilder = new MethodBuilder();

        methodBuilder.qualifiedName = builder.qualifiedName + "." + method.getSimpleName();
        methodBuilder.visibility = method.getVisibility();
        methodBuilder.containingType = new TypeReference<>(typeDecl);
        methodBuilder.thrownExceptions = method.getThrownExceptions();
        methodBuilder.parameters.addAll(method.getParameters());
        methodBuilder.type = method.getType();

        methodBuilder.modifiers = toEnumSet(method.getModifiers(), Modifier.class);
        if (!typeDecl.isAbstract())
            methodBuilder.modifiers.remove(ABSTRACT);
        if (typeDecl.isClass())
            methodBuilder.modifiers.remove(DEFAULT);

        return methodBuilder.make();
    }

    private static List<AccessModifier> fieldVisibilities(Builder<TypeDecl> container) {
        return switch (container) {
            case InterfaceBuilder ignored -> List.of(PUBLIC, PACKAGE_PRIVATE);
            default -> List.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
        };
    }

    private static Set<Set<Modifier>> fieldModifiers(Builder<TypeDecl> container) {
        return switch (container) {
            case RecordBuilder ignored -> powerSet(STATIC, FINAL).stream()
                    .filter(mods -> mods.contains(STATIC))
                    .collect(Collectors.toSet());
            default -> powerSet(STATIC, FINAL);
        };
    }

    private List<Pair<AccessModifier, Set<Modifier>>> methodVisibilitiesAndModifiers(TypeDeclBuilder typeDeclBuilder) {
        List<Pair<AccessModifier, Set<Modifier>>> visibilitiesAndModifiers = new ArrayList<>();

        methodVisibilities(typeDeclBuilder).forEach(visibility ->
                methodModifiers(typeDeclBuilder).forEach(modifiers -> {
                    if (visibility == PRIVATE && !Sets.intersection(modifiers, Set.of(DEFAULT, ABSTRACT)).isEmpty())
                        return;
                    if (visibility == PACKAGE_PRIVATE && modifiers.contains(ABSTRACT))
                        if (typeDeclBuilder instanceof ClassBuilder clsBuilder && clsBuilder.modifiers.contains(ABSTRACT))
                            return;

                    visibilitiesAndModifiers.add(new Pair<>(visibility, modifiers));
                })
        );

        return visibilitiesAndModifiers;
    }

    private static List<AccessModifier> methodVisibilities(Builder<TypeDecl> container) {
        return switch (container) {
            case InterfaceBuilder ignored -> List.of(PUBLIC, /*PACKAGE_PRIVATE,*/ PRIVATE);
            default -> List.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
        };
    }

    private static Set<Set<Modifier>> methodModifiers(Builder<TypeDecl> container) {
        var modifiers = powerSet(STATIC, FINAL, ABSTRACT, NATIVE, DEFAULT, SYNCHRONIZED)
                .stream()
                .filter(mods -> !mods.containsAll(Set.of(FINAL, ABSTRACT)))
                .filter(mods -> !mods.contains(ABSTRACT) || Sets.intersection(mods, Set.of(NATIVE, STATIC, SYNCHRONIZED)).isEmpty())
                .filter(mods -> !mods.contains(DEFAULT) || Sets.intersection(mods, Set.of(STATIC, ABSTRACT)).isEmpty())
                .collect(Collectors.toSet());

        return switch (container) {
            case InterfaceBuilder ignored -> modifiers.stream()
                    .filter(mods -> Sets.intersection(mods, Set.of(NATIVE, SYNCHRONIZED, FINAL)).isEmpty())
                    .collect(Collectors.toSet());
            case RecordBuilder ignored -> modifiers.stream()
                    .filter(mods -> !mods.contains(ABSTRACT) && !mods.contains(NATIVE) && !mods.contains(DEFAULT))
                    .collect(Collectors.toSet());
            case ClassBuilder b -> modifiers.stream()
                    .filter(mods -> !mods.contains(DEFAULT))
                    .filter(mods -> b.make().isAbstract() || !mods.contains(ABSTRACT))
                    .collect(Collectors.toSet());
            default -> modifiers;
        };
    }

    private static <T> Set<Set<T>> powerSet(List<T> elements) {
        return Sets.powerSet(new LinkedHashSet<>(elements));
    }

    private static <T> Set<Set<T>> powerSet(T... elements) {
        return powerSet(List.of(elements));
    }

    private static <T extends Enum<T>> EnumSet<T> toEnumSet(Set<T> set, Class<T> cls) {
        return set.isEmpty()
                ? EnumSet.noneOf(cls)
                : EnumSet.copyOf(set);
    }
}
