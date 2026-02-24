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

	public FootprintGenerator(String packageName, String className) {
		this.packageName = Objects.requireNonNull(packageName);
		this.className = Objects.requireNonNull(className);
	}

	public String generate(API api) {
		this.api = Objects.requireNonNull(api);
		typeMethodCounter = 0;
		localTypeCounter = 0;
		localVariableCounter = 0;

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
		String typeName = renderTypeName(itf);
		if (itf.isAnnotation()) {
			block.line("// Annotation interfaces are referenced and their members are invoked as interface methods");
			return;
		}

		if (canExtendInterface(itf)) {
			String extensionName = nextLocalType("Extended");
			block.line("interface " + extensionName + " extends " + typeName + " {");
			block.line("}");
		} else if (itf.isSealed()) {
			block.line("// Cannot extend sealed interface from generated footprint: " + itf.getQualifiedName());
		}

		if (canImplementInterface(itf)) {
			List<MethodDecl> toImplement = sortedMethods(api.getAllMethodsToImplement(itf));
			if (toImplement.stream().allMatch(this::isRepresentable)) {
				String implVar = nextLocal("impl");
				block.line(typeName + " " + implVar + " = new " + typeName + "() {");
				block.indent();
				for (MethodDecl method : toImplement) {
					emitMethodImplementation(block, method);
				}
				block.outdent();
				block.line("};");
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
		List<ConstructorDecl> constructors = sortedConstructors(cls.getDeclaredConstructors());
		for (ConstructorDecl constructor : constructors) {
			if (constructor.isPublic() && !cls.isEffectivelyAbstract() && isRepresentable(constructor)) {
				Optional<String> invocation = directConstructorInvocation(cls, constructor);
				if (invocation.isPresent()) {
					emitInvocation(block, invocation.get(), constructor);
					if (!isNonStaticNestedClass(cls)) {
						emitConstructorReference(block, cls, constructor);
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
			if (baseCtor != null && toImplement.stream().allMatch(this::isRepresentable)) {
				Optional<String> invocation = directConstructorInvocation(cls, baseCtor);
				if (invocation.isPresent()) {
					String anonVar = nextLocal("anonImpl");
					block.line(typeName + " " + anonVar + " = " + invocation.get().replace(";", "") + " {");
					block.indent();
					for (MethodDecl method : toImplement) {
						emitMethodImplementation(block, method);
					}
					block.outdent();
					block.line("};");
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
		String typeName = renderTypeName(cls);
		List<ConstructorDecl> accessibleConstructors = findSubclassAccessibleConstructors(cls);
		if (accessibleConstructors.isEmpty()) {
			block.line("// No subclass-accessible constructor for " + cls.getQualifiedName());
			return;
		}

		List<MethodDecl> methodsToImplement = sortedMethods(api.getAllMethodsToImplement(cls));
		boolean concreteSubclass = methodsToImplement.stream().allMatch(this::isRepresentable);
		String classKind = concreteSubclass ? "class" : "abstract class";
		block.line(classKind + " " + subclassName + " extends " + typeName + " {");
		block.indent();
		for (int i = 0; i < accessibleConstructors.size(); i++) {
			ConstructorDecl constructor = accessibleConstructors.get(i);
			String marker = markerParameters(i);
			String throwsClause = renderThrowsClause(constructor);
				String signature = marker.isBlank()
					? subclassName + "()" + throwsClause
					: subclassName + "(" + marker + ")" + throwsClause;
				block.line(signature + " {");
				block.indent();
				String args = renderArguments(constructor.getParameters(), false, typeParameterErasures(constructor));
				block.line("super(" + args + ");");
				block.outdent();
				block.line("}");
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
					emitRuntimeStatement(block, renderRawType(field.getType()) + " " + nextLocal("protectedRead") +
						" = " + typeName + "." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, typeName + "." + field.getSimpleName() + " = " + defaultValueExpression(field.getType()) + ";");
					}
				} else {
					emitRuntimeStatement(block, renderRawType(field.getType()) + " " + nextLocal("protectedRead") +
						" = this." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, "this." + field.getSimpleName() + " = " + defaultValueExpression(field.getType()) + ";");
					}
				}
			}

			for (MethodDecl method : protectedMethods) {
				String receiver = method.isStatic() ? typeName : "this";
				List<String> invocations = invocationForms(method, receiver);
				for (String invocation : invocations) {
					emitInvocation(block, invocation, method);
				}
				if (!method.getFormalTypeParameters().isEmpty() && renderExplicitMethodTypeArguments(method).isEmpty()) {
					block.line("// Explicit type arguments unrepresentable for " + method.getQualifiedName());
				}

				emitMethodReference(block, method, receiver);
			}
			block.outdent();
			block.line("}");
		}

		Set<String> implementedSignatures = new HashSet<>();
		if (concreteSubclass) {
			for (MethodDecl method : methodsToImplement) {
				emitMethodImplementation(block, method);
				implementedSignatures.add(method.getSignature());
			}
		} else if (!methodsToImplement.isEmpty()) {
			block.line("// Concrete extension is unrepresentable due to inaccessible method signatures");
		}
		List<MethodDecl> overridable = sortedMethods(cls.getDeclaredMethods()).stream()
			.filter(this::isOverridable)
			.filter(this::isRepresentable)
			.filter(method -> !implementedSignatures.contains(method.getSignature()))
			.toList();
		for (MethodDecl method : overridable) {
			emitOverrideMethod(block, method);
		}

		block.outdent();
		block.line("}");
		if (concreteSubclass) {
			for (int i = 0; i < accessibleConstructors.size(); i++) {
				ConstructorDecl constructor = accessibleConstructors.get(i);
				String markerArgs = markerArguments(i);
				String invocation = markerArgs.isBlank()
					? "new " + subclassName + "();"
					: "new " + subclassName + "(" + markerArgs + ");";
				emitInvocation(block, invocation, constructor);
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

		for (FieldDecl field : sortedFields(type.getDeclaredFields())) {
			if (!isRepresentable(field.getType())) {
				block.line("// Field uses inaccessible type from this footprint package: " + field.getQualifiedName());
				continue;
			}
			String fieldType = renderRawType(field.getType());
			if (field.isPublic()) {
				if (field.isStatic()) {
					emitRuntimeStatement(block, fieldType + " " + nextLocal("readField") + " = " + typeName + "." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, typeName + "." + field.getSimpleName() + " = " + defaultValueExpression(field.getType()) + ";");
					}
				} else {
					String receiver = "((" + typeName + ") null)";
					emitRuntimeStatement(block, fieldType + " " + nextLocal("readField") + " = " + receiver + "." + field.getSimpleName() + ";");
					if (!field.isFinal()) {
						emitRuntimeStatement(block, receiver + "." + field.getSimpleName() + " = " + defaultValueExpression(field.getType()) + ";");
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

		for (MethodDecl method : sortedMethods(type.getDeclaredMethods())) {
			if (!isRepresentable(method)) {
				block.line("// Method uses inaccessible types from this footprint package: " + method.getQualifiedName());
				continue;
			}
			if (method.isPublic()) {
				String receiver = method.isStatic() ? typeName : "((" + typeName + ") null)";
				List<String> invocations = invocationForms(method, receiver);
				for (String invocation : invocations) {
					emitInvocation(block, invocation, method);
				}
				if (!method.getFormalTypeParameters().isEmpty() && renderExplicitMethodTypeArguments(method).isEmpty()) {
					block.line("// Explicit type arguments unrepresentable for " + method.getQualifiedName());
				}
				emitMethodReference(block, method, receiver);
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
			String receiver = "((" + typeName + ") null)";
			List<String> invocations = invocationForms(method, receiver);
			for (String invocation : invocations) {
				emitInvocation(block, invocation, method);
			}
			if (!method.getFormalTypeParameters().isEmpty() && renderExplicitMethodTypeArguments(method).isEmpty()) {
				block.line("// Explicit type arguments unrepresentable for " + method.getQualifiedName());
			}
			emitMethodReference(block, method, receiver);
		}
	}

	private void emitMethodReference(CodeBlock block, MethodDecl method, String receiver) {
		Map<String, String> typeParameterErasures = typeParameterErasures(method);
		String methodRefType = nextLocalType("MethodRef");
		String methodRefVar = nextLocal("methodRef");
		List<String> params = new ArrayList<>(method.getParameters().size());
		for (int i = 0; i < method.getParameters().size(); i++) {
			ParameterDecl parameter = method.getParameters().get(i);
			String pType = parameter.isVarargs()
				? renderRawType(parameter.type(), typeParameterErasures) + "[]"
				: renderRawType(parameter.type(), typeParameterErasures);
			params.add(pType + " p" + i);
		}

		block.line("@java.lang.FunctionalInterface");
		block.line("interface " + methodRefType + " {");
		block.indent();
		block.line(renderRawType(method.getType(), typeParameterErasures) + " invoke(" + String.join(", ", params) + ")" + renderThrowsClause(method) + ";");
		block.outdent();
		block.line("}");
		emitRuntimeStatement(block, methodRefType + " " + methodRefVar + " = " + receiver + "::" + method.getSimpleName() + ";");
	}

	private void emitMethodImplementation(CodeBlock block, MethodDecl method) {
		Map<String, String> typeParameterErasures = typeParameterErasures(method);
		String returnType = renderRawType(method.getType(), typeParameterErasures);
		String throwsClause = renderThrowsClause(method);
		List<String> params = new ArrayList<>(method.getParameters().size());
		for (int i = 0; i < method.getParameters().size(); i++) {
			ParameterDecl parameter = method.getParameters().get(i);
			String paramType = renderRawType(parameter.type(), typeParameterErasures);
			if (parameter.isVarargs()) {
				paramType += "...";
			}
			params.add(paramType + " p" + i);
		}

		block.line("@Override");
		block.line("public " + returnType + " " + method.getSimpleName() + "(" + String.join(", ", params) + ")" + throwsClause + " {");
		block.indent();
		if ("void".equals(returnType)) {
			block.line("return;");
		} else {
			block.line("return " + defaultValueExpression(method.getType()) + ";");
		}
		block.outdent();
		block.line("}");
	}

	private void emitOverrideMethod(CodeBlock block, MethodDecl method) {
		Map<String, String> typeParameterErasures = typeParameterErasures(method);
		String returnType = renderRawType(method.getType(), typeParameterErasures);
		String throwsClause = renderThrowsClause(method);
		List<String> params = new ArrayList<>(method.getParameters().size());
		for (int i = 0; i < method.getParameters().size(); i++) {
			ParameterDecl parameter = method.getParameters().get(i);
			String paramType = renderRawType(parameter.type(), typeParameterErasures);
			if (parameter.isVarargs()) {
				paramType += "...";
			}
			params.add(paramType + " p" + i);
		}

		block.line("@Override");
		block.line("public " + returnType + " " + method.getSimpleName() + "(" + String.join(", ", params) + ")" + throwsClause + " {");
		block.indent();
		if ("void".equals(returnType)) {
			block.line("return;");
		} else {
			block.line("return " + defaultValueExpression(method.getType()) + ";");
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

	private void emitConstructorReference(CodeBlock block, ClassDecl cls, ConstructorDecl constructor) {
		String refType = nextLocalType("CtorRef");
		String refVar = nextLocal("ctorRef");
		List<String> params = new ArrayList<>(constructor.getParameters().size());
		for (int i = 0; i < constructor.getParameters().size(); i++) {
			ParameterDecl parameter = constructor.getParameters().get(i);
			String pType = parameter.isVarargs()
				? renderRawType(parameter.type()) + "[]"
				: renderRawType(parameter.type());
			params.add(pType + " p" + i);
		}
		block.line("@java.lang.FunctionalInterface");
		block.line("interface " + refType + " {");
		block.indent();
		block.line(renderTypeName(cls) + " create(" + String.join(", ", params) + ")" + renderThrowsClause(constructor) + ";");
		block.outdent();
		block.line("}");
		emitRuntimeStatement(block, refType + " " + refVar + " = " + renderTypeName(cls) + "::new;");
	}

	private List<String> invocationForms(MethodDecl method, String receiver) {
		Map<String, String> typeParameterErasures = typeParameterErasures(method);
		Set<String> forms = new HashSet<>();
		String inferredPrefix = method.isStatic()
			? receiver + "." + method.getSimpleName()
			: receiver + "." + method.getSimpleName();
		String inferredArgs = renderArguments(method.getParameters(), false, typeParameterErasures);
		forms.add(inferredPrefix + "(" + inferredArgs + ");");
		if (method.isVarargs()) {
			forms.add(inferredPrefix + "(" + renderArguments(method.getParameters(), true, typeParameterErasures) + ");");
		}

		Optional<String> explicitTypeArgs = renderExplicitMethodTypeArguments(method);
		if (explicitTypeArgs.isPresent()) {
			String explicitPrefix = receiver + ".<" + explicitTypeArgs.get() + ">" + method.getSimpleName();
			forms.add(explicitPrefix + "(" + inferredArgs + ");");
			if (method.isVarargs()) {
				forms.add(explicitPrefix + "(" + renderArguments(method.getParameters(), true, typeParameterErasures) + ");");
			}
		}

		return forms.stream().sorted().toList();
	}

	private Optional<String> renderExplicitMethodTypeArguments(MethodDecl method) {
		if (method.getFormalTypeParameters().isEmpty()) {
			return Optional.empty();
		}

		List<String> args = new ArrayList<>(method.getFormalTypeParameters().size());
		for (FormalTypeParameter parameter : method.getFormalTypeParameters()) {
			Optional<String> explicit = explicitTypeArgument(parameter);
			if (explicit.isEmpty()) {
				return Optional.empty();
			}
			args.add(explicit.get());
		}
		return Optional.of(String.join(", ", args));
	}

	private Optional<String> renderParameterizedType(TypeDecl type) {
		if (type.getFormalTypeParameters().isEmpty()) {
			return Optional.empty();
		}

		List<String> args = new ArrayList<>(type.getFormalTypeParameters().size());
		for (FormalTypeParameter parameter : type.getFormalTypeParameters()) {
			Optional<String> rendered = parameterizedTypeArgument(parameter);
			if (rendered.isEmpty()) {
				return Optional.empty();
			}
			args.add(rendered.get());
		}
		return Optional.of(renderTypeName(type) + "<" + String.join(", ", args) + ">");
	}

	private Optional<String> parameterizedTypeArgument(FormalTypeParameter parameter) {
		List<ITypeReference> bounds = parameter.bounds();
		if (bounds.size() == 1 && bounds.getFirst().equals(TypeReference.OBJECT)) {
			return Optional.of("?");
		}
		if (!bounds.isEmpty() && bounds.getFirst() instanceof TypeReference<?> tr) {
			return Optional.of("? extends " + renderRawType(tr));
		}
		return Optional.empty();
	}

	private Optional<String> explicitTypeArgument(FormalTypeParameter parameter) {
		List<ITypeReference> bounds = parameter.bounds();
		if (bounds.size() == 1 && bounds.getFirst().equals(TypeReference.OBJECT)) {
			return Optional.of("java.lang.Object");
		}
		if (!bounds.isEmpty() && bounds.getFirst() instanceof TypeReference<?> tr) {
			return Optional.of(renderRawType(tr));
		}
		return Optional.empty();
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

	private Optional<String> directConstructorInvocation(ClassDecl cls, ConstructorDecl constructor) {
		String creationPrefix;
		if (isNonStaticNestedClass(cls)) {
			Optional<TypeDecl> enclosing = cls.getEnclosingType().flatMap(api.resolver()::resolve);
			if (enclosing.isEmpty() || !isDirectlyAccessible(enclosing.get())) {
				return Optional.empty();
			}
			creationPrefix = "((" + renderTypeName(enclosing.get()) + ") null).new " + cls.getSimpleName();
		} else {
			creationPrefix = "new " + renderTypeName(cls);
		}

		String args = renderArguments(constructor.getParameters(), false, typeParameterErasures(constructor));
		String invocation = creationPrefix + "(" + args + ")";
		return Optional.of(invocation + ";");
	}

	private String renderArguments(List<ParameterDecl> parameters, boolean varargsAsArray, Map<String, String> typeParameterErasures) {
		List<String> rendered = new ArrayList<>(parameters.size());
		for (ParameterDecl parameter : parameters) {
			if (parameter.isVarargs() && varargsAsArray) {
				String component = renderRawType(parameter.type(), typeParameterErasures);
				rendered.add("new " + component + "[] { " + defaultValueExpression(parameter.type()) + " }");
			} else {
				rendered.add(defaultValueExpression(parameter.type()));
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

	private Map<String, String> typeParameterErasures(ExecutableDecl executable) {
		Map<String, String> erasures = new LinkedHashMap<>();
		api.resolver().resolve(executable.getContainingType())
			.ifPresent(type -> addTypeParameterErasures(type.getFormalTypeParameters(), erasures));
		addTypeParameterErasures(executable.getFormalTypeParameters(), erasures);
		return erasures;
	}

	private void addTypeParameterErasures(List<FormalTypeParameter> formalTypeParameters, Map<String, String> erasures) {
		for (FormalTypeParameter parameter : formalTypeParameters) {
			ITypeReference firstBound = parameter.bounds().isEmpty()
				? TypeReference.OBJECT
				: parameter.bounds().getFirst();
			erasures.put(parameter.name(), resolveTypeParameterErasure(firstBound, erasures, new HashSet<>()));
		}
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
