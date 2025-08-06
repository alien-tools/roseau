package io.github.alien.roseau.combinatorial.api;

import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.*;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.builder.*;

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
import static io.github.alien.roseau.api.model.Modifier.NON_SEALED;
import static io.github.alien.roseau.api.model.Modifier.SEALED;
import static io.github.alien.roseau.api.model.Modifier.STATIC;
import static io.github.alien.roseau.api.model.Modifier.SYNCHRONIZED;

public final class CombinatorialApi {
	static final TypeReferenceFactory typeReferenceFactory = new CachingTypeReferenceFactory();

	static final List<AccessModifier> topLevelVisibilities = List.of(PUBLIC);
	static final List<AccessModifier> constructorsVisibilities = List.of(PROTECTED, PUBLIC);

	// STATIC handled separately for nested types only
	static final Set<Set<Modifier>> classModifiers = powerSet(FINAL, ABSTRACT, SEALED, NON_SEALED)
			.stream()
			.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
			.filter(mods -> !mods.containsAll(Set.of(ABSTRACT, FINAL)))
			.filter(mods -> !mods.contains(FINAL) || Sets.intersection(mods, Set.of(ABSTRACT, SEALED, NON_SEALED)).isEmpty())
			.collect(Collectors.toCollection(LinkedHashSet::new));
	static final Set<Set<Modifier>> interfaceModifiers = powerSet(ABSTRACT, SEALED, NON_SEALED)
			.stream()
			.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	static final Set<Set<Modifier>> recordModifiers = powerSet(FINAL);
	static final Set<Set<Modifier>> enumModifiers = powerSet();

	static final List<ITypeReference> fieldTypes = List.of(
			typeReferenceFactory.createPrimitiveTypeReference("int"),
			typeReferenceFactory.createTypeReference("java.lang.Long"),
			typeReferenceFactory.createTypeReference("java.util.Random"),
			typeReferenceFactory.createTypeReference("java.sql.Time"),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createPrimitiveTypeReference("int"), 1),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createTypeReference("java.lang.Long"), 1),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createTypeReference("java.util.Random"), 1),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createTypeReference("java.sql.Time"), 1)
	);

	static final List<List<ITypeReference>> methodParamsTypes = powerSet(fieldTypes)
			.stream()
			.map(setTypes -> setTypes.stream().toList())
			.toList();
	static final List<ITypeReference> methodReturnTypes = Stream
			.concat(fieldTypes.stream(), Stream.of(new PrimitiveTypeReference("void")))
			.toList();

	static final List<List<ITypeReference>> thrownExceptions = powerSet(TypeReference.IO_EXCEPTION /* No throws for unchecked: TypeReference.RUNTIME_EXCEPTION*/)
			.stream()
			.map(set -> set.stream().map(ITypeReference.class::cast).toList())
			.toList();

	static final List<Boolean> isHidingAndOverriding = List.of(/*true, */false);

	static List<ClassBuilder> classBuilders = new ArrayList<>();
	static List<InterfaceBuilder> interfaceBuilders = new ArrayList<>();

	static final String apiPackageName = "api";

	static final int typeHierarchyDepth = 1;
	static final int typeHierarchyWidth = 1;
	static final int enumValuesCount = 2;
	static final int paramsCount = 1;

	static int symbolCounter = 0;
	static int constructorCounter = 0;
	static int methodCounter = 0;

	final Map<String, TypeBuilder> typeStore = new HashMap<>();

	public API build() {
		createTypes();

		weaveFields();
		weaveMethods();

		createHierarchies();

		return getAPI();
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
									t.fields.add(builder);
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
							// Parameters different types and count
							IntStream.range(0, paramsCount + 1).forEach(methodParamsCount -> {
								var methodsParamsTypesForParamsCount = paramsCountToMethodsParamsTypes.get(methodParamsCount);
								if (methodParamsCount > 1) {
									methodsParamsTypesForParamsCount = List.of(methodsParamsTypesForParamsCount.get(methodCounter % methodsParamsTypesForParamsCount.size()));
								}

								methodsParamsTypesForParamsCount.forEach(methodParamsTypes -> {
									var parameters = new ArrayList<ParameterBuilder>();
									IntStream.range(0, methodParamsTypes.size()).forEach(paramIndex -> {
										var param = methodParamsTypes.get(paramIndex % methodParamsTypes.size());
										parameters.add(generateParameterBuilder("p" + paramIndex, param, false));
									});

									createMethodAndAddToType(visibility, modifiers, parameters, t);
								});
							});

							// Varargs
							IntStream.range(1, paramsCount + 1).forEach(methodParamsCount ->
									fieldTypes.forEach(varArgsParamType -> {
										var methodsParamsTypesForParamsCount = paramsCountToMethodsParamsTypes.get(methodParamsCount - 1);
										var methodParamsTypes = methodsParamsTypesForParamsCount.get(methodCounter % methodsParamsTypesForParamsCount.size());
										var parameters = new ArrayList<ParameterBuilder>();

										IntStream.range(0, methodParamsTypes.size()).forEach(paramIndex -> {
											var param = methodParamsTypes.get(paramIndex % methodParamsTypes.size());
											parameters.add(generateParameterBuilder("p" + paramIndex, param, false));
										});

										parameters.add(generateParameterBuilder("p" + methodParamsCount, varArgsParamType, true));

										createMethodAndAddToType(visibility, modifiers, parameters, t);
									})
							);

							// Overloading
							var overloadedQualifiedName = t.qualifiedName + ".m" + ++symbolCounter;
							IntStream.range(0, paramsCount + 1).forEach(methodParamsCount -> {
								var methodsParamsTypesForParamsCount = paramsCountToMethodsParamsTypes.get(methodParamsCount);
								var methodParamsTypes = methodsParamsTypesForParamsCount.get(methodCounter % methodsParamsTypesForParamsCount.size());
								var parameters = new ArrayList<ParameterBuilder>();

								IntStream.range(0, methodParamsTypes.size()).forEach(paramIndex -> {
									var param = methodParamsTypes.get(paramIndex % methodParamsTypes.size());
									parameters.add(generateParameterBuilder("p" + paramIndex, param, false));
								});

								createMethodAndAddToType(overloadedQualifiedName, visibility, modifiers, parameters, t);
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

	private API getAPI() {
		var types = new LibraryTypes(typeStore.values().stream().map(TypeBuilder::make).toList());
		return types.toAPI();
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

								var constructorVisibility = constructorsVisibilities.get(Math.ceilDiv(constructorCounter, thrownExceptions.size()) % constructorsVisibilities.size());
								var constructorExceptions = thrownExceptions.get(constructorCounter % thrownExceptions.size());
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
								recordBuilder.recordComponents.add(varArgsRecordComponentBuilder);

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
			var superClsIsSealed = superCls.isSealed();
			var atLeastOneImplementingInterfaceIsSealed = implementingIntfBuilders.stream().anyMatch(iB -> iB.make().isSealed());
			// Class extending sealed class or implementing at least one sealed interface must be sealed, non-sealed or final
			if ((superClsIsSealed || atLeastOneImplementingInterfaceIsSealed) && Sets.intersection(modifiers, Set.of(SEALED, NON_SEALED, FINAL)).isEmpty())
				return;
			// Class extending non-sealed class and implementing non-sealed interfaces can't be non-sealed
			if (!superClsIsSealed && !atLeastOneImplementingInterfaceIsSealed && modifiers.contains(NON_SEALED))
				return;

			topLevelVisibilities.forEach(visibility ->
					isHidingAndOverriding.forEach(isHidingAndOverriding -> {
						var clsBuilder = new ClassBuilder();
						clsBuilder.qualifiedName = "%s.C%s".formatted(apiPackageName, ++symbolCounter);
						clsBuilder.visibility = visibility;
						clsBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
						clsBuilder.superClass = typeReferenceFactory.createTypeReference(superCls.getQualifiedName());
						addConstructorToClassBuilder(clsBuilder, PUBLIC, List.of(), List.of(), false);

						if (superCls.isSealed()) {
							superClsBuilder.permittedTypes.add(clsBuilder.qualifiedName);
						}

						if (isHidingAndOverriding) {
							superCls.getDeclaredFields()
									.forEach(f -> clsBuilder.fields.add(generateFieldForTypeDeclBuilder(f, clsBuilder)));
						}

						var currentApi = getAPI();
						var methodsToGenerate = new HashMap<String, MethodBuilder>();
						if (isHidingAndOverriding) {
							superCls.getDeclaredMethods().stream()
									.filter(m -> !m.isFinal())
									.forEach(m -> methodsToGenerate.put(m.getSignature(), generateMethodForTypeDeclBuilder(m, clsBuilder)));
						} else if (!clsBuilder.modifiers.contains(ABSTRACT) && superCls.isAbstract()) {
							currentApi.getAllMethodsToImplement(superCls)
									.forEach(m -> methodsToGenerate.put(m.getSignature(), generateMethodForTypeDeclBuilder(m, clsBuilder)));
						}

						implementingIntfBuilders.forEach(implementingIntfBuilder -> {
							var implementingIntf = implementingIntfBuilder.make();

							clsBuilder.implementedInterfaces.add(typeReferenceFactory.createTypeReference(implementingIntf.getQualifiedName()));
							if (!clsBuilder.modifiers.contains(ABSTRACT)) {
								currentApi.getAllMethodsToImplement(implementingIntf)
										.forEach(m -> {
											if (!methodsToGenerate.containsKey(m.getSignature())) {
												methodsToGenerate.put(m.getSignature(), generateMethodForTypeDeclBuilder(m, clsBuilder));
											}
										});
							}

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
					if (depth == 0 && modifiers.contains(SEALED))
						return;
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
					addConstructorToClassBuilder(clsBuilder, PUBLIC, List.of(), List.of(), false);
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

	private void store(TypeBuilder type) {
		typeStore.put(type.qualifiedName, type);
	}

	private void addConstructorToClassBuilder(ClassBuilder classBuilder, AccessModifier visibility, List<ITypeReference> params,
													 List<ITypeReference> exceptions, boolean lastParamIsVarargs) {
		constructorCounter++;

		var constructorBuilder = new ConstructorBuilder();
		constructorBuilder.qualifiedName = classBuilder.qualifiedName;
		constructorBuilder.visibility = visibility;
		constructorBuilder.containingType = typeReferenceFactory.createTypeReference(classBuilder.qualifiedName);
		constructorBuilder.type = new PrimitiveTypeReference("void");
		constructorBuilder.thrownExceptions = exceptions;

		IntStream.range(0, params.size()).forEach(constructorParamTypeIndex -> {
			var constructorsParamType = params.get(constructorParamTypeIndex);
			boolean isVarargs = lastParamIsVarargs && constructorParamTypeIndex == params.size() - 1;
			constructorBuilder.parameters.add(generateParameterBuilder("c" + constructorParamTypeIndex, constructorsParamType, isVarargs));
		});

		if (lastParamIsVarargs) {
			var currentApi = getAPI();
			var currentConstructor = constructorBuilder.make();
			var constructorsWithSameErasure = classBuilder.constructors.stream()
					.filter(c -> currentApi.haveSameErasure(c.make(), currentConstructor))
					.toList();

			if (!constructorsWithSameErasure.isEmpty()) {
				if (constructorCounter % 4 == 0 || constructorCounter % 4 == 2) {
					constructorsWithSameErasure.forEach(c -> classBuilder.constructors.remove(c));
				} else {
					return;
				}
			}
		}

		classBuilder.constructors.add(constructorBuilder);
	}

	private void addImplementedInterfacesToTypeDeclBuilder(TypeBuilder builder, List<InterfaceBuilder> implementingIntfBuilders) {
		implementingIntfBuilders.forEach(implementingIntfBuilder -> {
			var implementingIntf = implementingIntfBuilder.make();

			builder.implementedInterfaces.add(typeReferenceFactory.createTypeReference(implementingIntf.getQualifiedName()));
			if (!builder.modifiers.contains(ABSTRACT)) {
				getAPI().getAllMethodsToImplement(implementingIntf)
						.forEach(m -> builder.methods.add(generateMethodForTypeDeclBuilder(m, builder)));
			}

			if (implementingIntf.isSealed()) {
				implementingIntfBuilder.permittedTypes.add(builder.qualifiedName);
			}
		});
	}

	private void addRecordComponentsToRecordBuilder(RecordBuilder recordBuilder, List<ITypeReference> recordsParamsTypes) {
		IntStream.range(0, recordsParamsTypes.size()).forEach(recordComponentTypeIndex -> {
			var recordComponentType = recordsParamsTypes.get(recordComponentTypeIndex);

			var recordComponentBuilder = new RecordComponentBuilder();
			recordComponentBuilder.qualifiedName = "%s.c%s".formatted(recordBuilder.qualifiedName, recordComponentTypeIndex);
			recordComponentBuilder.type = recordComponentType;
			recordComponentBuilder.containingType = typeReferenceFactory.createTypeReference(recordBuilder.qualifiedName);

			recordBuilder.recordComponents.add(recordComponentBuilder);
		});
	}

	private static void createMethodAndAddToType(AccessModifier visibility, Set<Modifier> modifiers, List<ParameterBuilder> parameters, TypeBuilder type) {
		createMethodAndAddToType(type.qualifiedName + ".m" + ++symbolCounter, visibility, modifiers, parameters, type);
	}

	private static void createMethodAndAddToType(String qualifiedName, AccessModifier visibility, Set<Modifier> modifiers, List<ParameterBuilder> parameters, TypeBuilder type) {
		var methodBuilder = new MethodBuilder();

		methodBuilder.visibility = visibility;
		methodBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
		methodBuilder.parameters = parameters;
		methodBuilder.containingType = typeReferenceFactory.createTypeReference(type.qualifiedName);
		methodBuilder.qualifiedName = qualifiedName;
		methodBuilder.type = methodReturnTypes.get(methodCounter % methodReturnTypes.size());
		methodBuilder.thrownExceptions = thrownExceptions.get(methodCounter % thrownExceptions.size());

		type.methods.add(methodBuilder);
		methodCounter++;
	}

	private static void addEnumValuesToEnumBuilder(EnumBuilder enumBuilder) {
		var enumTypeReference = typeReferenceFactory.createTypeReference(enumBuilder.qualifiedName);

		for (int i = 0; i < enumValuesCount; i++) {
			var enumValueBuilder = new EnumValueBuilder();
			enumValueBuilder.qualifiedName = "%s.V%s".formatted(enumBuilder.qualifiedName, i);
			enumValueBuilder.containingType = enumTypeReference;
			enumValueBuilder.type = enumTypeReference;

			enumBuilder.values.add(enumValueBuilder);
		}
	}

	private static FieldBuilder generateFieldForTypeDeclBuilder(FieldDecl field, TypeBuilder builder) {
		var typeDecl = builder.make();
		var fieldBuilder = new FieldBuilder();

		fieldBuilder.qualifiedName = builder.qualifiedName + "." + field.getSimpleName();
		fieldBuilder.visibility = field.getVisibility();
		fieldBuilder.modifiers = toEnumSet(field.getModifiers(), Modifier.class);
		fieldBuilder.containingType = typeReferenceFactory.createTypeReference(typeDecl.getQualifiedName());
		fieldBuilder.type = field.getType();

		return fieldBuilder;
	}

	private static MethodBuilder generateMethodForTypeDeclBuilder(MethodDecl method, TypeBuilder builder) {
		// @Override ann?
		var typeDecl = builder.make();
		var methodBuilder = new MethodBuilder();

		methodBuilder.qualifiedName = builder.qualifiedName + "." + method.getSimpleName();
		methodBuilder.visibility = method.getVisibility();
		methodBuilder.containingType = typeReferenceFactory.createTypeReference(typeDecl.getQualifiedName());
		methodBuilder.thrownExceptions = method.getThrownExceptions();
		methodBuilder.parameters.addAll(new ArrayList<>(method.getParameters().stream().map(ParameterBuilder::from).toList()));
		methodBuilder.type = method.getType();

		methodBuilder.modifiers = toEnumSet(method.getModifiers(), Modifier.class);
		if (!typeDecl.isAbstract())
			methodBuilder.modifiers.remove(ABSTRACT);
		if (typeDecl.isClass())
			methodBuilder.modifiers.remove(DEFAULT);

		return methodBuilder;
	}

	private static ParameterBuilder generateParameterBuilder(String name, ITypeReference type, boolean isVarargs) {
		var paramBuilder = new ParameterBuilder();

		paramBuilder.name = name;
		paramBuilder.type = type;
		paramBuilder.isVarargs = isVarargs;

		return paramBuilder;
	}

	private static List<AccessModifier> fieldVisibilities(Builder<TypeDecl> container) {
		return switch (container) {
			case InterfaceBuilder ignored -> List.of(PUBLIC);
			case RecordBuilder record -> record.recordComponents.isEmpty()
					? List.of(PUBLIC, PROTECTED)
					: List.of();
			default -> List.of(PUBLIC, PROTECTED);
		};
	}

	private static Set<Set<Modifier>> fieldModifiers(Builder<TypeDecl> container) {
		return switch (container) {
			case RecordBuilder ignored -> powerSet(STATIC, FINAL).stream()
					.filter(mods -> mods.contains(STATIC))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			default -> powerSet(STATIC, FINAL);
		};
	}

	private static List<AccessModifier> methodVisibilities(Builder<TypeDecl> container) {
		return switch (container) {
			case InterfaceBuilder ignored -> List.of(PUBLIC);
			case RecordBuilder record -> record.recordComponents.isEmpty()
					? List.of(PUBLIC, PROTECTED)
					: List.of();
			default -> List.of(PUBLIC, PROTECTED);
		};
	}

	private static Set<Set<Modifier>> methodModifiers(Builder<TypeDecl> container) {
		var modifiers = powerSet(STATIC, FINAL, ABSTRACT, DEFAULT, SYNCHRONIZED)
				.stream()
				.filter(mods -> !mods.containsAll(Set.of(FINAL, ABSTRACT)))
				.filter(mods -> !mods.contains(ABSTRACT) || Sets.intersection(mods, Set.of(STATIC, SYNCHRONIZED)).isEmpty())
				.filter(mods -> !mods.contains(DEFAULT) || Sets.intersection(mods, Set.of(STATIC, ABSTRACT)).isEmpty())
				.collect(Collectors.toCollection(LinkedHashSet::new));

		return switch (container) {
			case InterfaceBuilder ignored -> modifiers.stream()
					.filter(mods -> Sets.intersection(mods, Set.of(SYNCHRONIZED, FINAL)).isEmpty())
					.collect(Collectors.toCollection(LinkedHashSet::new));
			case RecordBuilder ignored -> modifiers.stream()
					.filter(mods -> Sets.intersection(mods, Set.of(ABSTRACT, DEFAULT)).isEmpty())
					.collect(Collectors.toCollection(LinkedHashSet::new));
			case ClassBuilder b -> modifiers.stream()
					.filter(mods -> !mods.contains(DEFAULT))
					.filter(mods -> b.make().isAbstract() || !mods.contains(ABSTRACT))
					.collect(Collectors.toCollection(LinkedHashSet::new));
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
