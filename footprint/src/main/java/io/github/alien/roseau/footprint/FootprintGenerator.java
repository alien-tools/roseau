package io.github.alien.roseau.footprint;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates a deterministic single-source footprint for all exported API symbols.
 */
public final class FootprintGenerator {
	private static final Comparator<TypeDecl> TYPE_COMPARATOR = Comparator.comparing(TypeDecl::getQualifiedName);
	private static final Comparator<ConstructorDecl> CONSTRUCTOR_COMPARATOR = Comparator.comparing(ExecutableDecl::getSignature);
	private static final Comparator<MethodDecl> METHOD_COMPARATOR = Comparator.comparing(ExecutableDecl::getSignature);
	private static final Comparator<FieldDecl> FIELD_COMPARATOR = Comparator.comparing(FieldDecl::getSimpleName);

	private final String packageName;
	private final String className;

	private API api;
	private int typeMethodCounter;
	private int localTypeCounter;
	private int localVariableCounter;
	private int boundWitnessCounter;
	private final Map<String, String> boundWitnessByKey = new LinkedHashMap<>();
	private final List<String> boundWitnessDeclarations = new ArrayList<>();

	private record TypeInstantiation(
		String typeExpression,
		Map<String, String> typeBindings,
		boolean parameterized
	) {
	}

	private record InvocationForm(
		String statement,
		Map<String, String> bindings
	) {
	}

	public FootprintGenerator(String packageName, String className) {
		this.packageName = Objects.requireNonNull(packageName);
		this.className = Objects.requireNonNull(className);
	}

	public String generate(API api) {
		this.api = Objects.requireNonNull(api);
		typeMethodCounter = 0;
		localTypeCounter = 0;
		localVariableCounter = 0;
		boundWitnessCounter = 0;
		boundWitnessByKey.clear();
		boundWitnessDeclarations.clear();

		List<TypeDecl> exportedTypes = api.getExportedTypes().stream()
			.sorted(TYPE_COMPARATOR)
			.toList();

		List<String> usageMethods = new ArrayList<>(exportedTypes.size());
		List<String> runCalls = new ArrayList<>(exportedTypes.size());
		for (TypeDecl type : exportedTypes) {
			String methodName = "useType" + (++typeMethodCounter);
			runCalls.add(methodName + "();");
			usageMethods.add(generateTypeMethod(methodName, type));
		}

		CodeBlock out = new CodeBlock(0);
		if (!packageName.isBlank()) {
			out.line("package " + packageName + ";");
			out.emptyLine();
		}

		out.line("public final class " + className + " {");
		out.indent();
		out.line("private " + className + "() {");
		out.indent();
		out.outdent();
		out.line("}");
		out.emptyLine();
		out.line("public static void main(String[] args) {");
		out.indent();
		out.line("run();");
		out.outdent();
		out.line("}");
		out.emptyLine();
		out.line("public static void run() {");
		out.indent();
		if (runCalls.isEmpty()) {
			out.line("// No exported symbol to exercise");
		} else {
			runCalls.forEach(out::line);
		}
		out.outdent();
		out.line("}");

		for (String declaration : boundWitnessDeclarations) {
			out.emptyLine();
			out.raw(declaration);
		}

		for (String usageMethod : usageMethods) {
			out.emptyLine();
			out.raw(usageMethod);
		}

		out.outdent();
		out.line("}");
		return out.toString();
	}

	private String generateTypeMethod(String methodName, TypeDecl type) {
		CodeBlock block = new CodeBlock(1);
		block.line("private static void " + methodName + "() {");
		block.indent();
		block.line("// Type symbol: " + type.getQualifiedName());

		if (!isDirectlyAccessible(type)) {
			block.line("// Unrepresentable from a foreign package: " + type.getQualifiedName());
			block.outdent();
			block.line("}");
			return block.toString();
		}

		String typeName = renderTypeName(type);
		String typeVar = nextLocal("typeRef");
		block.line(typeName + " " + typeVar + " = (" + typeName + ") null;");
		block.line("java.lang.Class<?> " + nextLocal("typeToken") + " = " + typeName + ".class;");
		if (!type.getFormalTypeParameters().isEmpty()) {
			Optional<String> parameterized = renderParameterizedType(type);
			if (parameterized.isPresent()) {
				String param = parameterized.get();
				block.line(param + " " + nextLocal("parameterizedRef") + " = (" + param + ") null;");
			} else {
				block.line("// Parameterized use not representable for " + type.getQualifiedName());
			}
		}

		if (type instanceof InterfaceDecl itf) {
			emitInterfaceTypeUsage(block, itf);
		}

		if (type instanceof ClassDecl cls) {
			emitClassTypeUsage(block, cls);
			if (isThrowableType(cls)) {
				emitThrowableTypeUsage(block, cls);
			}
		}

		emitFieldUsages(block, type);
		emitMethodUsages(block, type);

		if (type instanceof EnumDecl enm) {
			emitEnumValueUsages(block, enm);
		}

		if (type instanceof AnnotationDecl ann) {
			emitAnnotationMethodUsages(block, ann);
		}

		block.outdent();
		block.line("}");
		return block.toString();
	}

	private void emitInterfaceTypeUsage(CodeBlock block, InterfaceDecl itf) {
		TypeInstantiation typeInstantiation = resolveTypeInstantiation(itf);
		if (itf.isAnnotation()) {
			block.line("// Annotation interfaces are referenced and their members are invoked as interface methods");
			return;
		}
		List<MethodDecl> toImplement = sortedMethods(api.getAllMethodsToImplement(itf));
		Map<String, String> instantiationBindings = typeInstantiation.typeBindings();
		if (typeInstantiation.parameterized() && toImplement.stream()
			.anyMatch(method -> !canRenderMethodInSubclassContext(method, instantiationBindings))) {
			typeInstantiation = rawTypeInstantiation(itf);
		}
		String typeName = typeInstantiation.typeExpression();
		boolean preserveMethodTypeParameters = typeInstantiation.parameterized() || itf.getFormalTypeParameters().isEmpty();

		if (canExtendInterface(itf)) {
			String extensionName = nextLocalType("Extended");
			block.line("interface " + extensionName + " extends " + typeName + " {");
			block.line("}");
		} else if (itf.isSealed()) {
			block.line("// Cannot extend sealed interface from generated footprint: " + itf.getQualifiedName());
		}

		if (canImplementInterface(itf)) {
			if (toImplement.stream().allMatch(this::isRepresentable)) {
				String implVar = nextLocal("impl");
				block.line("try {");
				block.indent();
				block.line(typeName + " " + implVar + " = new " + typeName + "() {");
				block.indent();
				for (MethodDecl method : toImplement) {
					emitMethodImplementation(block, method, typeInstantiation.typeBindings(), preserveMethodTypeParameters);
				}
				block.outdent();
				block.line("};");
				block.outdent();
				block.line("} catch (java.lang.RuntimeException ignored) {");
				block.line("}");
			} else {
				block.line("// Interface implementation is unrepresentable due to inaccessible method types: "
					+ itf.getQualifiedName());
			}
		} else if (itf.isSealed()) {
			block.line("// Cannot implement sealed interface from generated footprint: " + itf.getQualifiedName());
		}
	}

	private void emitClassTypeUsage(CodeBlock block, ClassDecl cls) {
		if (cls.isEnum()) {
			block.line("// Enums cannot be instantiated with constructors from client code");
			return;
		}

		String typeName = renderTypeName(cls);
		TypeInstantiation classInstantiation = resolveTypeInstantiation(cls);
		String instantiatedType = classInstantiation.typeExpression();
		boolean preserveMethodTypeParameters = classInstantiation.parameterized() || cls.getFormalTypeParameters().isEmpty();
		List<ConstructorDecl> constructors = sortedConstructors(cls.getDeclaredConstructors());
		for (ConstructorDecl constructor : constructors) {
			if (constructor.isPublic() && !cls.isEffectivelyAbstract() && isRepresentable(constructor)) {
				List<String> invocations = directConstructorInvocations(cls, instantiatedType, constructor,
					classInstantiation.typeBindings());
				if (!invocations.isEmpty()) {
					for (String invocation : invocations) {
						emitInvocation(block, invocation, constructor);
					}
					if (!isNonStaticNestedClass(cls)) {
						emitConstructorReference(block, cls, constructor, instantiatedType, classInstantiation.typeBindings());
					}
				} else {
					block.line("// Constructor cannot be invoked directly in this footprint context: " + constructor.getQualifiedName());
				}
			} else if (constructor.isPublic() && !isRepresentable(constructor)) {
				block.line("// Constructor uses inaccessible parameter types: " + constructor.getQualifiedName());
			}
		}

		if (canExtendClass(cls)) {
			emitClassExtensionUsage(block, cls);
		} else if (api.isEffectivelyFinal(cls) || cls.isSealed() || cls.isRecord() || cls.isEnum()) {
			block.line("// Class extension is illegal for: " + cls.getQualifiedName());
		}

		if (cls.isAbstract() && canExtendClass(cls)) {
			List<MethodDecl> toImplement = sortedMethods(api.getAllMethodsToImplement(cls));
			ConstructorDecl baseCtor = findSubclassAccessibleConstructors(cls).stream().findFirst().orElse(null);
				if (baseCtor != null && toImplement.stream().allMatch(method ->
					isRepresentable(method) && canRenderMethodInSubclassContext(method, classInstantiation.typeBindings()))) {
					List<String> invocations = directConstructorInvocations(cls, instantiatedType, baseCtor,
						classInstantiation.typeBindings());
					if (!invocations.isEmpty()) {
						String invocation = invocations.getFirst();
						String anonVar = nextLocal("anonImpl");
						block.line("try {");
						block.indent();
						block.line(instantiatedType + " " + anonVar + " = " + invocation.replace(";", "") + " {");
						block.indent();
						for (MethodDecl method : toImplement) {
							emitMethodImplementation(block, method, classInstantiation.typeBindings(), preserveMethodTypeParameters);
						}
						block.outdent();
						block.line("};");
						block.outdent();
						block.line("}");
						for (TypeReference<?> checkedType : checkedExceptionTypes(baseCtor)) {
							block.line("catch (" + renderRawType(checkedType) + " ignored) {");
							block.line("}");
						}
						block.line("catch (java.lang.RuntimeException ignored) {");
						block.line("}");
					} else {
						block.line("// Cannot build anonymous implementation for " + cls.getQualifiedName());
					}
				} else if (baseCtor != null) {
				block.line("// Anonymous implementation is unrepresentable due to inaccessible method types: "
					+ cls.getQualifiedName());
			}
		}
	}

	private void emitClassExtensionUsage(CodeBlock block, ClassDecl cls) {
		String subclassName = nextLocalType("ExtendedClass");
		TypeInstantiation superType = resolveTypeInstantiation(cls);
		String typeName = renderTypeName(cls);
		String superTypeExpression = superType.typeExpression();
		Map<String, String> superTypeBindings = superType.typeBindings();
		boolean preserveMethodTypeParameters = superType.parameterized() || cls.getFormalTypeParameters().isEmpty();
		List<ConstructorDecl> accessibleConstructors = findSubclassAccessibleConstructors(cls);
		if (accessibleConstructors.isEmpty()) {
			block.line("// No subclass-accessible constructor for " + cls.getQualifiedName());
			return;
		}

		List<MethodDecl> methodsToImplement = sortedMethods(api.getAllMethodsToImplement(cls));
		boolean concreteSubclass = methodsToImplement.stream().allMatch(method ->
			isRepresentable(method) && canRenderMethodInSubclassContext(method, superTypeBindings));
		String classKind = concreteSubclass ? "class" : "abstract class";
		block.line(classKind + " " + subclassName + " extends " + superTypeExpression + " {");
		block.indent();
		List<Integer> emittedConstructors = new ArrayList<>();
		for (int i = 0; i < accessibleConstructors.size(); i++) {
			ConstructorDecl constructor = accessibleConstructors.get(i);
			Optional<Map<String, String>> resolvedConstructorBindings = resolveTypeParameterBindings(
				constructor.getFormalTypeParameters(), superTypeBindings);
			if (!constructor.getFormalTypeParameters().isEmpty() && resolvedConstructorBindings.isEmpty()) {
				block.line("// Super-constructor type arguments are unrepresentable for " + constructor.getQualifiedName());
				continue;
			}
			Map<String, String> constructorBindings = resolvedConstructorBindings
				.orElseGet(() -> new LinkedHashMap<>(superTypeBindings));

			String marker = markerParameters(i);
			String throwsClause = renderThrowsClause(constructor);
				String signature = marker.isBlank()
					? subclassName + "()" + throwsClause
					: subclassName + "(" + marker + ")" + throwsClause;
				block.line(signature + " {");
				block.indent();
				String args = renderArguments(constructor.getParameters(), false, constructorBindings);
				String superCall = "super(" + args + ");";
				if (!constructor.getFormalTypeParameters().isEmpty()) {
					String explicitTypeArgs = constructor.getFormalTypeParameters().stream()
						.map(parameter -> constructorBindings.get(parameter.name()))
						.collect(Collectors.joining(", "));
					superCall = "<" + explicitTypeArgs + ">super(" + args + ");";
				}
				block.line(superCall);
				block.outdent();
				block.line("}");
				emittedConstructors.add(i);
			}

		List<FieldDecl> protectedFields = sortedFields(cls.getDeclaredFields()).stream()
			.filter(FieldDecl::isProtected)
			.filter(field -> {
				if (isRepresentable(field.getType())) {
					return true;
				}
				block.line("// Protected field uses inaccessible type: " + field.getQualifiedName());
				return false;
			})
			.toList();
		List<MethodDecl> protectedMethods = sortedMethods(cls.getDeclaredMethods()).stream()
			.filter(MethodDecl::isProtected)
			.filter(method -> {
				if (isRepresentable(method)) {
					return true;
				}
				block.line("// Protected method uses inaccessible types: " + method.getQualifiedName());
				return false;
			})
			.toList();
		if (!protectedFields.isEmpty() || !protectedMethods.isEmpty()) {
			block.line("void useProtectedMembers() {");
			block.indent();
			for (FieldDecl field : protectedFields) {
				if (field.isStatic()) {
					emitRuntimeStatement(block, renderType(field.getType(), superTypeBindings) + " " + nextLocal("protectedRead") +
						" = " + typeName + "." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, typeName + "." + field.getSimpleName() + " = " +
							typedDefaultValueExpression(field.getType(), superTypeBindings) + ";");
					}
				} else {
					emitRuntimeStatement(block, renderType(field.getType(), superTypeBindings) + " " + nextLocal("protectedRead") +
						" = this." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, "this." + field.getSimpleName() + " = " +
							typedDefaultValueExpression(field.getType(), superTypeBindings) + ";");
					}
				}
			}

			for (MethodDecl method : protectedMethods) {
				String receiver = method.isStatic() ? typeName : "this";
				List<InvocationForm> invocations = invocationForms(method, receiver, superTypeBindings);
				for (InvocationForm invocation : invocations) {
					emitInvocation(block, invocation.statement(), method);
				}
				if (invocations.isEmpty() && !method.getFormalTypeParameters().isEmpty()) {
					block.line("// Explicit type arguments unrepresentable for " + method.getQualifiedName());
				}

				emitMethodReference(block, method, receiver, superTypeBindings);
			}
			block.outdent();
			block.line("}");
		}

		Set<String> implementedSignatures = new HashSet<>();
		if (concreteSubclass) {
			for (MethodDecl method : methodsToImplement) {
				emitMethodImplementation(block, method, superTypeBindings, preserveMethodTypeParameters);
				implementedSignatures.add(method.getSignature());
			}
		} else if (!methodsToImplement.isEmpty()) {
			block.line("// Concrete extension is unrepresentable due to inaccessible method signatures");
		}
		List<MethodDecl> overridable = sortedMethods(cls.getDeclaredMethods()).stream()
			.filter(this::isOverridable)
			.filter(this::isRepresentable)
			.filter(method -> canRenderMethodInSubclassContext(method, superTypeBindings))
			.filter(method -> !implementedSignatures.contains(method.getSignature()))
			.toList();
		for (MethodDecl method : overridable) {
			emitOverrideMethod(block, method, superTypeBindings, preserveMethodTypeParameters);
		}

		block.outdent();
		block.line("}");
		if (concreteSubclass) {
			for (Integer constructorIndex : emittedConstructors) {
				ConstructorDecl constructor = accessibleConstructors.get(constructorIndex);
				String markerArgs = markerArguments(constructorIndex);
				String invocation = markerArgs.isBlank()
					? "new " + subclassName + "();"
					: "new " + subclassName + "(" + markerArgs + ");";
				emitInvocation(block, invocation, constructor);
			}
			if (emittedConstructors.isEmpty()) {
				block.line("// No constructible subclass constructor could be emitted for " + cls.getQualifiedName());
			}
		} else {
			block.line(subclassName + " " + nextLocal("extendedRef") + " = null;");
		}
	}

	private void emitFieldUsages(CodeBlock block, TypeDecl type) {
		if (!isDirectlyAccessible(type)) {
			return;
		}
		String typeName = renderTypeName(type);
		TypeInstantiation typeInstantiation = resolveTypeInstantiation(type);
		Map<String, String> typeBindings = typeInstantiation.typeBindings();
		String instanceReceiver = "((" + typeInstantiation.typeExpression() + ") null)";

		for (FieldDecl field : sortedFields(type.getDeclaredFields())) {
			if (!isRepresentable(field.getType())) {
				block.line("// Field uses inaccessible type from this footprint package: " + field.getQualifiedName());
				continue;
			}
			String fieldType = renderType(field.getType(), typeBindings);
			if (field.isPublic()) {
				if (field.isStatic()) {
					emitRuntimeStatement(block, fieldType + " " + nextLocal("readField") + " = " + typeName + "." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, typeName + "." + field.getSimpleName() + " = " +
							typedDefaultValueExpression(field.getType(), typeBindings) + ";");
					}
				} else {
					emitRuntimeStatement(block, fieldType + " " + nextLocal("readField") + " = " + instanceReceiver + "." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, instanceReceiver + "." + field.getSimpleName() + " = " +
							typedDefaultValueExpression(field.getType(), typeBindings) + ";");
					}
				}
			} else if (field.isProtected() && !canExtendClass(type)) {
				block.line("// Protected field cannot be accessed without a legal subclass: " + field.getQualifiedName());
			}
		}
	}

	private void emitMethodUsages(CodeBlock block, TypeDecl type) {
		if (!isDirectlyAccessible(type)) {
			return;
		}
		String typeName = renderTypeName(type);
		TypeInstantiation typeInstantiation = resolveTypeInstantiation(type);
		Map<String, String> typeBindings = typeInstantiation.typeBindings();
		String instanceReceiver = "((" + typeInstantiation.typeExpression() + ") null)";

		for (MethodDecl method : sortedMethods(type.getDeclaredMethods())) {
			if (!isRepresentable(method)) {
				block.line("// Method uses inaccessible types from this footprint package: " + method.getQualifiedName());
				continue;
			}
			if (method.isPublic()) {
				String receiver = method.isStatic() ? typeName : instanceReceiver;
				List<InvocationForm> invocations = invocationForms(method, receiver, typeBindings);
				for (InvocationForm invocation : invocations) {
					emitInvocation(block, invocation.statement(), method);
				}
				if (invocations.isEmpty() && !method.getFormalTypeParameters().isEmpty()) {
					block.line("// Explicit type arguments unrepresentable for " + method.getQualifiedName());
				}
				emitMethodReference(block, method, receiver, typeBindings);
			} else if (method.isProtected() && !canExtendClass(type)) {
				block.line("// Protected method cannot be accessed without a legal subclass: " + method.getQualifiedName());
			}
		}
	}

	private void emitEnumValueUsages(CodeBlock block, EnumDecl enm) {
		String typeName = renderTypeName(enm);
		for (EnumValueDecl value : enm.getValues().stream().sorted(Comparator.comparing(EnumValueDecl::getSimpleName)).toList()) {
			block.line(typeName + " " + nextLocal("enumValue") + " = " + typeName + "." + value.getSimpleName() + ";");
		}
	}

	private void emitAnnotationMethodUsages(CodeBlock block, AnnotationDecl ann) {
		String typeName = renderTypeName(ann);
		for (AnnotationMethodDecl method : ann.getAnnotationMethods().stream().sorted(METHOD_COMPARATOR).toList()) {
			if (!isRepresentable(method)) {
				block.line("// Annotation method uses inaccessible types: " + method.getQualifiedName());
				continue;
			}
			Map<String, String> typeParameterErasures = typeParameterErasures(method);
			String receiver = "((" + typeName + ") null)";
			List<InvocationForm> invocations = invocationForms(method, receiver, typeParameterErasures);
			for (InvocationForm invocation : invocations) {
				emitInvocation(block, invocation.statement(), method);
			}
			if (invocations.isEmpty() && !method.getFormalTypeParameters().isEmpty()) {
				block.line("// Explicit type arguments unrepresentable for " + method.getQualifiedName());
			}
			emitMethodReference(block, method, receiver, typeParameterErasures);
		}
	}

	private void emitMethodReference(CodeBlock block, MethodDecl method, String receiver,
	                                 Map<String, String> typeParameterErasures) {
		if (!canEmitMethodReference(method)) {
			block.line("// Method reference is ambiguous or unrepresentable for " + method.getQualifiedName());
			return;
		}

		Optional<Map<String, String>> resolvedBindings = resolveTypeParameterBindings(method.getFormalTypeParameters(),
			typeParameterErasures);
		if (!method.getFormalTypeParameters().isEmpty() && resolvedBindings.isEmpty()) {
			block.line("// Method reference generic type arguments are unrepresentable for " + method.getQualifiedName());
			return;
		}
		Map<String, String> bindings = resolvedBindings.orElseGet(() -> new LinkedHashMap<>(typeParameterErasures));

		String methodRefType = nextLocalType("MethodRef");
		String methodRefVar = nextLocal("methodRef");
		List<String> params = new ArrayList<>(method.getParameters().size());
		for (int i = 0; i < method.getParameters().size(); i++) {
			ParameterDecl parameter = method.getParameters().get(i);
			String pType = parameter.isVarargs()
				? renderRawType(parameter.type(), bindings) + "[]"
				: renderType(parameter.type(), bindings);
			params.add(pType + " p" + i);
		}

		block.line("@java.lang.FunctionalInterface");
		block.line("interface " + methodRefType + " {");
		block.indent();
		block.line(renderType(method.getType(), bindings) + " invoke(" + String.join(", ", params) + ")" + renderThrowsClause(method) + ";");
		block.outdent();
		block.line("}");
		String target = receiver + "::" + method.getSimpleName();
		if (!method.getFormalTypeParameters().isEmpty()) {
			String explicit = method.getFormalTypeParameters().stream()
				.map(parameter -> bindings.get(parameter.name()))
				.collect(Collectors.joining(", "));
			target = receiver + "::<" + explicit + ">" + method.getSimpleName();
		}
		emitRuntimeStatement(block, methodRefType + " " + methodRefVar + " = " + target + ";");
	}

	private void emitMethodImplementation(CodeBlock block, MethodDecl method) {
		emitMethodImplementation(block, method, typeParameterErasures(method), true);
	}

	private void emitMethodImplementation(CodeBlock block, MethodDecl method, Map<String, String> typeParameterErasures) {
		emitMethodImplementation(block, method, typeParameterErasures, true);
	}

	private void emitMethodImplementation(CodeBlock block, MethodDecl method, Map<String, String> typeParameterErasures,
	                                      boolean preserveMethodTypeParameters) {
		Map<String, String> declarationBindings = new LinkedHashMap<>(typeParameterErasures);
		String methodTypeParameters;
		if (preserveMethodTypeParameters) {
			for (FormalTypeParameter typeParameter : method.getFormalTypeParameters()) {
				declarationBindings.put(typeParameter.name(), typeParameter.name());
			}
			methodTypeParameters = renderFormalTypeParameters(method.getFormalTypeParameters(), typeParameterErasures);
		} else {
			addTypeParameterErasures(method.getFormalTypeParameters(), declarationBindings);
			methodTypeParameters = "";
		}

		String returnType = preserveMethodTypeParameters
			? renderType(method.getType(), declarationBindings)
			: renderRawType(method.getType(), declarationBindings);
		String throwsClause = renderThrowsClause(method);
		List<String> params = new ArrayList<>(method.getParameters().size());
		for (int i = 0; i < method.getParameters().size(); i++) {
			ParameterDecl parameter = method.getParameters().get(i);
			String paramType = preserveMethodTypeParameters
				? renderType(parameter.type(), declarationBindings)
				: renderRawType(parameter.type(), declarationBindings);
			if (parameter.isVarargs()) {
				paramType += "...";
			}
			params.add(paramType + " p" + i);
		}

		block.line("@Override");
		block.line("public " + methodTypeParameters + returnType + " " + method.getSimpleName() + "(" + String.join(", ", params) + ")" + throwsClause + " {");
		block.indent();
		if ("void".equals(returnType)) {
			block.line("return;");
		} else {
			block.line("return " + typedDefaultValueExpression(method.getType(), declarationBindings,
				!preserveMethodTypeParameters) + ";");
		}
		block.outdent();
		block.line("}");
	}

	private void emitOverrideMethod(CodeBlock block, MethodDecl method) {
		emitOverrideMethod(block, method, typeParameterErasures(method), true);
	}

	private void emitOverrideMethod(CodeBlock block, MethodDecl method, Map<String, String> typeParameterErasures) {
		emitOverrideMethod(block, method, typeParameterErasures, true);
	}

	private void emitOverrideMethod(CodeBlock block, MethodDecl method, Map<String, String> typeParameterErasures,
	                                boolean preserveMethodTypeParameters) {
		Map<String, String> declarationBindings = new LinkedHashMap<>(typeParameterErasures);
		String methodTypeParameters;
		if (preserveMethodTypeParameters) {
			for (FormalTypeParameter typeParameter : method.getFormalTypeParameters()) {
				declarationBindings.put(typeParameter.name(), typeParameter.name());
			}
			methodTypeParameters = renderFormalTypeParameters(method.getFormalTypeParameters(), typeParameterErasures);
		} else {
			addTypeParameterErasures(method.getFormalTypeParameters(), declarationBindings);
			methodTypeParameters = "";
		}

		String returnType = preserveMethodTypeParameters
			? renderType(method.getType(), declarationBindings)
			: renderRawType(method.getType(), declarationBindings);
		String throwsClause = renderThrowsClause(method);
		List<String> params = new ArrayList<>(method.getParameters().size());
		for (int i = 0; i < method.getParameters().size(); i++) {
			ParameterDecl parameter = method.getParameters().get(i);
			String paramType = preserveMethodTypeParameters
				? renderType(parameter.type(), declarationBindings)
				: renderRawType(parameter.type(), declarationBindings);
			if (parameter.isVarargs()) {
				paramType += "...";
			}
			params.add(paramType + " p" + i);
		}

		block.line("@Override");
		block.line("public " + methodTypeParameters + returnType + " " + method.getSimpleName() + "(" + String.join(", ", params) + ")" + throwsClause + " {");
		block.indent();
		if ("void".equals(returnType)) {
			block.line("return;");
		} else {
			block.line("return " + typedDefaultValueExpression(method.getType(), declarationBindings,
				!preserveMethodTypeParameters) + ";");
		}
		block.outdent();
		block.line("}");
	}

	private void emitRuntimeStatement(CodeBlock block, String statement) {
		block.line("try { " + statement + " } catch (java.lang.RuntimeException ignored) {");
		block.line("}");
	}

	private void emitInvocation(CodeBlock block, String invocationStatement, ExecutableDecl executable) {
		List<TypeReference<?>> checked = checkedExceptionTypes(executable);
		block.line("try { " + invocationStatement + " }");
		for (TypeReference<?> checkedType : checked) {
			block.line("catch (" + renderRawType(checkedType) + " ignored) {");
			block.line("}");
		}
		if (!coversRuntimeException(checked)) {
			block.line("catch (java.lang.RuntimeException ignored) {");
			block.line("}");
		}
	}

	private void emitThrowableTypeUsage(CodeBlock block, ClassDecl cls) {
		String typeName = renderTypeName(cls);
		String helperName = nextLocalType("ThrowableUsage");
		block.line("class " + helperName + " {");
		block.indent();
		block.line("void declared() throws " + typeName + " {");
		block.line("}");
		block.line("void thrownAndCaught() {");
		block.indent();
		block.line("try {");
		block.indent();
		block.line("if (false) {");
		block.indent();
		block.line("throw (" + typeName + ") null;");
		block.outdent();
		block.line("}");
		block.outdent();
		block.line("} catch (" + typeName + " ignored) {");
		block.line("}");
		block.outdent();
		block.line("}");
		block.outdent();
		block.line("}");
		block.line(helperName + " " + nextLocal("throwableUsage") + " = new " + helperName + "();");
	}

	private void emitConstructorReference(CodeBlock block, ClassDecl cls, ConstructorDecl constructor,
	                                      String instantiatedType, Map<String, String> containingBindings) {
		Optional<Map<String, String>> resolvedConstructorBindings = resolveTypeParameterBindings(
			constructor.getFormalTypeParameters(), containingBindings);
		if (!constructor.getFormalTypeParameters().isEmpty() && resolvedConstructorBindings.isEmpty()) {
			block.line("// Constructor reference generic type arguments are unrepresentable for " + constructor.getQualifiedName());
			return;
		}
		Map<String, String> invocationBindings = resolvedConstructorBindings
			.orElseGet(() -> new LinkedHashMap<>(containingBindings));

		String refType = nextLocalType("CtorRef");
		String refVar = nextLocal("ctorRef");
		List<String> params = new ArrayList<>(constructor.getParameters().size());
		for (int i = 0; i < constructor.getParameters().size(); i++) {
			ParameterDecl parameter = constructor.getParameters().get(i);
			String pType = parameter.isVarargs()
				? renderRawType(parameter.type(), invocationBindings) + "[]"
				: renderType(parameter.type(), invocationBindings);
			params.add(pType + " p" + i);
		}
		block.line("@java.lang.FunctionalInterface");
		block.line("interface " + refType + " {");
		block.indent();
		block.line(instantiatedType + " create(" + String.join(", ", params) + ")" + renderThrowsClause(constructor) + ";");
		block.outdent();
		block.line("}");
		String target = instantiatedType + "::new";
		if (!constructor.getFormalTypeParameters().isEmpty()) {
			String explicitTypeArgs = constructor.getFormalTypeParameters().stream()
				.map(parameter -> invocationBindings.get(parameter.name()))
				.collect(Collectors.joining(", "));
			target = instantiatedType + "::<" + explicitTypeArgs + ">new";
		}
		emitRuntimeStatement(block, refType + " " + refVar + " = " + target + ";");
	}

	private List<InvocationForm> invocationForms(MethodDecl method, String receiver, Map<String, String> containingTypeBindings) {
		Optional<Map<String, String>> resolvedBindings = resolveTypeParameterBindings(method.getFormalTypeParameters(),
			containingTypeBindings);
		if (!method.getFormalTypeParameters().isEmpty() && resolvedBindings.isEmpty()) {
			return List.of();
		}

		Map<String, String> invocationBindings = resolvedBindings.orElseGet(() -> new LinkedHashMap<>(containingTypeBindings));
		String inferredPrefix = receiver + "." + method.getSimpleName();
		List<InvocationForm> forms = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();

		String inferredArgs = renderArguments(method.getParameters(), false, invocationBindings);
		String inferredInvocation = inferredPrefix + "(" + inferredArgs + ");";
		if (seen.add(inferredInvocation)) {
			forms.add(new InvocationForm(inferredInvocation, invocationBindings));
		}
		if (method.isVarargs()) {
			String varargsInvocation = inferredPrefix + "(" + renderArguments(method.getParameters(), true, invocationBindings) + ");";
			if (seen.add(varargsInvocation)) {
				forms.add(new InvocationForm(varargsInvocation, invocationBindings));
			}
		}

		if (!method.getFormalTypeParameters().isEmpty()) {
			List<String> explicitArgs = method.getFormalTypeParameters().stream()
				.map(parameter -> invocationBindings.get(parameter.name()))
				.toList();
			String explicitPrefix = receiver + ".<" + String.join(", ", explicitArgs) + ">" + method.getSimpleName();
			String explicitInvocation = explicitPrefix + "(" + inferredArgs + ");";
			if (seen.add(explicitInvocation)) {
				forms.add(new InvocationForm(explicitInvocation, invocationBindings));
			}
			if (method.isVarargs()) {
				String explicitVarargs = explicitPrefix + "(" + renderArguments(method.getParameters(), true, invocationBindings) + ");";
				if (seen.add(explicitVarargs)) {
					forms.add(new InvocationForm(explicitVarargs, invocationBindings));
				}
			}
		}

		return forms;
	}

	private Optional<String> renderExplicitMethodTypeArguments(MethodDecl method, Map<String, String> typeParameterErasures) {
		Optional<Map<String, String>> bindings = resolveTypeParameterBindings(method.getFormalTypeParameters(),
			typeParameterErasures);
		if (bindings.isEmpty()) {
			return Optional.empty();
		}
		List<String> args = method.getFormalTypeParameters().stream()
			.map(parameter -> bindings.get().get(parameter.name()))
			.toList();
		return Optional.of(String.join(", ", args));
	}

	private Optional<Map<String, String>> resolveTypeParameterBindings(List<FormalTypeParameter> formalTypeParameters,
	                                                                  Map<String, String> seedBindings) {
		Map<String, String> bindings = new LinkedHashMap<>(seedBindings);
		for (FormalTypeParameter parameter : formalTypeParameters) {
			Optional<String> explicit = explicitTypeArgument(parameter, bindings);
			if (explicit.isEmpty()) {
				return Optional.empty();
			}
			bindings.put(parameter.name(), explicit.get());
		}
		return Optional.of(bindings);
	}

	private TypeInstantiation resolveTypeInstantiation(TypeDecl type) {
		String rawTypeName = renderTypeName(type);
		if (type.getFormalTypeParameters().isEmpty()) {
			return new TypeInstantiation(rawTypeName, Map.of(), false);
		}

		Map<String, String> erasures = new LinkedHashMap<>();
		addTypeParameterErasures(type.getFormalTypeParameters(), erasures);
		Optional<Map<String, String>> concrete = resolveTypeParameterBindings(type.getFormalTypeParameters(), Map.of());
		if (concrete.isEmpty()) {
			return new TypeInstantiation(rawTypeName, erasures, false);
		}

		List<String> arguments = type.getFormalTypeParameters().stream()
			.map(parameter -> concrete.get().get(parameter.name()))
			.toList();
		return new TypeInstantiation(rawTypeName + "<" + String.join(", ", arguments) + ">",
			Map.copyOf(concrete.get()), true);
	}

	private TypeInstantiation rawTypeInstantiation(TypeDecl type) {
		Map<String, String> erasures = new LinkedHashMap<>();
		addTypeParameterErasures(type.getFormalTypeParameters(), erasures);
		return new TypeInstantiation(renderTypeName(type), Map.copyOf(erasures), false);
	}

	private Optional<String> renderParameterizedType(TypeDecl type) {
		if (type.getFormalTypeParameters().isEmpty()) {
			return Optional.empty();
		}

		Map<String, String> typeParameterErasures = new LinkedHashMap<>();
		addTypeParameterErasures(type.getFormalTypeParameters(), typeParameterErasures);

		List<String> args = new ArrayList<>(type.getFormalTypeParameters().size());
		for (FormalTypeParameter parameter : type.getFormalTypeParameters()) {
			Optional<String> rendered = parameterizedTypeArgument(parameter, typeParameterErasures);
			if (rendered.isEmpty()) {
				return Optional.empty();
			}
			args.add(rendered.get());
		}
		return Optional.of(renderTypeName(type) + "<" + String.join(", ", args) + ">");
	}

	private Optional<String> parameterizedTypeArgument(FormalTypeParameter parameter, Map<String, String> typeParameterErasures) {
		List<ITypeReference> bounds = parameter.bounds();
		Optional<String> witness = createIntersectionWitness(parameter, typeParameterErasures);
		if (witness.isPresent()) {
			return witness;
		}
		if (containsTypeParameter(bounds, parameter.name())) {
			return Optional.empty();
		}

		ITypeReference erasureBound = selectErasureBound(bounds);
		if (erasureBound.equals(TypeReference.OBJECT)) {
			return Optional.of("java.lang.Object");
		}
		if (!erasureBoundSatisfiesAllBounds(erasureBound, bounds)) {
			return Optional.empty();
		}
		if (erasureBound instanceof TypeParameterReference tpr) {
			String resolved = typeParameterErasures.get(tpr.name());
			return resolved == null ? Optional.empty() : Optional.of(resolved);
		}
		return Optional.of(renderType(erasureBound, typeParameterErasures));
	}

	private Optional<String> explicitTypeArgument(FormalTypeParameter parameter, Map<String, String> typeParameterErasures) {
		List<ITypeReference> bounds = parameter.bounds();
		Optional<String> witness = createIntersectionWitness(parameter, typeParameterErasures);
		if (witness.isPresent()) {
			return witness;
		}
		if (containsTypeParameter(bounds, parameter.name())) {
			return Optional.empty();
		}

		ITypeReference erasureBound = selectErasureBound(bounds);
		if (erasureBound.equals(TypeReference.OBJECT)) {
			return Optional.of("java.lang.Object");
		}
		if (!erasureBoundSatisfiesAllBounds(erasureBound, bounds)) {
			return Optional.empty();
		}
		if (erasureBound instanceof TypeParameterReference tpr) {
			String resolved = typeParameterErasures.get(tpr.name());
			return resolved == null ? Optional.empty() : Optional.of(resolved);
		}
		return Optional.of(renderType(erasureBound, typeParameterErasures));
	}

	private List<ConstructorDecl> findSubclassAccessibleConstructors(ClassDecl cls) {
		return sortedConstructors(cls.getDeclaredConstructors()).stream()
			.filter(c -> c.isPublic() || c.isProtected())
			.filter(this::isRepresentable)
			.toList();
	}

	private boolean isDirectlyAccessible(TypeDecl type) {
		// A type in the default package cannot be referenced from a class in another package.
		if (type.getPackageName().isEmpty()) {
			return false;
		}

		if (!type.isPublic()) {
			return false;
		}

		Optional<TypeReference<TypeDecl>> enclosing = type.getEnclosingType();
		while (enclosing.isPresent()) {
			Optional<TypeDecl> resolved = api.resolver().resolve(enclosing.get());
			if (resolved.isEmpty() || !resolved.get().isPublic()) {
				return false;
			}
			enclosing = resolved.get().getEnclosingType();
		}

		return true;
	}

	private boolean canExtendClass(TypeDecl type) {
		if (!(type instanceof ClassDecl cls)) {
			return false;
		}
		if (cls.isRecord() || cls.isEnum()) {
			return false;
		}
		if (!isDirectlyAccessible(cls)) {
			return false;
		}
		if (cls.isSealed() || api.isEffectivelyFinal(cls)) {
			return false;
		}
		if (isNonStaticNestedClass(cls)) {
			return false;
		}
		return !findSubclassAccessibleConstructors(cls).isEmpty();
	}

	private boolean canExtendInterface(InterfaceDecl itf) {
		if (itf.isAnnotation()) {
			return false;
		}
		return isDirectlyAccessible(itf) && !itf.isSealed();
	}

	private boolean canImplementInterface(InterfaceDecl itf) {
		if (itf.isAnnotation()) {
			return false;
		}
		return isDirectlyAccessible(itf) && !itf.isSealed();
	}

	private boolean isNonStaticNestedClass(TypeDecl type) {
		return type.isNested() && type.isClass() && !type.getModifiers().contains(Modifier.STATIC);
	}

	private boolean isOverridable(MethodDecl method) {
		return !method.isStatic() && !method.isFinal() && !method.isPrivate() && (method.isPublic() || method.isProtected());
	}

	private boolean isRepresentable(ExecutableDecl executable) {
		return executable.getParameters().stream().allMatch(p -> isRepresentable(p.type())) &&
			api.getThrownCheckedExceptions(executable).stream().allMatch(this::isRepresentable);
	}

	private boolean isRepresentable(MethodDecl method) {
		return isRepresentable((ExecutableDecl) method) && isRepresentable(method.getType());
	}

	private boolean isRepresentable(ITypeReference type) {
		return switch (type) {
			case PrimitiveTypeReference _ -> true;
			case TypeParameterReference _ -> true;
			case WildcardTypeReference wildcard -> wildcard.bounds().stream().allMatch(this::isRepresentable);
			case ArrayTypeReference array -> isRepresentable(array.componentType());
			case TypeReference<?> reference ->
				reference.typeArguments().stream().allMatch(this::isRepresentable) &&
					api.resolver().resolve(reference).map(this::isDirectlyAccessible).orElse(true);
		};
	}

	private List<String> directConstructorInvocations(ClassDecl cls, String instantiatedType,
	                                                 ConstructorDecl constructor, Map<String, String> containingBindings) {
		Optional<Map<String, String>> resolvedConstructorBindings = resolveTypeParameterBindings(
			constructor.getFormalTypeParameters(), containingBindings);
		if (!constructor.getFormalTypeParameters().isEmpty() && resolvedConstructorBindings.isEmpty()) {
			return List.of();
		}

		Map<String, String> invocationBindings = resolvedConstructorBindings
			.orElseGet(() -> new LinkedHashMap<>(containingBindings));

		String constructedTypeName = cls.getSimpleName();
		if (!cls.getFormalTypeParameters().isEmpty()) {
			List<String> classArguments = cls.getFormalTypeParameters().stream()
				.map(parameter -> containingBindings.get(parameter.name()))
				.filter(Objects::nonNull)
				.toList();
			if (classArguments.size() == cls.getFormalTypeParameters().size()) {
				constructedTypeName += "<" + String.join(", ", classArguments) + ">";
			}
		}

		String inferredPrefix;
		String explicitPrefix = null;
		if (isNonStaticNestedClass(cls)) {
			Optional<TypeDecl> enclosing = cls.getEnclosingType().flatMap(api.resolver()::resolve);
			if (enclosing.isEmpty() || !isDirectlyAccessible(enclosing.get())) {
				return List.of();
			}
			String enclosingReceiver = "((" + renderTypeName(enclosing.get()) + ") null)";
			inferredPrefix = enclosingReceiver + ".new " + constructedTypeName;
			if (!constructor.getFormalTypeParameters().isEmpty()) {
				String explicitTypeArgs = constructor.getFormalTypeParameters().stream()
					.map(parameter -> invocationBindings.get(parameter.name()))
					.collect(Collectors.joining(", "));
				explicitPrefix = enclosingReceiver + ".new <" + explicitTypeArgs + "> " + constructedTypeName;
			}
		} else {
			inferredPrefix = "new " + instantiatedType;
			if (!constructor.getFormalTypeParameters().isEmpty()) {
				String explicitTypeArgs = constructor.getFormalTypeParameters().stream()
					.map(parameter -> invocationBindings.get(parameter.name()))
					.collect(Collectors.joining(", "));
				explicitPrefix = "new <" + explicitTypeArgs + "> " + instantiatedType;
			}
		}

		List<String> invocations = new ArrayList<>();
		String inferredArgs = renderArguments(constructor.getParameters(), false, invocationBindings);
		invocations.add(inferredPrefix + "(" + inferredArgs + ");");
		if (constructor.isVarargs()) {
			invocations.add(inferredPrefix + "(" + renderArguments(constructor.getParameters(), true, invocationBindings) + ");");
		}
		if (explicitPrefix != null) {
			invocations.add(explicitPrefix + "(" + inferredArgs + ");");
			if (constructor.isVarargs()) {
				invocations.add(explicitPrefix + "(" + renderArguments(constructor.getParameters(), true, invocationBindings) + ");");
			}
		}
		return invocations.stream().distinct().toList();
	}

	private String renderArguments(List<ParameterDecl> parameters, boolean varargsAsArray, Map<String, String> typeParameterErasures) {
		List<String> rendered = new ArrayList<>(parameters.size());
		for (ParameterDecl parameter : parameters) {
			if (parameter.isVarargs() && varargsAsArray) {
				String component = renderRawType(parameter.type(), typeParameterErasures);
				rendered.add("new " + component + "[] { " + typedDefaultValueExpression(parameter.type(), typeParameterErasures) + " }");
			} else {
				rendered.add(typedDefaultValueExpression(parameter.type(), typeParameterErasures));
			}
		}
		return String.join(", ", rendered);
	}

	private String renderTypeName(TypeDecl type) {
		return toSourceQualifiedName(type.getQualifiedName());
	}

	private String renderRawType(ITypeReference type) {
		return renderRawType(type, Map.of());
	}

	private String renderRawType(ITypeReference type, Map<String, String> typeParameterErasures) {
		return switch (type) {
			case PrimitiveTypeReference primitive -> primitive.name();
			case ArrayTypeReference array -> renderRawType(array.componentType(), typeParameterErasures) + "[]".repeat(array.dimension());
			case TypeReference<?> reference -> toSourceQualifiedName(reference.getQualifiedName());
			case TypeParameterReference tpr -> typeParameterErasures.getOrDefault(tpr.name(), "java.lang.Object");
			case WildcardTypeReference _ -> "java.lang.Object";
		};
	}

	private String renderType(ITypeReference type) {
		return renderType(type, Map.of());
	}

	private String renderType(ITypeReference type, Map<String, String> typeParameterBindings) {
		return switch (type) {
			case PrimitiveTypeReference primitive -> primitive.name();
			case ArrayTypeReference array -> renderType(array.componentType(), typeParameterBindings) + "[]".repeat(array.dimension());
			case TypeParameterReference tpr -> typeParameterBindings.getOrDefault(tpr.name(), "java.lang.Object");
			case WildcardTypeReference wildcard -> {
				if (wildcard.isUnbounded()) {
					yield "?";
				}
				String kind = wildcard.upper() ? "extends" : "super";
				String bounds = wildcard.bounds().stream()
					.map(bound -> renderType(bound, typeParameterBindings))
					.collect(Collectors.joining(" & "));
				yield "? " + kind + " " + bounds;
			}
			case TypeReference<?> reference -> {
				String raw = toSourceQualifiedName(reference.getQualifiedName());
				if (reference.typeArguments().isEmpty()) {
					yield raw;
				}
				String args = reference.typeArguments().stream()
					.map(argument -> renderType(argument, typeParameterBindings))
					.collect(Collectors.joining(", "));
				yield raw + "<" + args + ">";
			}
		};
	}

	private String defaultValueExpression(ITypeReference type) {
		return switch (type) {
			case PrimitiveTypeReference primitive -> switch (primitive.name()) {
				case "boolean" -> "false";
				case "char" -> "'\\0'";
				case "byte" -> "(byte) 0";
				case "short" -> "(short) 0";
				case "int" -> "0";
				case "long" -> "0L";
				case "float" -> "0f";
				case "double" -> "0d";
				case "void" -> "";
				default -> "0";
			};
			case ArrayTypeReference _ -> "null";
			case TypeParameterReference _ -> "null";
			case WildcardTypeReference _ -> "null";
			case TypeReference<?> _ -> "(" + renderRawType(type) + ") null";
		};
	}

	private String typedDefaultValueExpression(ITypeReference type, Map<String, String> typeParameterErasures) {
		return typedDefaultValueExpression(type, typeParameterErasures, false);
	}

	private String typedDefaultValueExpression(ITypeReference type, Map<String, String> typeParameterErasures,
	                                           boolean rawTypeCast) {
		return switch (type) {
			case PrimitiveTypeReference primitive -> defaultValueExpression(primitive);
			default -> "(" + (rawTypeCast
				? renderRawType(type, typeParameterErasures)
				: renderType(type, typeParameterErasures)) + ") null";
		};
	}

	private String markerParameters(int index) {
		if (index == 0) {
			return "";
		}
		return IntStream.range(0, index)
			.mapToObj(i -> "int m" + i)
			.collect(Collectors.joining(", "));
	}

	private String markerArguments(int index) {
		if (index == 0) {
			return "";
		}
		return IntStream.range(0, index)
			.mapToObj(i -> "0")
			.collect(Collectors.joining(", "));
	}

	private String renderFormalTypeParameters(List<FormalTypeParameter> formalTypeParameters,
	                                         Map<String, String> enclosingBindings) {
		if (formalTypeParameters.isEmpty()) {
			return "";
		}

		Map<String, String> bindings = new LinkedHashMap<>(enclosingBindings);
		List<String> rendered = new ArrayList<>(formalTypeParameters.size());
		for (FormalTypeParameter parameter : formalTypeParameters) {
			Map<String, String> localBindings = new LinkedHashMap<>(bindings);
			localBindings.put(parameter.name(), parameter.name());
			String bounds = parameter.bounds().stream()
				.filter(bound -> !bound.equals(TypeReference.OBJECT))
				.map(bound -> renderType(bound, localBindings))
				.collect(Collectors.joining(" & "));
			rendered.add(bounds.isBlank() ? parameter.name() : parameter.name() + " extends " + bounds);
			bindings.put(parameter.name(), parameter.name());
		}
		return "<" + String.join(", ", rendered) + "> ";
	}

	private String renderThrowsClause(ExecutableDecl executable) {
		List<TypeReference<?>> checked = checkedExceptionTypes(executable);
		if (checked.isEmpty()) {
			return "";
		}
		String rendered = checked.stream()
			.map(this::renderRawType)
			.collect(Collectors.joining(", "));
		return " throws " + rendered;
	}

	private boolean coversRuntimeException(List<TypeReference<?>> checked) {
		return checked.stream()
			.anyMatch(type -> api.subtyping().isSubtypeOf(TypeReference.RUNTIME_EXCEPTION, type));
	}

	private boolean isThrowableType(ClassDecl cls) {
		return api.subtyping().isSubtypeOf(new TypeReference<>(cls.getQualifiedName()), TypeReference.THROWABLE);
	}

	private boolean canEmitMethodReference(MethodDecl method) {
		Optional<TypeDecl> containing = api.resolver().resolve(method.getContainingType());
		if (containing.isEmpty()) {
			return true;
		}

		List<MethodDecl> sameName = containing.get().getDeclaredMethods().stream()
			.filter(candidate -> candidate.getSimpleName().equals(method.getSimpleName()))
			.toList();
		if (sameName.size() != 1) {
			return false;
		}
		if (method.isStatic() && sameName.stream().anyMatch(candidate -> !candidate.isStatic())) {
			return false;
		}
		return true;
	}

	private boolean canRenderMethodInSubclassContext(MethodDecl method, Map<String, String> containingTypeBindings) {
		Map<String, String> bindings = new LinkedHashMap<>(containingTypeBindings);
		for (FormalTypeParameter methodTypeParameter : method.getFormalTypeParameters()) {
			bindings.put(methodTypeParameter.name(), methodTypeParameter.name());
		}
		return !hasUnresolvedTypeParameter(method.getType(), bindings) &&
			method.getParameters().stream().noneMatch(parameter -> hasUnresolvedTypeParameter(parameter.type(), bindings)) &&
			checkedExceptionTypes(method).stream().noneMatch(exceptionType -> hasUnresolvedTypeParameter(exceptionType, bindings));
	}

	private boolean hasUnresolvedTypeParameter(ITypeReference type, Map<String, String> typeParameterErasures) {
		return switch (type) {
			case PrimitiveTypeReference _ -> false;
			case TypeParameterReference tpr -> !typeParameterErasures.containsKey(tpr.name());
			case WildcardTypeReference wildcard -> wildcard.bounds().stream()
				.anyMatch(bound -> hasUnresolvedTypeParameter(bound, typeParameterErasures));
			case ArrayTypeReference array -> hasUnresolvedTypeParameter(array.componentType(), typeParameterErasures);
			case TypeReference<?> reference -> reference.typeArguments().stream()
				.anyMatch(argument -> hasUnresolvedTypeParameter(argument, typeParameterErasures));
		};
	}

	private Map<String, String> typeParameterErasures(ExecutableDecl executable) {
		Map<String, String> erasures = new LinkedHashMap<>();
		api.resolver().resolve(executable.getContainingType())
			.ifPresent(type -> addTypeParameterErasures(type.getFormalTypeParameters(), erasures));
		addTypeParameterErasures(executable.getFormalTypeParameters(), erasures);
		return erasures;
	}

	private void addTypeParameterErasures(List<FormalTypeParameter> formalTypeParameters, Map<String, String> erasures) {
		for (FormalTypeParameter parameter : formalTypeParameters) {
			ITypeReference erasureBound = selectErasureBound(parameter.bounds());
			erasures.put(parameter.name(), resolveTypeParameterErasure(erasureBound, erasures, new HashSet<>()));
		}
	}

	private ITypeReference selectErasureBound(List<ITypeReference> bounds) {
		if (bounds == null || bounds.isEmpty()) {
			return TypeReference.OBJECT;
		}
		return bounds.stream()
			.filter(bound -> !bound.equals(TypeReference.OBJECT))
			.findFirst()
			.orElse(bounds.getFirst());
	}

	private Optional<String> createIntersectionWitness(FormalTypeParameter parameter, Map<String, String> typeParameterErasures) {
		List<ITypeReference> bounds = parameter.bounds();
		if (bounds == null) {
			return Optional.empty();
		}

		List<ITypeReference> nonObjectBounds = new ArrayList<>();
		for (ITypeReference bound : bounds) {
			if (!bound.equals(TypeReference.OBJECT)) {
				nonObjectBounds.add(bound);
			}
		}
		boolean requiresWitness = nonObjectBounds.size() > 1 || containsTypeParameter(nonObjectBounds, parameter.name());
		if (!requiresWitness) {
			return Optional.empty();
		}

		List<String> normalizedBounds = new ArrayList<>(nonObjectBounds.size());
		for (ITypeReference bound : nonObjectBounds) {
			Optional<String> rendered = renderWitnessBound(bound, parameter.name(), "__SELF__", typeParameterErasures);
			if (rendered.isEmpty()) {
				return Optional.empty();
			}
			normalizedBounds.add(rendered.get());
		}

		String key = String.join(" & ", normalizedBounds);
		String existing = boundWitnessByKey.get(key);
		if (existing != null) {
			return Optional.of(existing);
		}

		ITypeReference classBound = null;
		List<ITypeReference> interfaceBounds = new ArrayList<>();
		for (ITypeReference bound : nonObjectBounds) {
			Optional<TypeDecl> resolved = resolveRawBoundType(bound, parameter.name(), typeParameterErasures);
			if (resolved.isEmpty()) {
				return Optional.empty();
			}
			if (resolved.get().isInterface()) {
				interfaceBounds.add(bound);
			} else {
				if (classBound != null) {
					return Optional.empty();
				}
				classBound = bound;
			}
		}

		if (classBound == null && interfaceBounds.isEmpty()) {
			return Optional.empty();
		}

		String witnessName = "BoundWitness" + (++boundWitnessCounter);
		CodeBlock declaration = new CodeBlock(1);
			if (classBound != null) {
			Optional<String> renderedClassBound = renderWitnessBound(classBound, parameter.name(), witnessName,
				typeParameterErasures);
			if (renderedClassBound.isEmpty()) {
				return Optional.empty();
			}
			Optional<TypeDecl> resolvedClassBound = resolveRawBoundType(classBound, parameter.name(), typeParameterErasures);
			if (resolvedClassBound.isEmpty() || !canUseAsWitnessSuperClass(resolvedClassBound.get())) {
				return Optional.empty();
			}
			List<String> renderedInterfaces = new ArrayList<>(interfaceBounds.size());
			for (ITypeReference interfaceBound : interfaceBounds) {
				Optional<String> renderedInterface = renderWitnessBound(interfaceBound, parameter.name(), witnessName,
					typeParameterErasures);
				if (renderedInterface.isEmpty()) {
					return Optional.empty();
				}
				renderedInterfaces.add(renderedInterface.get());
			}
			String implementsClause = renderedInterfaces.isEmpty()
				? ""
				: " implements " + String.join(", ", renderedInterfaces);
			declaration.line("private abstract static class " + witnessName + " extends " + renderedClassBound.get()
				+ implementsClause + " {");
			declaration.line("}");
		} else {
			List<String> renderedInterfaces = new ArrayList<>(interfaceBounds.size());
			for (ITypeReference interfaceBound : interfaceBounds) {
				Optional<String> renderedInterface = renderWitnessBound(interfaceBound, parameter.name(), witnessName,
					typeParameterErasures);
				if (renderedInterface.isEmpty()) {
					return Optional.empty();
				}
				renderedInterfaces.add(renderedInterface.get());
			}
			declaration.line("private interface " + witnessName + " extends " + String.join(", ", renderedInterfaces) + " {");
			declaration.line("}");
		}

		boundWitnessByKey.put(key, witnessName);
		boundWitnessDeclarations.add(declaration.toString());
		return Optional.of(witnessName);
	}

	private boolean canUseAsWitnessSuperClass(TypeDecl typeDecl) {
		if (!(typeDecl instanceof ClassDecl cls)) {
			return false;
		}
		if (!isDirectlyAccessible(cls)) {
			return false;
		}
		return !cls.getQualifiedName().equals(Enum.class.getCanonicalName()) &&
			!cls.isEnum() && !cls.isRecord() && !cls.isSealed() && !api.isEffectivelyFinal(cls);
	}

	private boolean containsTypeParameter(List<ITypeReference> references, String typeParameterName) {
		return references.stream().anyMatch(reference -> containsTypeParameter(reference, typeParameterName));
	}

	private boolean containsTypeParameter(ITypeReference reference, String typeParameterName) {
		return switch (reference) {
			case PrimitiveTypeReference _ -> false;
			case TypeParameterReference tpr -> tpr.name().equals(typeParameterName);
			case WildcardTypeReference wildcard -> containsTypeParameter(wildcard.bounds(), typeParameterName);
			case ArrayTypeReference array -> containsTypeParameter(array.componentType(), typeParameterName);
			case TypeReference<?> typeReference ->
				typeReference.typeArguments().stream().anyMatch(argument -> containsTypeParameter(argument, typeParameterName));
		};
	}

	private Optional<TypeDecl> resolveRawBoundType(ITypeReference bound, String selfParameterName,
	                                               Map<String, String> typeParameterErasures) {
		return switch (bound) {
			case TypeReference<?> reference -> api.resolver().resolve(new TypeReference<>(reference.getQualifiedName()));
			case TypeParameterReference reference -> {
				if (reference.name().equals(selfParameterName)) {
					yield Optional.empty();
				}
				String erasure = typeParameterErasures.get(reference.name());
				if (erasure == null || erasure.indexOf('.') < 0 || erasure.endsWith("[]")) {
					yield Optional.empty();
				}
				yield api.resolver().resolve(new TypeReference<>(erasure));
			}
			default -> Optional.empty();
		};
	}

	private Optional<String> renderWitnessBound(ITypeReference bound, String selfParameterName,
	                                            String selfReplacement, Map<String, String> typeParameterErasures) {
		return switch (bound) {
			case PrimitiveTypeReference primitive -> Optional.of(primitive.name());
			case ArrayTypeReference array -> renderWitnessBound(array.componentType(), selfParameterName, selfReplacement,
				typeParameterErasures)
				.map(component -> component + "[]".repeat(array.dimension()));
			case TypeParameterReference reference -> {
				if (reference.name().equals(selfParameterName)) {
					yield Optional.of(selfReplacement);
				}
				String erased = typeParameterErasures.get(reference.name());
				yield Optional.ofNullable(erased);
			}
			case WildcardTypeReference wildcard -> {
				ITypeReference selectedBound;
				if (wildcard.isUnbounded()) {
					selectedBound = TypeReference.OBJECT;
				} else if (wildcard.upper()) {
					selectedBound = wildcard.bounds().stream()
						.filter(wildcardBound -> !wildcardBound.equals(TypeReference.OBJECT))
						.findFirst()
						.orElse(TypeReference.OBJECT);
				} else {
					selectedBound = wildcard.bounds().getFirst();
				}
				yield renderWitnessBound(selectedBound, selfParameterName, selfReplacement, typeParameterErasures);
			}
			case TypeReference<?> reference -> {
				String raw = toSourceQualifiedName(reference.getQualifiedName());
				if (reference.typeArguments().isEmpty()) {
					yield Optional.of(raw);
				}
				List<String> renderedArguments = new ArrayList<>(reference.typeArguments().size());
				for (ITypeReference argument : reference.typeArguments()) {
					Optional<String> renderedArgument = renderWitnessBound(argument, selfParameterName, selfReplacement,
						typeParameterErasures);
					if (renderedArgument.isEmpty()) {
						yield Optional.empty();
					}
					renderedArguments.add(renderedArgument.get());
				}
				yield Optional.of(raw + "<" + String.join(", ", renderedArguments) + ">");
			}
		};
	}

	private boolean erasureBoundSatisfiesAllBounds(ITypeReference erasureBound, List<ITypeReference> allBounds) {
		if (!(erasureBound instanceof TypeReference<?> erasureReference)) {
			return true;
		}

		for (ITypeReference bound : allBounds) {
			if (bound.equals(TypeReference.OBJECT)) {
				continue;
			}
			if (!(bound instanceof TypeReference<?> target)) {
				return false;
			}
			if (!api.subtyping().isSubtypeOf(erasureReference, target)) {
				return false;
			}
		}
		return true;
	}

	private String resolveTypeParameterErasure(ITypeReference bound, Map<String, String> erasures, Set<String> visiting) {
		return switch (bound) {
			case PrimitiveTypeReference primitive -> primitive.name();
			case ArrayTypeReference array ->
				resolveTypeParameterErasure(array.componentType(), erasures, visiting) + "[]".repeat(array.dimension());
			case TypeReference<?> reference -> toSourceQualifiedName(reference.getQualifiedName());
			case TypeParameterReference reference -> {
				if (!visiting.add(reference.name())) {
					yield "java.lang.Object";
				}
				yield erasures.getOrDefault(reference.name(), "java.lang.Object");
			}
			case WildcardTypeReference _ -> "java.lang.Object";
		};
	}

	private List<TypeReference<?>> checkedExceptionTypes(ExecutableDecl executable) {
		List<TypeReference<?>> raw = new ArrayList<>();
		for (ITypeReference exceptionType : api.getThrownCheckedExceptions(executable)) {
			if (exceptionType instanceof TypeReference<?> tr && isRepresentable(tr)) {
				raw.add(tr);
			}
		}

		raw.sort((a, b) -> {
			boolean aSubB = api.subtyping().isSubtypeOf(a, b);
			boolean bSubA = api.subtyping().isSubtypeOf(b, a);
			if (aSubB && !bSubA) {
				return -1;
			}
			if (bSubA && !aSubB) {
				return 1;
			}
			return a.getQualifiedName().compareTo(b.getQualifiedName());
		});

		LinkedHashSet<String> seen = new LinkedHashSet<>();
		List<TypeReference<?>> deduped = new ArrayList<>();
		for (TypeReference<?> type : raw) {
			String rendered = renderRawType(type);
			if (seen.add(rendered)) {
				deduped.add(type);
			}
		}
		return deduped;
	}

	private String nextLocal(String prefix) {
		return prefix + (++localVariableCounter);
	}

	private String nextLocalType(String prefix) {
		return prefix + (++localTypeCounter);
	}

	private static List<FieldDecl> sortedFields(Set<FieldDecl> fields) {
		return fields.stream().sorted(FIELD_COMPARATOR).toList();
	}

	private static List<MethodDecl> sortedMethods(Set<MethodDecl> methods) {
		return methods.stream().sorted(METHOD_COMPARATOR).toList();
	}

	private static List<ConstructorDecl> sortedConstructors(Set<ConstructorDecl> constructors) {
		return constructors.stream().sorted(CONSTRUCTOR_COMPARATOR).toList();
	}

	private static String toSourceQualifiedName(String qualifiedName) {
		return qualifiedName.replace('$', '.');
	}

	private static final class CodeBlock {
		private final StringBuilder builder = new StringBuilder(4_096);
		private int indentLevel;

		private CodeBlock(int indentLevel) {
			this.indentLevel = indentLevel;
		}

		void line(String text) {
			for (int i = 0; i < indentLevel; i++) {
				builder.append('\t');
			}
			builder.append(text).append('\n');
		}

		void raw(String text) {
			builder.append(text);
		}

		void emptyLine() {
			builder.append('\n');
		}

		void indent() {
			indentLevel++;
		}

		void outdent() {
			indentLevel = Math.max(0, indentLevel - 1);
		}

		@Override
		public String toString() {
			return builder.toString();
		}
	}
}
