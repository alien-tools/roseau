package io.github.alien.roseau.combinatorial.api;

import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.CachedTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.builder.Builder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.ConstructorBuilder;
import io.github.alien.roseau.combinatorial.builder.EnumBuilder;
import io.github.alien.roseau.combinatorial.builder.EnumValueBuilder;
import io.github.alien.roseau.combinatorial.builder.FieldBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordComponentBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeDeclBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.github.alien.roseau.api.model.AccessModifier.PROTECTED;
import static io.github.alien.roseau.api.model.AccessModifier.PUBLIC;
import static io.github.alien.roseau.api.model.Modifier.ABSTRACT;
import static io.github.alien.roseau.api.model.Modifier.DEFAULT;
import static io.github.alien.roseau.api.model.Modifier.FINAL;
import static io.github.alien.roseau.api.model.Modifier.NATIVE;
import static io.github.alien.roseau.api.model.Modifier.NON_SEALED;
import static io.github.alien.roseau.api.model.Modifier.SEALED;
import static io.github.alien.roseau.api.model.Modifier.STATIC;
import static io.github.alien.roseau.api.model.Modifier.SYNCHRONIZED;

public final class CombinatorialApi {
	static final TypeReferenceFactory typeReferenceFactory = new CachedTypeReferenceFactory();

	static final List<AccessModifier> topLevelVisibilities = List.of(PUBLIC);
	static final List<AccessModifier> constructorsVisibilities = List.of(PROTECTED, PUBLIC);

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
			typeReferenceFactory.createPrimitiveTypeReference("int"), // Primitive
			typeReferenceFactory.createTypeReference("java.lang.Boolean"), // Boxed
			typeReferenceFactory.createTypeReference("java.lang.Thread"), // Object reference
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createPrimitiveTypeReference("char"), 1) // Array
	);

	static final List<List<ITypeReference>> methodParamsTypes = powerSet(fieldTypes)
			.stream()
			.map(setTypes -> setTypes.stream().toList())
			.toList();
	static final List<ITypeReference> methodReturnTypes = Stream
			.concat(fieldTypes.stream(), Stream.of(new PrimitiveTypeReference("void")))
			.toList();

	static final List<List<ITypeReference>> thrownExceptions = powerSet(TypeReference.EXCEPTION /* No throws for unchecked: TypeReference.RUNTIME_EXCEPTION*/)
			.stream()
			.map(set -> set.stream().map(ITypeReference.class::cast).toList())
			.toList();

	static final List<Boolean> isHidingAndOverriding = List.of(true, false);

	static List<ClassBuilder> classBuilders = new ArrayList<>();
	static List<InterfaceBuilder> interfaceBuilders = new ArrayList<>();

	static final String apiPackageName = Constants.API_FOLDER;

	static final int typeHierarchyDepth = 0;
	static final int typeHierarchyWidth = 0;
	static final int enumValuesCount = 5;
	static final int paramsCount = 2;

	static int symbolCounter = 0;
	static int constructorCounter = 0;
	static int methodCounter = 0;

	final Map<String, TypeDeclBuilder> typeStore = new HashMap<>();

	public void build() {
		createTypes();

		weaveFields();
		weaveMethods();

		createHierarchies();
	}

	public API getAPI() {
		return new API(typeStore.values().stream().map(TypeDeclBuilder::make).toList(), typeReferenceFactory);
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
									builder.containingType = typeReferenceFactory.createTypeReference(t.qualifiedName);
									t.fields.add(builder.make());
								})
						)
				)
		);
	}

	private void weaveMethods() {
		var paramsCountToMethodsParamsTypes = getParamsCountToParamsTypesMap();

		typeStore.forEach((fqn, t) ->
				methodVisibilities(t).forEach(visibility ->
						methodModifiers(t).forEach(modifiers -> {
							var methodBuilder = new MethodBuilder();
							methodBuilder.visibility = visibility;
							methodBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
							methodBuilder.containingType = typeReferenceFactory.createTypeReference(t.qualifiedName);

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
				)
		);
	}

	private void createHierarchies() {
		var interfaceBuildersPowerSet = powerSet(interfaceBuilders);
		var widthToInterfacesMap = new HashMap<Integer, List<List<InterfaceBuilder>>>();
		IntStream.range(0, typeHierarchyWidth + 1).forEach(width ->
				interfaceBuildersPowerSet.forEach(interfaceBuilderSet -> {
					if (interfaceBuilderSet.size() == width) {
						widthToInterfacesMap.putIfAbsent(width, new ArrayList<>());
						widthToInterfacesMap.get(width).add(interfaceBuilderSet.stream().toList());
					}
				})
		);

		classBuilders.forEach(clsBuilder -> {
			// Create C extends C hierarchy & C extends C implements I hierarchy
			widthToInterfacesMap.forEach((width, allInterfaceBuildersForWidth) ->
					allInterfaceBuildersForWidth.forEach(interfaceBuildersForWidth ->
							createNewClassesExtendingClassAndImplementingInterfaces(clsBuilder, interfaceBuildersForWidth, typeHierarchyDepth - 1)
					)
			);
		});

		widthToInterfacesMap.forEach((width, allInterfaceBuildersForWidth) -> {
			if (width == 0) return;

			// Create C / R / E / I implements I hierarchy
			allInterfaceBuildersForWidth.forEach(interfaceBuildersForWidth -> {
				createNewInterfacesExtendingInterfaces(interfaceBuildersForWidth, typeHierarchyDepth - 1);
				createNewClassesImplementingInterfaces(interfaceBuildersForWidth);
				createNewEnumsImplementingInterfaces(interfaceBuildersForWidth);
				createNewRecordsImplementingInterfaces(interfaceBuildersForWidth);
			});
		});
	}

	private void createInterfaces() {
		topLevelVisibilities.forEach(visibility ->
				interfaceModifiers.forEach(modifiers -> {
					// First level of hierarchy can't have non-sealed interfaces
					if (modifiers.contains(NON_SEALED)) return;
					// Interface can't be sealed if no hierarchy
					if (modifiers.contains(SEALED) && typeHierarchyDepth == 0) return;

					var interfaceBuilder = new InterfaceBuilder();
					interfaceBuilder.qualifiedName = "%s.I%s".formatted(apiPackageName, ++symbolCounter);
					interfaceBuilder.visibility = visibility;
					interfaceBuilder.modifiers = toEnumSet(modifiers, Modifier.class);

					store(interfaceBuilder);
					interfaceBuilders.add(interfaceBuilder);
				})
		);
	}

	private void createClasses() {
		var paramsCountToConstructorsParamsTypes = getParamsCountToParamsTypesMap();

		topLevelVisibilities.forEach(visibility ->
				classModifiers.forEach(modifiers -> {
					// First level of hierarchy can't have non-sealed classes
					if (modifiers.contains(NON_SEALED)) return;
					// Class can't be sealed if no hierarchy
					if (modifiers.contains(SEALED) && typeHierarchyDepth == 0) return;

					var classBuilder = new ClassBuilder();
					classBuilder.qualifiedName = "%s.C%s".formatted(apiPackageName, ++symbolCounter);
					classBuilder.visibility = visibility;
					classBuilder.modifiers = toEnumSet(modifiers, Modifier.class);

					// Default empty public constructor
					addConstructorToClassBuilder(classBuilder, PUBLIC, List.of(), List.of(), false);

					// Constructors different params types and count
					IntStream.range(1, paramsCountToConstructorsParamsTypes.size()).forEach(paramsCount -> {
						var constructorsParamsTypesForParamsCount = paramsCountToConstructorsParamsTypes.get(paramsCount);

						constructorsParamsTypesForParamsCount.forEach(constructorParamsTypes -> {
							var constructorVisibility = constructorsVisibilities.get(constructorCounter % constructorsVisibilities.size());
							var constructorExceptions = thrownExceptions.get(Math.ceilDiv(constructorCounter, constructorsVisibilities.size()) % thrownExceptions.size());

							addConstructorToClassBuilder(classBuilder, constructorVisibility, constructorParamsTypes, constructorExceptions, false);
						});
					});

					// Constructors with Varargs
					IntStream.range(0, paramsCountToConstructorsParamsTypes.size() - 1).forEach(paramsCount ->
						fieldTypes.forEach(varArgsParamType -> {
							var constructorsParamsTypesForParamsCount = paramsCountToConstructorsParamsTypes.get(paramsCount);
							var constructorParamsTypes = constructorsParamsTypesForParamsCount.get(constructorCounter % constructorsParamsTypesForParamsCount.size());

							if (!constructorParamsTypes.isEmpty()) {
								var paramTypeBeforeVarargs = constructorParamsTypes.getLast();
								if (varArgsParamType.getQualifiedName().equals(paramTypeBeforeVarargs.getQualifiedName())) {
									return;
								}
							}

							var constructorVisibility = constructorsVisibilities.get(constructorCounter % constructorsVisibilities.size());
							var constructorExceptions = thrownExceptions.get(Math.ceilDiv(constructorCounter, constructorsVisibilities.size()) % thrownExceptions.size());
							var constructorParamsTypesWithVarargs = new ArrayList<>(constructorParamsTypes);
							constructorParamsTypesWithVarargs.add(varArgsParamType);

							addConstructorToClassBuilder(classBuilder, constructorVisibility, constructorParamsTypesWithVarargs, constructorExceptions, true);
						})
					);

					store(classBuilder);
					classBuilders.add(classBuilder);
				})
		);
	}

	private void createRecords() {
		var paramsCountToRecordsComponentTypes = getParamsCountToParamsTypesMap();

		topLevelVisibilities.forEach(visibility ->
				recordModifiers.forEach(modifiers -> {
					// Record components different types and count
					IntStream.range(0, paramsCount + 1).forEach(recordComponentParamsCount -> {
						var recordsComponentsTypesForParamsCount = paramsCountToRecordsComponentTypes.get(recordComponentParamsCount);

						recordsComponentsTypesForParamsCount.forEach(recordsParamsTypes -> {
							var recordBuilder = new RecordBuilder();
							recordBuilder.qualifiedName = "%s.R%s".formatted(apiPackageName, ++symbolCounter);
							recordBuilder.visibility = visibility;
							recordBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
							addRecordComponentsToRecordBuilder(recordBuilder, recordsParamsTypes);

							store(recordBuilder);
						});
					});

					// Varargs
					IntStream.range(1, paramsCount + 1).forEach(recordComponentParamsCount ->
							fieldTypes.forEach(varArgsParamType -> {
								var recordsComponentsTypesForParamsCount = paramsCountToRecordsComponentTypes.get(recordComponentParamsCount - 1);
								var recordsParamsTypes = recordsComponentsTypesForParamsCount.get(symbolCounter % recordsComponentsTypesForParamsCount.size());

								var recordBuilder = new RecordBuilder();
								recordBuilder.qualifiedName = "%s.R%s".formatted(apiPackageName, ++symbolCounter);
								recordBuilder.visibility = visibility;
								recordBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
								addRecordComponentsToRecordBuilder(recordBuilder, recordsParamsTypes);

								var varArgsRecordComponentBuilder = new RecordComponentBuilder();
								varArgsRecordComponentBuilder.qualifiedName = "%s.c%s".formatted(recordBuilder.qualifiedName, recordComponentParamsCount);
								varArgsRecordComponentBuilder.type = varArgsParamType;
								varArgsRecordComponentBuilder.containingType = typeReferenceFactory.createTypeReference(recordBuilder.qualifiedName);
								varArgsRecordComponentBuilder.isVarargs = true;
								recordBuilder.recordComponents.add(varArgsRecordComponentBuilder.make());

								store(recordBuilder);
							})
					);
				})
		);
	}

	private void createEnums() {
		topLevelVisibilities.forEach(visibility ->
				enumModifiers.forEach(modifiers -> {
					var enumBuilder = new EnumBuilder();
					enumBuilder.qualifiedName = "%s.E%s".formatted(apiPackageName, ++symbolCounter);
					enumBuilder.visibility = visibility;
					enumBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
					addEnumValuesToEnumBuilder(enumBuilder);

					store(enumBuilder);
				})
		);
	}

	private void createNewClassesExtendingClassAndImplementingInterfaces(ClassBuilder superClsBuilder, List<InterfaceBuilder> implementingIntfBuilders, int depth) {
		var superCls = superClsBuilder.make();
		if (superCls.isFinal() || depth < 0) return;

		classModifiers.forEach(modifiers -> {
			// Last level of hierarchy can't have sealed classes
			if (depth == 0 && modifiers.contains(SEALED))
				return;
			// Class extending sealed class must be sealed, non-sealed or final
			if (superCls.isSealed() && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED, FINAL)).isEmpty())
				return;
			// Class implementing at least one sealed interface must be sealed, non-sealed or final
			if (implementingIntfBuilders.stream().anyMatch(iB -> iB.make().isSealed()) && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED, FINAL)).isEmpty())
				return;
			// Class extending non-sealed class can't be non-sealed
			if (!superCls.isSealed() && modifiers.contains(NON_SEALED))
				return;
			// Class implementing non-sealed interfaces can't be non-sealed
			if (implementingIntfBuilders.stream().noneMatch(iB -> iB.make().isSealed()) && modifiers.contains(NON_SEALED))
				return;

			topLevelVisibilities.forEach(visibility ->
					isHidingAndOverriding.forEach(isHidingAndOverriding -> {
						var clsBuilder = new ClassBuilder();
						clsBuilder.qualifiedName = "%s.C%s".formatted(apiPackageName, ++symbolCounter);
						clsBuilder.visibility = visibility;
						clsBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
						clsBuilder.superClass = typeReferenceFactory.createTypeReference(superCls.getQualifiedName());

						if (superCls.isSealed()) {
							superClsBuilder.permittedTypes.add(clsBuilder.qualifiedName);
						}

						if (isHidingAndOverriding) {
							superCls.getDeclaredFields()
									.forEach(f -> clsBuilder.fields.add(generateFieldForTypeDeclBuilder(f, clsBuilder)));
						}

						var methodsToGenerate = new HashMap<String, MethodDecl>();
						if (isHidingAndOverriding) {
							superCls.getDeclaredMethods().stream()
									.filter(m -> !m.isFinal())
									.forEach(m -> methodsToGenerate.put(m.getSignature(), generateMethodForTypeDeclBuilder(m, clsBuilder)));
						} else if (superCls.isAbstract()) {
							superCls.getAllMethodsToImplement()
									.forEach(m -> methodsToGenerate.put(m.getSignature(), generateMethodForTypeDeclBuilder(m, clsBuilder)));
						}

						implementingIntfBuilders.forEach(implementingIntfBuilder -> {
							var implementingIntf = implementingIntfBuilder.make();

							clsBuilder.implementedInterfaces.add(typeReferenceFactory.createTypeReference(implementingIntf.getQualifiedName()));
							implementingIntf.getAllMethods()
									.forEach(m -> {
										if (!methodsToGenerate.containsKey(m.getSignature())) {
											methodsToGenerate.put(m.getSignature(), generateMethodForTypeDeclBuilder(m, clsBuilder));
										}
									});

							if (implementingIntf.isSealed()) {
								implementingIntfBuilder.permittedTypes.add(clsBuilder.qualifiedName);
							}
						});

						clsBuilder.methods.addAll(methodsToGenerate.values());

						store(clsBuilder);
						if (depth > 0)
							createNewClassesExtendingClassAndImplementingInterfaces(clsBuilder, implementingIntfBuilders, depth - 1);
					})
			);
		});
	}

	private void createNewInterfacesExtendingInterfaces(List<InterfaceBuilder> extendingIntfBuilders, int depth) {
		topLevelVisibilities.forEach(visibility ->
				interfaceModifiers.forEach(modifiers -> {
					// Last level of hierarchy can't have sealed interfaces
					if (depth == 0 && modifiers.contains(SEALED)) return;
					// Interface extending at least one sealed interface must be sealed or non-sealed
					if (extendingIntfBuilders.stream().anyMatch(p -> p.make().isSealed()) && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED)).isEmpty())
						return;
					// Interface extending non-sealed interfaces can't be non-sealed
					if (extendingIntfBuilders.stream().noneMatch(p -> p.make().isSealed()) && modifiers.contains(NON_SEALED))
						return;

					var intfBuilder = new InterfaceBuilder();
					intfBuilder.qualifiedName = "%s.I%s".formatted(apiPackageName, ++symbolCounter);
					intfBuilder.visibility = visibility;
					intfBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
					addImplementedInterfacesToTypeDeclBuilder(intfBuilder, extendingIntfBuilders);

					store(intfBuilder);
					if (depth > 0)
						createNewInterfacesExtendingInterfaces(List.of(intfBuilder), depth - 1);
				})
		);
	}

	private void createNewClassesImplementingInterfaces(List<InterfaceBuilder> implementingIntfBuilders) {
		topLevelVisibilities.forEach(visibility ->
				classModifiers.forEach(modifiers -> {
					// Last level of hierarchy can't have sealed classes
					if (modifiers.contains(SEALED))
						return;
					// Class implementing at least one sealed interface must be sealed, non-sealed or final
					if (implementingIntfBuilders.stream().anyMatch(iB -> iB.make().isSealed()) && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED, FINAL)).isEmpty())
						return;
					// Class implementing non-sealed interfaces can't be non-sealed
					if (implementingIntfBuilders.stream().noneMatch(iB -> iB.make().isSealed()) && modifiers.contains(NON_SEALED))
						return;

					var clsBuilder = new ClassBuilder();
					clsBuilder.qualifiedName = "%s.C%s".formatted(apiPackageName, ++symbolCounter);
					clsBuilder.visibility = visibility;
					clsBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
					addImplementedInterfacesToTypeDeclBuilder(clsBuilder, implementingIntfBuilders);

					store(clsBuilder);
				})
		);
	}

	private void createNewRecordsImplementingInterfaces(List<InterfaceBuilder> implementingIntfBuilders) {
		topLevelVisibilities.forEach(visibility ->
				recordModifiers.forEach(modifiers -> {
					var rcdBuilder = new RecordBuilder();
					rcdBuilder.qualifiedName = "%s.R%s".formatted(apiPackageName, ++symbolCounter);
					rcdBuilder.visibility = visibility;
					rcdBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
					addImplementedInterfacesToTypeDeclBuilder(rcdBuilder, implementingIntfBuilders);

					store(rcdBuilder);
				})
		);
	}

	private void createNewEnumsImplementingInterfaces(List<InterfaceBuilder> implementingIntfBuilders) {
		topLevelVisibilities.forEach(visibility ->
				enumModifiers.forEach(modifiers -> {
					var enumBuilder = new EnumBuilder();
					enumBuilder.qualifiedName = "%s.E%s".formatted(apiPackageName, ++symbolCounter);
					enumBuilder.visibility = visibility;
					enumBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
					addImplementedInterfacesToTypeDeclBuilder(enumBuilder, implementingIntfBuilders);
					addEnumValuesToEnumBuilder(enumBuilder);

					store(enumBuilder);
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

	private static void addConstructorToClassBuilder(ClassBuilder classBuilder, AccessModifier visibility, List<ITypeReference> params,
													 List<ITypeReference> exceptions, boolean lastParamIsVarargs) {
		var constructorBuilder = new ConstructorBuilder();
		constructorBuilder.qualifiedName = classBuilder.qualifiedName;
		constructorBuilder.visibility = visibility;
		constructorBuilder.containingType = typeReferenceFactory.createTypeReference(classBuilder.qualifiedName);
		constructorBuilder.type = new PrimitiveTypeReference("void");
		constructorBuilder.thrownExceptions = exceptions;

		IntStream.range(0, params.size()).forEach(constructorParamTypeIndex -> {
			var constructorsParamType = params.get(constructorParamTypeIndex);
			boolean isVarargs = lastParamIsVarargs && constructorParamTypeIndex == params.size() - 1;
			constructorBuilder.parameters.add(new ParameterDecl("c" + constructorParamTypeIndex, constructorsParamType, isVarargs));
		});

		classBuilder.constructors.add(constructorBuilder.make());
		constructorCounter++;
	}

	private static void addImplementedInterfacesToTypeDeclBuilder(TypeDeclBuilder builder, List<InterfaceBuilder> implementingIntfBuilders) {
		implementingIntfBuilders.forEach(implementingIntfBuilder -> {
			var implementingIntf = implementingIntfBuilder.make();

			builder.implementedInterfaces.add(typeReferenceFactory.createTypeReference(implementingIntf.getQualifiedName()));
			implementingIntf.getAllMethods()
					.forEach(m -> builder.methods.add(generateMethodForTypeDeclBuilder(m, builder)));

			if (implementingIntf.isSealed()) {
				implementingIntfBuilder.permittedTypes.add(builder.qualifiedName);
			}
		});
	}

	private static void addRecordComponentsToRecordBuilder(RecordBuilder recordBuilder, List<ITypeReference> recordsParamsTypes) {
		IntStream.range(0, recordsParamsTypes.size()).forEach(recordComponentTypeIndex -> {
			var recordComponentType = recordsParamsTypes.get(recordComponentTypeIndex);

			var recordComponentBuilder = new RecordComponentBuilder();
			recordComponentBuilder.qualifiedName = "%s.c%s".formatted(recordBuilder.qualifiedName, recordComponentTypeIndex);
			recordComponentBuilder.type = recordComponentType;
			recordComponentBuilder.containingType = typeReferenceFactory.createTypeReference(recordBuilder.qualifiedName);

			recordBuilder.recordComponents.add(recordComponentBuilder.make());
		});
	}

	private static void addEnumValuesToEnumBuilder(EnumBuilder enumBuilder) {
		var enumTypeReference = typeReferenceFactory.createTypeReference(enumBuilder.qualifiedName);

		for (int i = 0; i < enumValuesCount; i++) {
			var enumValueBuilder = new EnumValueBuilder();
			enumValueBuilder.qualifiedName = "%s.V%s".formatted(enumBuilder.qualifiedName, i);
			enumValueBuilder.containingType = enumTypeReference;
			enumValueBuilder.type = enumTypeReference;

			enumBuilder.values.add(enumValueBuilder.make());
		}
	}

	private static FieldDecl generateFieldForTypeDeclBuilder(FieldDecl field, TypeDeclBuilder builder) {
		var typeDecl = builder.make();
		var fieldBuilder = new FieldBuilder();

		fieldBuilder.qualifiedName = builder.qualifiedName + "." + field.getSimpleName();
		fieldBuilder.visibility = field.getVisibility();
		fieldBuilder.modifiers = toEnumSet(field.getModifiers(), Modifier.class);
		fieldBuilder.containingType = typeReferenceFactory.createTypeReference(typeDecl.getQualifiedName());
		fieldBuilder.type = field.getType();

		return fieldBuilder.make();
	}

	private static MethodDecl generateMethodForTypeDeclBuilder(MethodDecl method, TypeDeclBuilder builder) {
		// @Override ann?
		var typeDecl = builder.make();
		var methodBuilder = new MethodBuilder();

		methodBuilder.qualifiedName = builder.qualifiedName + "." + method.getSimpleName();
		methodBuilder.visibility = method.getVisibility();
		methodBuilder.containingType = typeReferenceFactory.createTypeReference(typeDecl.getQualifiedName());
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
			case InterfaceBuilder ignored -> List.of(PUBLIC);
			default -> List.of(PUBLIC, PROTECTED);
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

	private static List<AccessModifier> methodVisibilities(Builder<TypeDecl> container) {
		return switch (container) {
			case InterfaceBuilder ignored -> List.of(PUBLIC);
			default -> List.of(PUBLIC, PROTECTED);
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

	private static Map<Integer, List<List<ITypeReference>>> getParamsCountToParamsTypesMap() {
		Map<Integer, List<List<ITypeReference>>> paramsCountToConstructorsParamsTypes = new HashMap<>();

		IntStream.range(0, paramsCount + 1).forEach(methodParamsCount -> {
			var methodsArgsTypes = methodParamsTypes.stream()
					.filter(types -> types.size() == methodParamsCount)
					.toList();

			paramsCountToConstructorsParamsTypes.put(methodParamsCount, methodsArgsTypes);
		});

		return paramsCountToConstructorsParamsTypes;
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
