package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.combinatorial.api.builder.*;
import com.google.common.collect.Sets;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    static final Set<Set<Modifier>> classModifiers = Sets.powerSet(Set.of(FINAL, ABSTRACT/*, SEALED, NON_SEALED*/)) // TODO: sealed
            .stream()
            .filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
            .filter(mods -> !mods.containsAll(Set.of(ABSTRACT, FINAL)))
            .filter(mods -> !mods.contains(FINAL) || Sets.intersection(mods, Set.of(ABSTRACT/*, SEALED, NON_SEALED*/)).isEmpty()) // TODO: sealed
            .collect(Collectors.toSet());
    static final Set<Set<Modifier>> interfaceModifiers = Sets.powerSet(Set.of(ABSTRACT/*, SEALED, NON_SEALED*/)) // TODO: sealed
            .stream()
            .filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
            .collect(Collectors.toSet());
    static final Set<Set<Modifier>> recordModifiers = Sets.powerSet(Set.of(FINAL));
    static final Set<Set<Modifier>> enumModifiers = Sets.powerSet(Set.of());

    static final List<ITypeReference> fieldTypes = List.of(
            new PrimitiveTypeReference("int")/*, // Primitive
		new TypeReference<>("java.lang.Integer"), // Boxed
		new TypeReference<>("java.lang.Thread"), // Object reference
		new ArrayTypeReference(new PrimitiveTypeReference("int"), 1) // Array */
    );
    static final List<ITypeReference> methodTypes = fieldTypes;
    static final Set<Set<TypeReference<ClassDecl>>> thrownExceptions = Sets.powerSet(Set.of(TypeReference.EXCEPTION /* No throws for unchecked: TypeReference.RUNTIME_EXCEPTION*/));

    static final int typeHierarchyDepth = 2;
    static final int typeHierarchyWidth = 2;
    static final int paramsCount = 1;
    static int symbolCounter = 0;

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
        // TODO: overloading
        typeStore.forEach((fqn, t) ->
                methodVisibilities(t).forEach(visibility ->
                        methodModifiers(t).forEach(modifiers ->
                                methodTypes.forEach(type ->
                                        thrownExceptions.forEach(exc ->
                                                IntStream.range(0, paramsCount + 1).forEach(pCount -> {
                                                    /* vis/mod interactions are annoying; don't want them there */
                                                    if (visibility == PRIVATE && !Sets.intersection(modifiers, Set.of(DEFAULT, ABSTRACT)).isEmpty())
                                                        return;
                                                    if (visibility == PACKAGE_PRIVATE && modifiers.contains(ABSTRACT))
                                                        if (t instanceof ClassBuilder clsBuilder && clsBuilder.modifiers.contains(ABSTRACT))
                                                            return;

                                                    var builder = new MethodBuilder();
                                                    builder.qualifiedName = t.qualifiedName + ".m" + ++symbolCounter;
                                                    builder.visibility = visibility;
                                                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                                                    builder.type = type;
                                                    builder.containingType = new TypeReference<>(t.qualifiedName);
                                                    builder.thrownExceptions = exc.stream().toList();
                                                    IntStream.range(0, pCount).forEach(p -> {
                                                        // TODO: varargs
                                                        // TODO: parameter types
                                                        builder.parameters.add(new ParameterDecl("p" + p, TypeReference.OBJECT, false));
                                                    });
                                                    t.methods.add(builder.make());
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private void createHierarchies() {
        List.copyOf(typeStore.values()).forEach(t -> {
            if (t instanceof ClassBuilder c)
                createSubtypes(c);
            if (t instanceof InterfaceBuilder i)
                createSubtypes(i);
        });
    }

    private void createInterfaces() {
        topLevelVisibilities.forEach(visibility ->
                interfaceModifiers.forEach(modifiers -> {
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
                    var builder = new ClassBuilder();
                    builder.qualifiedName = "C" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    store(builder);
                })
        );
    }

    private void createSubtypes(InterfaceBuilder intf) {
        var api = getAPI();
        var intfDecl = (InterfaceDecl) api.findType(intf.qualifiedName).get();
        createOverridingInterface(intfDecl);
        createImplementingClass(intfDecl);
    }

    private void createSubtypes(ClassBuilder cls) {
        var api = getAPI();
        var clsDecl = (ClassDecl) api.findType(cls.qualifiedName).get();
        if (!clsDecl.getModifiers().contains(FINAL)) // isEffectivelyFinal always true cause there are no constructors
            createSubclass(clsDecl);
    }

    private void createOverridingInterface(InterfaceDecl intf) {
        topLevelVisibilities.forEach(visibility ->
                interfaceModifiers.forEach(modifiers -> {
                    var builder = new InterfaceBuilder();
                    builder.qualifiedName = "I" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    builder.implementedInterfaces.add(new TypeReference<>(intf.getQualifiedName()));
                    intf.getAllMethods()
                            .forEach(m -> builder.methods.add(generateMethodForTypeDeclBuilder(m, builder)));

                    // TODO: Field hiding
                    store(builder);
                })
        );
    }

    private void createImplementingClass(InterfaceDecl intf) {
        topLevelVisibilities.forEach(visibility ->
                classModifiers.forEach(modifiers -> {
                    var builder = new ClassBuilder();
                    builder.qualifiedName = "C" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    builder.implementedInterfaces.add(new TypeReference<>(intf.getQualifiedName()));
                    intf.getAllMethods()
                            .forEach(m -> builder.methods.add(generateMethodForTypeDeclBuilder(m, builder)));

                    // TODO: Field hiding
                    store(builder);
                })
        );
    }

    private void createSubclass(ClassDecl cls) {
        topLevelVisibilities.forEach(visibility ->
                classModifiers.forEach(modifiers -> {
                    var builder = new ClassBuilder();
                    builder.qualifiedName = "C" + ++symbolCounter;
                    builder.visibility = visibility;
                    builder.modifiers = toEnumSet(modifiers, Modifier.class);
                    builder.superClass = new TypeReference<>(cls.getQualifiedName());
                    cls.getAllMethods()
                            .filter(m -> !m.isFinal())
                            .forEach(m -> builder.methods.add(generateMethodForTypeDeclBuilder(m, builder)));

                    // TODO: Field hiding
                    store(builder);
                })
        );
    }

    private MethodDecl generateMethodForTypeDeclBuilder(MethodDecl method, TypeDeclBuilder builder) {
        // @Override ann?
        var mBuilder = new MethodBuilder();

        mBuilder.qualifiedName = builder.qualifiedName + "." + method.getSimpleName();
        mBuilder.visibility = method.getVisibility();
        mBuilder.containingType = new TypeReference<>(builder.qualifiedName);
        mBuilder.thrownExceptions = method.getThrownExceptions();
        mBuilder.parameters.addAll(method.getParameters());
        mBuilder.type = method.getType();

        mBuilder.modifiers = toEnumSet(method.getModifiers(), Modifier.class);
        if (!builder.make().isAbstract())
            mBuilder.modifiers.remove(ABSTRACT);
        if (builder instanceof ClassBuilder)
            mBuilder.modifiers.remove(DEFAULT);

        return mBuilder.make();
    }

    private <T> Set<Set<T>> powerSet(T... elements) {
        return Sets.powerSet(Set.of(elements));
    }

    private Set<AccessModifier> fieldVisibilities(Builder<TypeDecl> container) {
        return switch (container) {
            case InterfaceBuilder ignored -> Set.of(PUBLIC, PACKAGE_PRIVATE);
            default -> Set.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
        };
    }

    private Set<AccessModifier> methodVisibilities(Builder<TypeDecl> container) {
        return switch (container) {
            case InterfaceBuilder ignored -> Set.of(PUBLIC, /*PACKAGE_PRIVATE,*/ PRIVATE);
            default -> Set.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
        };
    }

    private Set<Set<Modifier>> fieldModifiers(Builder<TypeDecl> container) {
        return switch (container) {
            default -> powerSet(STATIC, FINAL);
        };
    }

    private Set<Set<Modifier>> methodModifiers(Builder<TypeDecl> container) {
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
            case ClassBuilder b -> modifiers.stream()
                    .filter(mods -> !mods.contains(DEFAULT))
                    .filter(mods -> b.make().isAbstract() || !mods.contains(ABSTRACT))
                    .collect(Collectors.toSet());
            default -> modifiers;
        };
    }

    private void store(TypeDeclBuilder type) {
        typeStore.put(type.qualifiedName, type);
    }

    private <T extends Enum<T>> EnumSet<T> toEnumSet(Set<T> set, Class<T> cls) {
        return set.isEmpty()
                ? EnumSet.noneOf(cls)
                : EnumSet.copyOf(set);
    }
}
