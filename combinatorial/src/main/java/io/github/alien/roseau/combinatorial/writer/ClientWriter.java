package io.github.alien.roseau.combinatorial.writer;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.alien.roseau.combinatorial.client.ClientTemplates.*;

public final class ClientWriter extends AbstractWriter {
	private final API api;

	private static final Logger LOGGER = LogManager.getLogger(ClientWriter.class);

	private static final String clientPackageName = Constants.CLIENT_FOLDER;

	private final Map<String, InnerType> _innerTypes = new HashMap<>();
	private final Set<String> _exceptions = new HashSet<>();
	private final List<String> _notThrowingInstructions = new ArrayList<>();
	private final List<String> _throwingInstructions = new ArrayList<>();
	private final List<String> _tryCatchInstructions = new ArrayList<>();

	public ClientWriter(Path outputDir, API api) {
		super(outputDir);
		this.api = api;
	}

	public void writeClassInheritance(ClassDecl classDecl) {
		var necessaryMethods = implementNecessaryMethods(classDecl);

		var inheritanceClassName = "%sMinimal".formatted(classDecl.getPrettyQualifiedName());
		var inheritanceConstructorRequired = implementRequiredConstructor(classDecl, inheritanceClassName);
		insertDeclarationsToInnerClass(classDecl, inheritanceClassName, inheritanceConstructorRequired, necessaryMethods);

		var fullClassName = "%sFull".formatted(classDecl.getPrettyQualifiedName());
		var fullConstructorRequired = implementRequiredConstructor(classDecl, fullClassName);
		insertDeclarationsToInnerClass(classDecl, fullClassName, fullConstructorRequired, necessaryMethods);

		var overrideClassName = "%sOverride".formatted(classDecl.getPrettyQualifiedName());
		var overrideConstructorRequired = implementRequiredConstructor(classDecl, overrideClassName);
		insertDeclarationsToInnerClass(classDecl, overrideClassName, overrideConstructorRequired, necessaryMethods);

		addInstructionToClientMain(generateConstructorDirectInvocationFromInheritance(classDecl, "Minimal") + ";");
	}

	public void writeConstructorDirectInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);
		var params = getParamsForExecutableInvocation(constructorDecl);

		var code = generateConstructorInvocationWithParamsForClass(containingClass, params);
		addInstructionToClientMain(exceptions, "%s;".formatted(code));

		if (constructorDecl.getFormalTypeParameters().isEmpty()) return;

		var formalParams = getFormalParamsForExecutableInvocation(constructorDecl);
		var codeWithFormalParams = generateConstructorInvocationWithParamsForClass(containingClass, formalParams, params);
		addInstructionToClientMain(exceptions, "%s;".formatted(codeWithFormalParams));
	}

	public void writeConstructorInheritanceInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		if (containingClass.isNested()) return;

		var innerTypeName = "%sFull".formatted(containingClass.getPrettyQualifiedName());

		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);
		var formattedExceptions = formatExceptionNames(exceptions);
		var paramsNames = constructorDecl.getParameters().stream()
				.map(ParameterDecl::name)
				.collect(Collectors.joining(", "));
		var formalParamsNames = constructorDecl.getFormalTypeParameters().stream()
				.map(FormalTypeParameter::name)
				.collect(Collectors.joining(", "));
		var paramsValues = getParamsForExecutableInvocation(constructorDecl);

		var constructor = "\t%s%s {\n\t\t%ssuper(%s);\n\t}".formatted(
				constructorDecl.toString().replace(constructorDecl.getSimpleName(), innerTypeName),
				formattedExceptions.isBlank() ? "" : " throws %s".formatted(formattedExceptions),
				formalParamsNames.isEmpty() ? "" : "<%s> ".formatted(formalParamsNames),
				paramsNames
		);

		insertDeclarationsToInnerClass(containingClass, innerTypeName, constructor, "");
		addInstructionToClientMain(exceptions, "new %s(%s);".formatted(innerTypeName, paramsValues));
	}

	public void writeExceptionCatch(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "try {\n\t\t\tthrow %s;\n\t\t} catch (%s e) {}".formatted(constructor, StringUtils.cleanQualifiedNameForType(classDecl));

		addInstructionToClientMain(code);
	}

	public void writeExceptionThrow(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "throw %s;".formatted(constructor);

		var exceptions = new ArrayList<String>();
		if (api.isCheckedException(classDecl)) {
			exceptions.add(StringUtils.cleanQualifiedNameForType(classDecl));
		}

		addInstructionToClientMain(exceptions, code);
	}

	public void writeExceptionThrows(ClassDecl classDecl) {
		this._exceptions.add(StringUtils.cleanQualifiedNameForType(classDecl));
	}

	public void writeEnumValueRead(EnumValueDecl enumValueDecl, EnumDecl containingEnum) {
		var caller = getContainingTypeAccessForTypeMember(containingEnum, enumValueDecl);
		var enumValueReadCode = "%s %sVal = %s.%s;".formatted(StringUtils.cleanQualifiedNameForType(containingEnum), enumValueDecl.getPrettyQualifiedName(), caller, enumValueDecl.getSimpleName());

		addInstructionToClientMain(enumValueReadCode);
	}

	public void writeReadFieldThroughContainingType(FieldDecl fieldDecl, TypeDecl containingType) {
		var type = StringUtils.cleanQualifiedNameForType(fieldDecl.getType());
		var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
		var fieldReadCode = "%s %sVal = %s.%s;".formatted(type, fieldDecl.getPrettyQualifiedName(), caller, fieldDecl.getSimpleName());

		addInstructionToClientMain(fieldReadCode);
	}

	public void writeReadFieldThroughMethodCall(FieldDecl fieldDecl, TypeDecl containingType) {
		var readFieldMethodName = "%sFieldRead".formatted(fieldDecl.getPrettyQualifiedName());

		var type = StringUtils.cleanQualifiedNameForType(fieldDecl.getType());
		var caller = fieldDecl.isStatic() ? StringUtils.cleanQualifiedNameForType(containingType) : "this";
		var readMethodBody = "%s val = %s.%s;".formatted(type, caller, fieldDecl.getSimpleName());

		addNewMethodToInnerType(containingType, readFieldMethodName, readMethodBody);
	}

	public void writeReadFieldThroughSubType(FieldDecl fieldDecl, TypeDecl containingType) {
		var type = StringUtils.cleanQualifiedNameForType(fieldDecl.getType());
		var inheritedCaller = generateConstructorDirectInvocationFromInheritance(containingType, "Full");
		var fieldReadCode = "%s %sInhVal = %s.%s;".formatted(type, fieldDecl.getPrettyQualifiedName(), inheritedCaller, fieldDecl.getSimpleName());
		addInstructionToClientMain(fieldReadCode);
	}

	public void writeWriteFieldThroughContainingType(FieldDecl fieldDecl, TypeDecl containingType) {
		var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
		var value = getDefaultValueForType(StringUtils.cleanQualifiedNameForType(fieldDecl.getType()));
		var fieldWriteCode = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

		addInstructionToClientMain(fieldWriteCode);
	}

	public void writeWriteFieldThroughMethodCall(FieldDecl fieldDecl, TypeDecl containingType) {
		var writeFieldMethodName = "%sFieldWrite".formatted(fieldDecl.getPrettyQualifiedName());

		var caller = fieldDecl.isStatic() ? StringUtils.cleanQualifiedNameForType(containingType) : "this";
		var value = getDefaultValueForType(StringUtils.cleanQualifiedNameForType(fieldDecl.getType()));
		var writeMethodBody = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

		addNewMethodToInnerType(containingType, writeFieldMethodName, writeMethodBody);
	}

	public void writeWriteFieldThroughSubType(FieldDecl fieldDecl, TypeDecl containingType) {
		var inheritedCaller = generateConstructorDirectInvocationFromInheritance(containingType, "Full");
		var value = getDefaultValueForType(StringUtils.cleanQualifiedNameForType(fieldDecl.getType()));
		var fieldWriteCode = "%s.%s = %s;".formatted(inheritedCaller, fieldDecl.getSimpleName(), value);
		addInstructionToClientMain(fieldWriteCode);
	}

	public void writeRecordComponentRead(RecordComponentDecl recordComponentDecl, RecordDecl containingRecord) {
		var name = StringUtils.cleanQualifiedNameForType(recordComponentDecl.getType());
		var type = recordComponentDecl.isVarargs() ? "%s[]".formatted(name) : name;
		var caller = getContainingTypeAccessForTypeMember(containingRecord, recordComponentDecl);
		var recordComponentReadCode = "%s %sVal = %s.%s();".formatted(type, recordComponentDecl.getPrettyQualifiedName(), caller, recordComponentDecl.getSimpleName());

		addInstructionToClientMain(recordComponentReadCode);
	}

	public void writeInterfaceExtension(InterfaceDecl interfaceDecl) {
		var interfaceName = "%sExtension".formatted(interfaceDecl.getPrettyQualifiedName());

		insertDeclarationsToInnerInterface(interfaceDecl, interfaceName);
	}

	public void writeInterfaceImplementation(InterfaceDecl interfaceDecl) {
		var necessaryMethods = implementNecessaryMethods(interfaceDecl);

		var interfaceName = "%sMinimal".formatted(interfaceDecl.getPrettyQualifiedName());
		insertDeclarationsToInnerClass(interfaceDecl, interfaceName, "", necessaryMethods);
		addInstructionToClientMain("new %s();".formatted(interfaceName));

		var fullInterfaceName = "%sFull".formatted(interfaceDecl.getPrettyQualifiedName());
		insertDeclarationsToInnerClass(interfaceDecl, fullInterfaceName, "", necessaryMethods);

		var overrideInterfaceName = "%sOverride".formatted(interfaceDecl.getPrettyQualifiedName());
		insertDeclarationsToInnerClass(interfaceDecl, overrideInterfaceName, "", necessaryMethods);
	}

	public void writeMethodDirectInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var caller = getContainingTypeAccessForTypeMember(containingType, methodDecl);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);

		var methodReturn = getReturnHandleForMethod(methodDecl, "Dir");
		var code = "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(exceptions, code);

		if (methodDecl.getFormalTypeParameters().isEmpty()) return;

		var methodReturnWithFormalParams = getReturnHandleForMethod(methodDecl, "DirFormalParams");
		var formalParams = getFormalParamsForExecutableInvocation(methodDecl);
		var codeWithFormalParams = "%s%s.%s%s(%s);".formatted(methodReturnWithFormalParams, caller, formalParams, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(exceptions, codeWithFormalParams);
	}

	public void writeMethodFullDirectInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var caller = generateConstructorDirectInvocationFromInheritance(containingType, "Full");
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);

		var methodReturn = getReturnHandleForMethod(methodDecl, "FullDir");
		var code = "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(exceptions, code);

		if (methodDecl.getFormalTypeParameters().isEmpty()) return;

		var methodReturnWithFormalParams = getReturnHandleForMethod(methodDecl, "FullDirFormalParams");
		var formalParams = getFormalParamsForExecutableInvocation(methodDecl);
		var codeWithFormalParams = "%s%s.%s%s(%s);".formatted(methodReturnWithFormalParams, caller, formalParams, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(exceptions, codeWithFormalParams);
	}

	public void writeMethodMinimalDirectInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var caller = generateConstructorDirectInvocationFromInheritance(containingType, "Minimal", true);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);

		var methodReturn = getReturnHandleForMethod(methodDecl, "MinDir");
		var code = "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(exceptions, code);

		if (methodDecl.getFormalTypeParameters().isEmpty()) return;

		var methodReturnWithFormalParams = getReturnHandleForMethod(methodDecl, "MinDirFormalParams");
		var formalParams = getFormalParamsForExecutableInvocation(methodDecl);
		var codeWithFormalParams = "%s%s.%s%s(%s);".formatted(methodReturnWithFormalParams, caller, formalParams, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(exceptions, codeWithFormalParams);
	}

	public void writeMethodInheritanceInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var caller = methodDecl.isStatic() ? StringUtils.cleanQualifiedNameForType(containingType) : "this";
		var methodName = methodDecl.getSimpleName();
		var params = getParamsForExecutableInvocation(methodDecl);

		var methodInvokeReturn = getReturnHandleForMethod(methodDecl);
		var methodBody = "%s%s.%s(%s);".formatted(methodInvokeReturn, caller, methodName, params);

		if (!methodDecl.getFormalTypeParameters().isEmpty()) {
			var methodReturnWithFormalParams = getReturnHandleForMethod(methodDecl, "FormalParams");
			var formalParams = getFormalParamsForExecutableInvocation(methodDecl);
			methodBody += "\n\t\t%s%s.%s%s(%s);".formatted(methodReturnWithFormalParams, caller, formalParams, methodName, params);
		}

		var paramTypes = formatParamTypeNames(methodDecl.getParameters());
		var invokeMethodName = "%s%sInvoke".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		addNewMethodToInnerType(containingType, invokeMethodName, methodBody, exceptions);

		if (exceptions.isEmpty()) return;

		var tryCatchMethodName = "%s%sTryInvoke".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);
		var tryCatchMethodBody = "try { %s } catch (%s ignored) {}".formatted(methodBody, formatExceptionNames(exceptions, " | "));
		addNewMethodToInnerType(containingType, tryCatchMethodName, tryCatchMethodBody);
	}

	public void writeMethodOverride(MethodDecl methodDecl, TypeDecl containingType) {
		var innerTypeName = "%sOverride".formatted(containingType.getPrettyQualifiedName());
		var overrideMethod = methodDecl.isStatic() ? implementMethod(methodDecl) : overrideMethod(methodDecl);

		insertDeclarationsToInnerClass(containingType, innerTypeName, "", overrideMethod);

		var caller = methodDecl.isStatic() ? innerTypeName : generateConstructorDirectInvocationFromInheritance(containingType, "Override");
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);

		var methodReturn = getReturnHandleForMethod(methodDecl, "Ove");
		addInstructionToClientMain(exceptions, "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params));

		if (methodDecl.getFormalTypeParameters().isEmpty()) return;

		var methodReturnWithFormalParams = getReturnHandleForMethod(methodDecl, "OveFormalParams");
		var formalParams = getFormalParamsForExecutableInvocation(methodDecl);
		var codeWithFormalParams = "%s%s.%s%s(%s);".formatted(methodReturnWithFormalParams, caller, formalParams, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(exceptions, codeWithFormalParams);
	}

	public void writeTypeReference(TypeDecl typeDecl) {
		var referenceVarName = "%sRef".formatted(typeDecl.getPrettyQualifiedName());
		var code = new StringBuilder("%s %s = null;".formatted(StringUtils.cleanQualifiedNameForType(typeDecl), referenceVarName));

		api.getAllSuperTypes(typeDecl).forEach(superType ->
				code.append("\n\t\t%s %sUpcastTo%s = %s;".formatted(StringUtils.cleanQualifiedNameForType(superType), typeDecl.getPrettyQualifiedName(), superType.getPrettyQualifiedName(), referenceVarName))
		);

		addInstructionToClientMain(code.toString());
	}

	public void writeClientFile() {
		try {
			var innerTypesCode = _innerTypes.values().stream()
					.map(InnerType::generateTypeCode)
					.collect(Collectors.joining("\n\n\t"));
			var sortedExceptions = formatExceptionNames(_exceptions.stream().toList());
			var exceptionsCode = sortedExceptions.isBlank() ? "" : " throws %s".formatted(sortedExceptions);

			List<String> mainCalls = new ArrayList<>();
			List<String> methodsInstructions = new ArrayList<>();
			dispatchInstructionsToMethodsAndInvokeThem(
					"callInstructionsWithoutException",
					_notThrowingInstructions,
					mainCalls,
					methodsInstructions
			);
			dispatchInstructionsToMethodsAndInvokeThem(
					"callInstructionsWithException",
					_throwingInstructions,
					exceptionsCode,
					CALL_INSTRUCTIONS_WITH_EXCEPTION_TEMPLATE,
					mainCalls,
					methodsInstructions
			);
			dispatchInstructionsToMethodsAndInvokeThem(
					"callInstructionsWithTryCatch",
					_tryCatchInstructions,
					mainCalls,
					methodsInstructions
			);

			var mainCode = concatDeclarations("\n\t\t", false, mainCalls.toArray(String[]::new));
			var methodsCode = concatDeclarations("\n", false, methodsInstructions.toArray(String[]::new));

			var fullCode = FULL_CLIENT_FILE_TEMPLATE.formatted(
					clientPackageName,
					Constants.CLIENT_FILENAME,
					innerTypesCode,
					exceptionsCode,
					mainCode,
					methodsCode
			).getBytes();

			var packagePath = clientPackageName.replace(".", "/");
			var filePath = outputDir.resolve("%s/FullClient.java".formatted(packagePath));
			filePath.toFile().getParentFile().mkdirs();

			Files.write(filePath, fullCode);
		} catch (IOException e) {
			LOGGER.error("Error writing client code to file: {}", e.getMessage());
		}
	}

	private void dispatchInstructionsToMethodsAndInvokeThem(String instructionsName, List<String> instructions, String exceptionsCode, String template, List<String> mainCalls, List<String> methodsInstructions) {
		int maxLinesByMethod = 1000;

		for (var index = 0; index < instructions.size(); index += maxLinesByMethod) {
			var instructionName = "%s%s".formatted(instructionsName, index / maxLinesByMethod);
			var subInstructionsList = instructions.subList(index, Math.min(index + maxLinesByMethod, instructions.size()));

			mainCalls.add("%s();".formatted(instructionName));

			if (exceptionsCode.isEmpty()) {
				methodsInstructions.add(template.formatted(instructionName, concatDeclarations("\n\t\t", false, subInstructionsList.toArray(String[]::new))));
			} else {
				methodsInstructions.add(template.formatted(instructionName, exceptionsCode, concatDeclarations("\n\t\t", false, subInstructionsList.toArray(String[]::new))));
			}
		}
	}

	private void dispatchInstructionsToMethodsAndInvokeThem(String instructionsName, List<String> instructions, List<String> mainCalls, List<String> methodsInstructions) {
		dispatchInstructionsToMethodsAndInvokeThem(instructionsName, instructions, "", CALL_INSTRUCTIONS_WITHOUT_EXCEPTION_TEMPLATE, mainCalls, methodsInstructions);
	}

	private void insertDeclarationsToInnerClass(TypeDecl superType, String typeName, String constructors, String methods) {
		insertDeclarationsToInnerType(superType, typeName, false, constructors, methods);
	}

	private void insertDeclarationsToInnerInterface(TypeDecl superType, String typeName) {
		insertDeclarationsToInnerType(superType, typeName, true, "", "");
	}

	private void insertDeclarationsToInnerType(TypeDecl superType, String typeName, boolean isInterface, String constructors, String methods) {
		if (!_innerTypes.containsKey(typeName)) {
			var innerType = new InnerType();
			innerType.typeName = typeName;
			innerType.superType = superType;
			innerType.isTypeInterface = isInterface;

			_innerTypes.put(typeName, innerType);
		}

		var innerType = _innerTypes.get(typeName);
		if (!constructors.isBlank()) innerType.constructors.add(constructors);
		if (!methods.isBlank()) innerType.methods.add(methods);
	}

	private void addInstructionToClientMain(String code) {
		addInstructionToClientMain(List.of(), code);
	}

	private void addInstructionToClientMain(List<String> exceptions, String code) {
		this._exceptions.addAll(exceptions);

		if (exceptions.isEmpty()) {
			this._notThrowingInstructions.add(code);
		} else {
			this._throwingInstructions.add(code);
			this._tryCatchInstructions.add("try { %s } catch (%s ignored) {}".formatted(code, formatExceptionNames(exceptions, " | ")));
		}
	}

	private void addNewMethodToInnerType(TypeDecl typeDecl, String methodName, String methodBody, List<String> exceptions) {
		var suffix = "Full";
		var className = "%s%s".formatted(typeDecl.getPrettyQualifiedName(), suffix);

		var newMethod = generateMethodDeclaration(methodName, methodBody, exceptions);
		insertDeclarationsToInnerClass(typeDecl, className, "", newMethod);

		var caller = generateConstructorDirectInvocationFromInheritance(typeDecl, suffix);
		addInstructionToClientMain(exceptions, "%s.%s();".formatted(caller, methodName));
	}

	private void addNewMethodToInnerType(TypeDecl typeDecl, String methodName, String methodBody) {
		addNewMethodToInnerType(typeDecl, methodName, methodBody, List.of());
	}

	private String implementRequiredConstructor(ClassDecl classDecl, String className) {
		if (classDecl.isNested()) {
			return implementRequiredNestedConstructor(classDecl, className);
		}

		var constructors = getSortedConstructors(classDecl);

		if (constructors.isEmpty()) return "";

		var firstConstructor = constructors.getFirst();
		var params = getParamsForExecutableInvocation(firstConstructor);
		var exceptions = getExceptionsForExecutableInvocation(firstConstructor);
		var exceptionsFormatted = formatExceptionNames(exceptions);

		return params.isBlank() && exceptionsFormatted.isBlank()
				? ""
				: "\t%s()%s {\n\t\tsuper(%s);\n\t}\n".formatted(
				className,
				exceptionsFormatted.isBlank() ? "" : " throws %s".formatted(exceptionsFormatted),
				params
		);
	}

	private String implementRequiredNestedConstructor(ClassDecl classDecl, String className) {
		var params = "";
		var exceptionsFormatted = "";

		var enclosingType = classDecl.getEnclosingType().flatMap(eT -> api.resolver().resolve(eT)).orElseThrow();
		if (!classDecl.isStatic() && enclosingType instanceof ClassDecl) {
			var constructors = getSortedConstructors(classDecl);
			if (!constructors.isEmpty()) {
				var firstConstructor = constructors.getFirst();
				params = getParamsForExecutableInvocation(firstConstructor);
				var exceptions = getExceptionsForExecutableInvocation(firstConstructor);
				exceptionsFormatted = formatExceptionNames(exceptions);
			}

			return "\t%s(%s outer)%s {\n\t\touter.super(%s);\n\t}\n".formatted(
					className,
					StringUtils.cleanQualifiedNameForType(enclosingType),
					exceptionsFormatted.isBlank() ? "" : " throws %s".formatted(exceptionsFormatted),
					params
			);
		} else {
			return "";
		}
	}

	private String implementNecessaryMethods(TypeDecl typeDecl) {
		return api.getAllMethodsToImplement(typeDecl).stream()
				.map(this::overrideMethod)
				.collect(Collectors.joining("\n\n"));
	}

	private List<String> getExceptionsForExecutableInvocation(ExecutableDecl executableDecl) {
		return api.getThrownCheckedExceptions(executableDecl).stream()
				.map(StringUtils::cleanQualifiedNameForType)
				.toList();
	}

	private String overrideMethod(MethodDecl methodDecl) {
		return "\t@Override\n" + implementMethod(methodDecl);
	}

	private String implementMethod(MethodDecl methodDecl) {
		var methodReturnTypeName = StringUtils.cleanQualifiedNameForType(methodDecl.getType());
		var methodSignature = methodDecl.toString()
				.replace("abstract ", "")
				.replace("default ", "");

		if (methodDecl.isNative()) {
			return "\t" + methodSignature + ";";
		}

		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		if (!exceptions.isEmpty()) {
			methodSignature += " throws %s".formatted(formatExceptionNames(exceptions));
		}

		return methodReturnTypeName.equals("void")
				? "\t" + methodSignature + " {}"
				: "\t%s { return %s; }".formatted(methodSignature, getDefaultValueForType(methodReturnTypeName));
	}

	private String getContainingTypeAccessForTypeMember(TypeDecl typeDecl, TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.isStatic() || typeDecl instanceof InterfaceDecl) return StringUtils.cleanQualifiedNameForType(typeDecl);
		else if (typeDecl instanceof EnumDecl enumDecl) return generateAccessToFirstEnumValue(enumDecl);
		else if (typeDecl instanceof ClassDecl classDecl) return generateEasiestConstructorInvocationForClass(classDecl);

		throw new IllegalArgumentException("Type member must be static, or type must be enum or class");
	}

	private String generateEasiestConstructorInvocationForClass(ClassDecl classDecl) {
		var sortedConstructors = getSortedConstructors(classDecl);
		var params = sortedConstructors.isEmpty() ? "" : getParamsForExecutableInvocation(sortedConstructors.getFirst());

		return generateConstructorInvocationWithParamsForClass(classDecl, params);
	}

	private String generateConstructorInvocationWithParamsForClass(ClassDecl classDecl, String formalParams, String params) {
		if (classDecl == null) return "";

		if (!classDecl.isNested() || classDecl.isStatic()) {
			return "new %s%s(%s)".formatted(formalParams, StringUtils.cleanQualifiedNameForType(classDecl), params);
		}

		var containingTypes = getContainingTypesForConstructorInvocation(classDecl);
		var code = new StringBuilder();
		var isFirst = true;
		var needsNewAtBeginning = true;
		var needsNewAtLast = true;
		while (!containingTypes.isEmpty()) {
			var containingType = containingTypes.pop();

			if (containingType instanceof EnumDecl containingEnumDecl) {
				code.append("%s.".formatted(generateAccessToFirstEnumValue(containingEnumDecl)));
				needsNewAtBeginning = false;
				needsNewAtLast = true;
			} else if (containingType instanceof ClassDecl containingClassDecl) {
				var sortedConstructors = getSortedConstructors(containingClassDecl);
				var innerParams = sortedConstructors.isEmpty() ? "" : getParamsForExecutableInvocation(sortedConstructors.getFirst());

				if (isFirst) {
					code.append("new %s(%s).".formatted(StringUtils.cleanQualifiedNameForType(containingClassDecl), innerParams));
					needsNewAtBeginning = false;
				} else if (containingClassDecl.isStatic()) {
					code.append("new %s(%s).".formatted(StringUtils.cleanQualifiedNameForType(containingClassDecl), innerParams));
				} else {
					code.append("new %s(%s).".formatted(StringUtils.cleanSimpleNameForType(containingClassDecl), innerParams));
				}

				needsNewAtLast = true;
			} else {
				code.append("%s.".formatted(StringUtils.cleanQualifiedNameForType(containingType)));
				needsNewAtLast = false;
			}

			isFirst = false;
		}

		if (needsNewAtLast && (classDecl.isRecord() || !classDecl.isEnum())) {
			code.append("new %s(%s)".formatted(StringUtils.cleanSimpleNameForType(classDecl), params));
		} else {
			code.append("%s(%s)".formatted(StringUtils.cleanSimpleNameForType(classDecl), params));
		}

		return needsNewAtBeginning ? "new %s".formatted(code) : code.toString();
	}

	private String generateConstructorInvocationWithParamsForClass(ClassDecl classDecl, String params) {
		return generateConstructorInvocationWithParamsForClass(classDecl, "", params);
	}

	private String generateConstructorDirectInvocationFromInheritance(TypeDecl typeDecl, String suffix, boolean withUpCast) {
		var constructorName = "%s%s".formatted(typeDecl.getPrettyQualifiedName(), suffix);
		var constructorInvocation = "new %s()".formatted(constructorName);

		if (!typeDecl.isStatic()) {
			var enclosingType = typeDecl.getEnclosingType().flatMap(eT -> api.resolver().resolve(eT)).orElse(null);
			if (enclosingType instanceof ClassDecl) {
				constructorInvocation = "new %s((%s) null)".formatted(constructorName, StringUtils.cleanQualifiedNameForType(enclosingType));
			}
		}

		return withUpCast
				? "((%s) %s)".formatted(StringUtils.cleanQualifiedNameForType(typeDecl), constructorInvocation)
				: constructorInvocation;
	}

	private String generateConstructorDirectInvocationFromInheritance(TypeDecl typeDecl, String suffix) {
		return generateConstructorDirectInvocationFromInheritance(typeDecl, suffix, false);
	}

	private Stack<TypeDecl> getContainingTypesForConstructorInvocation(TypeDecl typeDecl) {
		Stack<TypeDecl> containingTypes = new Stack<>();
		TypeDecl currentType = typeDecl;

		while (currentType != null && currentType.isNested()) {
			var enclosingType = currentType.getEnclosingType()
					.flatMap(tR -> api.resolver().resolve(tR))
					.orElse(null);

			if (enclosingType == null) {
				currentType = null;
			} else {
				containingTypes.push(enclosingType);

				if (enclosingType.isStatic() || enclosingType.isEnum() || enclosingType.isRecord() || enclosingType.isInterface())
					currentType = null;
				else
					currentType = enclosingType;
			}
		}

		return containingTypes;
	}

	private static String concatDeclarations(String delimiter, boolean isDefault, String... declarations) {
		return Arrays.stream(declarations)
				.filter(decl -> !decl.isBlank())
				.collect(Collectors.joining(delimiter));
	}

	private static String concatDeclarations(String... declarations) {
		return concatDeclarations("\n\n", true, declarations);
	}

	private static String generateAccessToFirstEnumValue(EnumDecl enumDecl) {
		var enumValue = "";
		if (enumDecl.getValues().isEmpty()) {
			var valuesAsFields = enumDecl.getDeclaredFields().stream().filter(f -> f.isStatic() && f.isFinal());
			enumValue = valuesAsFields.findFirst().map(Symbol::getSimpleName).orElse("");
		} else {
			enumValue = enumDecl.getValues().getFirst().toString();
		}

		return "%s.%s".formatted(StringUtils.cleanQualifiedNameForType(enumDecl), enumValue);
	}

	private static String getParamsForExecutableInvocation(ExecutableDecl executableDecl) {
		return executableDecl.getParameters().stream()
				.map(p -> {
					var value = getDefaultValueForType(StringUtils.cleanQualifiedNameForType(p.type()));

					return p.isVarargs() ? "%s, %s".formatted(value, value) : value;
				})
				.collect(Collectors.joining(", "));
	}

	private static String getFormalParamsForExecutableInvocation(ExecutableDecl executableDecl) {
		return executableDecl.getFormalTypeParameters().isEmpty()
			? ""
			: "<%s>".formatted(executableDecl.getFormalTypeParameters().stream()
				.map(p -> getDefaultTypeForBounds(p.bounds()))
				.collect(Collectors.joining(", ")));
	}

	private static String getReturnHandleForMethod(MethodDecl methodDecl, String suffix) {
		if (methodDecl.getType().getQualifiedName().equals("void")) return "";

		var varType = StringUtils.cleanQualifiedNameForType(methodDecl.getType());
		var paramTypes = formatParamTypeNames(methodDecl.getParameters());
		var varName = "%s%s%sVal".formatted(methodDecl.getPrettyQualifiedName(), paramTypes, suffix);
		return "%s %s = ".formatted(varType, varName);
	}

	private static String getReturnHandleForMethod(MethodDecl methodDecl) {
		return getReturnHandleForMethod(methodDecl, "");
	}

	private static String generateMethodDeclaration(String methodName, String methodBody) {
		return "\tpublic void %s() {\n\t\t%s\n\t}".formatted(methodName, methodBody);
	}

	private static String generateMethodDeclaration(String methodName, String methodBody, List<String> exceptions) {
		var exceptionsFormatted = formatExceptionNames(exceptions);

		return exceptionsFormatted.isBlank()
				? generateMethodDeclaration(methodName, methodBody)
				: "\tpublic void %s() throws %s {\n\t\t%s\n\t}".formatted(methodName, exceptionsFormatted, methodBody);
	}

	private static String getDefaultValueForType(String typeName) {
		if (typeName.contains("String") && !typeName.contains("[]")) return "\"\"";

		return switch (typeName) {
			case "int", "long", "float", "double", "byte", "short" -> "0";
			case "char" -> "'c'";
			case "boolean" -> "false";
			default -> "(%s) null".formatted(typeName);
		};
	}

	private static String getDefaultTypeForBounds(List<ITypeReference> bounds) {
		if (bounds.isEmpty()) return "";

		if (bounds.size() == 1) {
			return bounds.getFirst().getQualifiedName();
		}

		if (bounds.stream().anyMatch(b -> b.getQualifiedName().equals("java.lang.Number"))) {
			return "Integer";
		}

		return "java.lang.Object";
	}

	private static List<ConstructorDecl> getSortedConstructors(ClassDecl classDecl) {
		return classDecl.getDeclaredConstructors().stream()
				.sorted(Comparator.comparingInt(c -> c.getParameters().size()))
				.toList();
	}

	private static String formatParamTypeNames(List<ParameterDecl> params) {
		return params.stream()
				.map(p -> {
					var typeName = p.type().getPrettyQualifiedName();
					return p.isVarargs() ? "%sVarArgs".formatted(typeName) : typeName;
				})
				.collect(Collectors.joining());
	}

	private static String formatExceptionNames(List<String> exceptions, String delimiter) {
		return exceptions.stream().sorted().collect(Collectors.joining(delimiter));
	}

	private static String formatExceptionNames(List<String> exceptions) {
		return formatExceptionNames(exceptions, ", ");
	}

	private static final class InnerType {
		public String typeName;
		public TypeDecl superType;
		public boolean isTypeInterface;

		public List<String> constructors = new ArrayList<>();
		public List<String> methods = new ArrayList<>();

		public String generateTypeCode() {
			var constructors = String.join("\n\n", this.constructors);
			var methods = String.join("\n\n", this.methods);
			var typeBody = concatDeclarations(constructors, methods);

			var typeNameFormatted = typeName;
			var superTypeNameFormatted = StringUtils.cleanQualifiedNameForType(superType);
			if (!superType.getFormalTypeParameters().isEmpty()) {
				typeNameFormatted += "<%s>".formatted(superType.getFormalTypeParameters().stream()
						.map(FormalTypeParameter::toString)
						.collect(Collectors.joining(", ")));

				superTypeNameFormatted += "<%s>".formatted(superType.getFormalTypeParameters().stream()
						.map(FormalTypeParameter::name)
						.collect(Collectors.joining(", ")));
			}

			if (isTypeInterface) {
				return INTERFACE_EXTENSION_TEMPLATE.formatted(typeNameFormatted, superTypeNameFormatted);
			}

			var template = superType.isInterface() ? INTERFACE_IMPLEMENTATION_TEMPLATE : CLASS_EXTENSION_TEMPLATE;

			return String.join("\n\t", template.formatted(typeNameFormatted, superTypeNameFormatted, typeBody).split("\n"));
		}
	}
}
