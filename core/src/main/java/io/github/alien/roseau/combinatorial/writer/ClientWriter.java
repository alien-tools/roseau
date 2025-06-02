package io.github.alien.roseau.combinatorial.writer;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.model.reference.ITypeReference;
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
		addInstructionToClientMain("new %s();".formatted(inheritanceClassName));

		var fullClassName = "%sFull".formatted(classDecl.getPrettyQualifiedName());
		var fullConstructorRequired = implementRequiredConstructor(classDecl, fullClassName);
		insertDeclarationsToInnerClass(classDecl, fullClassName, fullConstructorRequired, necessaryMethods);

		var overrideClassName = "%sOverride".formatted(classDecl.getPrettyQualifiedName());
		var overrideConstructorRequired = implementRequiredConstructor(classDecl, overrideClassName);
		insertDeclarationsToInnerClass(classDecl, overrideClassName, overrideConstructorRequired, necessaryMethods);
	}

	public void writeInnerClassInheritance(ClassDecl classDecl) {
		var enclosingType = classDecl.getEnclosingType().map(eT -> eT.getResolvedApiType().orElseThrow()).orElseThrow();
		var innerInheritanceName = "Inner%s".formatted(classDecl.getSimpleName().split("\\$")[1]);
		var inheritanceClassName = "%sIn%sMinimal".formatted(innerInheritanceName, enclosingType.getPrettyQualifiedName());

		var inheritanceConstructorRequired = implementRequiredConstructor(classDecl, inheritanceClassName);
		var necessaryMethods = implementNecessaryMethods(classDecl);

		var innerClassCode = "\tpublic class %s extends %s {}".formatted(innerInheritanceName, classDecl.getQualifiedName());

		insertDeclarationsToInnerClass(enclosingType, inheritanceClassName, innerClassCode, inheritanceConstructorRequired, necessaryMethods);
		addInstructionToClientMain("new %s().new %s();".formatted(inheritanceClassName, innerInheritanceName));
	}

	public void writeConstructorDirectInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var params = getParamsForExecutableInvocation(constructorDecl);
		var code = "new %s(%s);".formatted(containingClass.getQualifiedName(), params);

		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);
		addInstructionToClientMain(exceptions, code);
	}

	public void writeConstructorInheritanceInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var innerTypeName = "%sFull".formatted(containingClass.getPrettyQualifiedName());

		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);
		var formattedExceptions = formatExceptionNames(exceptions);
		var paramsValue = getParamsForExecutableInvocation(constructorDecl);

		var constructor = "\t%s%s {\n\t\tsuper(%s);\n\t}".formatted(
				constructorDecl.toString().replace(constructorDecl.getSimpleName(), innerTypeName),
				formattedExceptions.isBlank() ? "" : " throws %s".formatted(formattedExceptions),
				paramsValue
		);

		insertDeclarationsToInnerClass(containingClass, innerTypeName, constructor, "");
		addInstructionToClientMain(exceptions, "new %s(%s);".formatted(innerTypeName, paramsValue));
	}

	public void writeExceptionCatch(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "try {\n\t\t\tthrow %s;\n\t\t} catch (%s e) {}".formatted(constructor, classDecl.getQualifiedName());

		addInstructionToClientMain(code);
	}

	public void writeExceptionThrow(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "throw %s;".formatted(constructor);

		var exceptions = new ArrayList<String>();
		if (api.isCheckedException(classDecl)) {
			exceptions.add(classDecl.getQualifiedName());
		}

		addInstructionToClientMain(exceptions, code);
	}

	public void writeExceptionThrows(ClassDecl classDecl) {
		this._exceptions.add(classDecl.getQualifiedName());
	}

	public void writeEnumValueRead(EnumValueDecl enumValueDecl, EnumDecl containingEnum) {
		var caller = getContainingTypeAccessForTypeMember(containingEnum, enumValueDecl);
		var enumValueReadCode = "%s %sVal = %s.%s;".formatted(containingEnum.getQualifiedName(), enumValueDecl.getPrettyQualifiedName(), caller, enumValueDecl.getSimpleName());

		addInstructionToClientMain(enumValueReadCode);
	}

	public void writeReadFieldThroughContainingType(FieldDecl fieldDecl, TypeDecl containingType) {
		var type = fieldDecl.getType().getQualifiedName();
		var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
		var fieldReadCode = "%s %sVal = %s.%s;".formatted(type, fieldDecl.getPrettyQualifiedName(), caller, fieldDecl.getSimpleName());

		addInstructionToClientMain(fieldReadCode);
	}

	public void writeReadFieldThroughMethodCall(FieldDecl fieldDecl, TypeDecl containingType) {
		var innerTypeName = "%sFull".formatted(containingType.getPrettyQualifiedName());
		var readFieldMethodName = "%sFieldRead".formatted(fieldDecl.getPrettyQualifiedName());

		var type = fieldDecl.getType().getQualifiedName();
		var caller = fieldDecl.isStatic() ? containingType.getQualifiedName() : "this";
		var readMethodBody = "%s val = %s.%s;".formatted(type, caller, fieldDecl.getSimpleName());

		addNewMethodToInnerType(innerTypeName, readFieldMethodName, readMethodBody, containingType);
	}

	public void writeReadFieldThroughSubType(FieldDecl fieldDecl, TypeDecl containingType) {
		var innerTypeName = "%sFull".formatted(containingType.getPrettyQualifiedName());

		var type = fieldDecl.getType().getQualifiedName();
		var inheritedCaller = fieldDecl.isStatic() ? innerTypeName : "new %s()".formatted(innerTypeName);
		var fieldReadCode = "%s %sInhVal = %s.%s;".formatted(type, fieldDecl.getPrettyQualifiedName(), inheritedCaller, fieldDecl.getSimpleName());
		addInstructionToClientMain(fieldReadCode);
	}

	public void writeWriteFieldThroughContainingType(FieldDecl fieldDecl, TypeDecl containingType) {
		var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
		var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());
		var fieldWriteCode = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

		addInstructionToClientMain(fieldWriteCode);
	}

	public void writeWriteFieldThroughMethodCall(FieldDecl fieldDecl, TypeDecl containingType) {
		var innerTypeName = "%sFull".formatted(containingType.getPrettyQualifiedName());
		var writeFieldMethodName = "%sFieldWrite".formatted(fieldDecl.getPrettyQualifiedName());

		var caller = fieldDecl.isStatic() ? containingType.getQualifiedName() : "this";
		var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());
		var writeMethodBody = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

		addNewMethodToInnerType(innerTypeName, writeFieldMethodName, writeMethodBody, containingType);
	}

	public void writeWriteFieldThroughSubType(FieldDecl fieldDecl, TypeDecl containingType) {
		var innerTypeName = "%sFull".formatted(containingType.getPrettyQualifiedName());

		var inheritedCaller = fieldDecl.isStatic() ? innerTypeName : "new %s()".formatted(innerTypeName);
		var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());
		var fieldWriteCode = "%s.%s = %s;".formatted(inheritedCaller, fieldDecl.getSimpleName(), value);
		addInstructionToClientMain(fieldWriteCode);
	}

	public void writeRecordComponentRead(RecordComponentDecl recordComponentDecl, RecordDecl containingRecord) {
		var type = recordComponentDecl.isVarargs() ? "%s[]".formatted(recordComponentDecl.getType().getQualifiedName()) : recordComponentDecl.getType().getQualifiedName();
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

	public void writeInnerInterfaceExtension(InterfaceDecl interfaceDecl) {
		var enclosingType = interfaceDecl.getEnclosingType().map(eT -> eT.getResolvedApiType().orElseThrow()).orElseThrow();
		var innerExtensionName = "Inner%s".formatted(interfaceDecl.getSimpleName().split("\\$")[1]);
		var extensionInterfaceName = "%sExtensionIn%sMinimal".formatted(innerExtensionName, enclosingType.getPrettyQualifiedName());

		var extensionConstructorRequired = implementRequiredConstructor(interfaceDecl, extensionInterfaceName);
		var necessaryMethods = implementNecessaryMethods(interfaceDecl);

		var innerClassCode = "\tpublic interface %s extends %s {}".formatted(innerExtensionName, interfaceDecl.getQualifiedName());

		insertDeclarationsToInnerClass(enclosingType, extensionInterfaceName, innerClassCode, extensionConstructorRequired, necessaryMethods);
		addInstructionToClientMain("new %s.%s() {};".formatted(extensionInterfaceName, innerExtensionName));
	}

	public void writeInnerInterfaceImplementation(InterfaceDecl interfaceDecl) {
		var enclosingType = interfaceDecl.getEnclosingType().map(eT -> eT.getResolvedApiType().orElseThrow()).orElseThrow();
		var innerImplementationName = "Inner%s".formatted(interfaceDecl.getSimpleName().split("\\$")[1]);
		var implementationClassName = "%sImplementationIn%sMinimal".formatted(innerImplementationName, enclosingType.getPrettyQualifiedName());

		var implementationConstructorRequired = implementRequiredConstructor(interfaceDecl, implementationClassName);
		var necessaryMethods = implementNecessaryMethods(interfaceDecl);

		var innerClassCode = "\tpublic class %s implements %s {}".formatted(innerImplementationName, interfaceDecl.getQualifiedName());

		insertDeclarationsToInnerClass(enclosingType, implementationClassName, innerClassCode, implementationConstructorRequired, necessaryMethods);
		addInstructionToClientMain("new %s().new %s();".formatted(implementationClassName, innerImplementationName));
	}

	public void writeMethodFullDirectInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var minimalTypeName = "%sFull".formatted(containingType.getPrettyQualifiedName());
		var params = getParamsForExecutableInvocation(methodDecl);
		var methodReturn = getReturnHandleForMethod(methodDecl, "FullDir");
		var methodInvocationCode = "%snew %s().%s(%s);".formatted(methodReturn, minimalTypeName, methodDecl.getSimpleName(), params);

		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		addInstructionToClientMain(exceptions, methodInvocationCode);
	}

	public void writeMethodDirectInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var caller = getContainingTypeAccessForTypeMember(containingType, methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);
		var methodReturn = getReturnHandleForMethod(methodDecl, "Dir");
		var methodInvocationCode = "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params);

		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		addInstructionToClientMain(exceptions, methodInvocationCode);
	}

	public void writeMethodMinimalDirectInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var minimalTypeName = "%sMinimal".formatted(containingType.getPrettyQualifiedName());
		var params = getParamsForExecutableInvocation(methodDecl);
		var methodReturn = getReturnHandleForMethod(methodDecl, "MinDir");
		var methodInvocationCode = "%snew %s().%s(%s);".formatted(methodReturn, minimalTypeName, methodDecl.getSimpleName(), params);

		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		addInstructionToClientMain(exceptions, methodInvocationCode);
	}

	public void writeMethodInheritanceInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var fullTypeName = "%sFull".formatted(containingType.getPrettyQualifiedName());
		var paramTypes = formatParamTypeNames(methodDecl.getParameters());
		var invokeMethodName = "%s%sInvoke".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);

		var methodInvokeReturn = getReturnHandleForMethod(methodDecl);
		var caller = methodDecl.isStatic() ? containingType.getQualifiedName() : "this";
		var methodName = methodDecl.getSimpleName();
		var params = getParamsForExecutableInvocation(methodDecl);
		var methodBody = "%s%s.%s(%s);".formatted(methodInvokeReturn, caller, methodName, params);

		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		addNewMethodToInnerType(fullTypeName, invokeMethodName, methodBody, exceptions, containingType);

		if (exceptions.isEmpty()) return;

		var tryCatchMethodName = "%s%sTryInvoke".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);
		var tryCatchMethodBody = "try { %s } catch (%s ignored) {}".formatted(methodBody, formatExceptionNames(exceptions, " | "));
		addNewMethodToInnerType(fullTypeName, tryCatchMethodName, tryCatchMethodBody, containingType);
	}

	public void writeMethodOverride(MethodDecl methodDecl, TypeDecl containingType) {
		var innerTypeName = "%sOverride".formatted(containingType.getPrettyQualifiedName());
		var overrideMethod = methodDecl.isStatic() ? implementMethod(methodDecl) : overrideMethod(methodDecl);

		insertDeclarationsToInnerClass(containingType, innerTypeName, "", overrideMethod);

		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		var methodReturn = getReturnHandleForMethod(methodDecl, "Ove");
		var caller = methodDecl.isStatic() ? innerTypeName : "new %s()".formatted(innerTypeName);
		var params = getParamsForExecutableInvocation(methodDecl);
		addInstructionToClientMain(exceptions, "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params));
	}

	public void writeTypeReference(TypeDecl typeDecl) {
		var referenceVarName = "%sRef".formatted(typeDecl.getPrettyQualifiedName());
		var code = new StringBuilder("%s %s = null;".formatted(typeDecl.getQualifiedName(), referenceVarName));

		typeDecl.getAllSuperTypes().forEach(superType ->
				code.append("\n\t\t%s %sUpcastTo%s = %s;".formatted(superType.getQualifiedName(), typeDecl.getPrettyQualifiedName(), superType.getPrettyQualifiedName(), referenceVarName))
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

	private void insertDeclarationsToInnerClass(TypeDecl superType, String typeName, String innerTypes, String constructors, String methods) {
		insertDeclarationsToInnerType(superType, typeName, false, innerTypes, constructors, methods);
	}

	private void insertDeclarationsToInnerClass(TypeDecl superType, String typeName, String constructors, String methods) {
		insertDeclarationsToInnerType(superType, typeName, false, "", constructors, methods);
	}

	private void insertDeclarationsToInnerInterface(TypeDecl superType, String typeName) {
		insertDeclarationsToInnerType(superType, typeName, true, "", "", "");
	}

	private void insertDeclarationsToInnerType(TypeDecl superType, String typeName, boolean isInterface, String innerTypes, String constructors, String methods) {
		if (!_innerTypes.containsKey(typeName)) {
			var innerType = new InnerType();
			innerType.typeName = typeName;
			innerType.superType = superType;
			innerType.isTypeInterface = isInterface;

			_innerTypes.put(typeName, innerType);
		}

		var innerType = _innerTypes.get(typeName);
		if (!innerTypes.isBlank()) innerType.innerTypes.add(innerTypes);
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

	private void addNewMethodToInnerType(String className, String methodName, String methodBody, List<String> exceptions, TypeDecl containingType) {
		var newMethod = generateMethodDeclaration(methodName, methodBody, exceptions);

		insertDeclarationsToInnerClass(containingType, className, "", newMethod);
		addInstructionToClientMain(exceptions, "new %s().%s();".formatted(className, methodName));
	}

	private void addNewMethodToInnerType(String className, String methodName, String methodBody, TypeDecl containingType) {
		addNewMethodToInnerType(className, methodName, methodBody, List.of(), containingType);
	}

	private String implementRequiredConstructor(TypeDecl typeDecl, String className) {
		if (typeDecl instanceof ClassDecl classDecl) {
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

		return "";
	}

	private String implementNecessaryMethods(TypeDecl typeDecl) {
		return api.getAllMethodsToImplement(typeDecl).stream()
				.map(this::overrideMethod)
				.collect(Collectors.joining("\n\n"));
	}

	private List<String> getExceptionsForExecutableInvocation(ExecutableDecl executableDecl) {
		return api.getThrownCheckedExceptions(executableDecl).stream()
				.map(ITypeReference::getQualifiedName)
				.toList();
	}

	private String overrideMethod(MethodDecl methodDecl) {
		return "\t@Override\n" + implementMethod(methodDecl);
	}

	private String implementMethod(MethodDecl methodDecl) {
		var methodReturnTypeName = methodDecl.getType().getQualifiedName();
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

	private static String concatDeclarations(String delimiter, boolean isDefault, String... declarations) {
		return Arrays.stream(declarations)
				.filter(decl -> !decl.isBlank())
				.collect(Collectors.joining(delimiter));
	}

	private static String concatDeclarations(String... declarations) {
		return concatDeclarations("\n\n", true, declarations);
	}

	private static String getContainingTypeAccessForTypeMember(TypeDecl typeDecl, TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.isStatic()) return typeDecl.getQualifiedName();
		else if (typeDecl instanceof InterfaceDecl) return typeDecl.getQualifiedName();
		else if (typeDecl instanceof EnumDecl enumDecl) return generateAccessToFirstEnumValue(enumDecl);
		else if (typeDecl instanceof RecordDecl recordDecl) return generateConstructorInvocationForRecord(recordDecl);
		else if (typeDecl instanceof ClassDecl classDecl) return generateEasiestConstructorInvocationForClass(classDecl);

		throw new IllegalArgumentException("Type member must be static, or type must be enum or class");
	}

	private static String generateAccessToFirstEnumValue(EnumDecl enumDecl) {
		return "%s.%s".formatted(enumDecl.getQualifiedName(), enumDecl.getValues().getFirst());
	}

	private static String generateEasiestConstructorInvocationForClass(ClassDecl classDecl) {
		var sortedConstructors = getSortedConstructors(classDecl);

		if (sortedConstructors.isEmpty()) return "new %s()".formatted(classDecl.getQualifiedName());

		var params = getParamsForExecutableInvocation(sortedConstructors.getFirst());
		return "new %s(%s)".formatted(classDecl.getQualifiedName(), params);
	}

	private static String generateConstructorInvocationForRecord(RecordDecl recordDecl) {
		var recordsComponents = recordDecl.getRecordComponents();

		if (recordsComponents.isEmpty()) return "new %s()".formatted(recordDecl.getQualifiedName());

		var params = getValuesForRecordComponents(recordsComponents);
		return "new %s(%s)".formatted(recordDecl.getQualifiedName(), params);
	}

	private static String getParamsForExecutableInvocation(ExecutableDecl executableDecl) {
		return executableDecl.getParameters().stream()
				.map(p -> {
					var value = getDefaultValueForType(p.type().getQualifiedName());

					return p.isVarargs() ? "%s, %s".formatted(value, value) : value;
				})
				.collect(Collectors.joining(", "));
	}

	private static String getValuesForRecordComponents(List<RecordComponentDecl> recordComponents) {
		return recordComponents.stream()
				.map(rC -> {
					var value = getDefaultValueForType(rC.getType().getQualifiedName());

					return rC.isVarargs() ? "%s, %s".formatted(value, value) : value;
				})
				.collect(Collectors.joining(", "));
	}

	private static String getReturnHandleForMethod(MethodDecl methodDecl, String suffix) {
		if (methodDecl.getType().getQualifiedName().equals("void")) return "";

		var varType = methodDecl.getType().getQualifiedName();
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

		public List<String> innerTypes = new ArrayList<>();
		public List<String> constructors = new ArrayList<>();
		public List<String> methods = new ArrayList<>();

		public String generateTypeCode() {
			var innerTypes = String.join("\n\n\t", this.innerTypes);
			var constructors = String.join("\n\n", this.constructors);
			var methods = String.join("\n\n", this.methods);
			var typeBody = concatDeclarations(innerTypes, constructors, methods);

			if (isTypeInterface) {
				return INTERFACE_EXTENSION_TEMPLATE.formatted(typeName, superType.getQualifiedName());
			}

			var template = superType.isInterface() ? INTERFACE_IMPLEMENTATION_TEMPLATE : CLASS_EXTENSION_TEMPLATE;

			return String.join("\n\t", template.formatted(typeName, superType.getQualifiedName(), typeBody).split("\n"));
		}
	}
}
