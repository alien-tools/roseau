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
	private static final Logger LOGGER = LogManager.getLogger(ClientWriter.class);

	private static final String clientPackageName = Constants.CLIENT_FOLDER;

	private final Set<String> imports = new HashSet<>();
	private final Map<String, InnerType> innerTypes = new HashMap<>();
	private final Set<String> exceptions = new HashSet<>();
	private final List<String> notThrowingInstructions = new ArrayList<>();
	private final List<String> throwingInstructions = new ArrayList<>();
	private final List<String> tryCatchInstructions = new ArrayList<>();

	public ClientWriter(Path outputDir) {
		super(outputDir);
	}

	public void writeClassInheritance(ClassDecl classDecl) {
		var necessaryMethods = implementNecessaryMethods(classDecl);

		var inheritanceClassName = "%sMinimal".formatted(classDecl.getPrettyQualifiedName());
		var inheritanceConstructorRequired = implementRequiredConstructor(classDecl, inheritanceClassName);
		insertDeclarationsToInnerType(classDecl, inheritanceClassName, inheritanceConstructorRequired, necessaryMethods);
		addInstructionToClientMain("new %s();".formatted(inheritanceClassName));

		var fullClassName = "%sFull".formatted(classDecl.getPrettyQualifiedName());
		var fullConstructorRequired = implementRequiredConstructor(classDecl, fullClassName);
		insertDeclarationsToInnerType(classDecl, fullClassName, fullConstructorRequired, necessaryMethods);

		var overrideClassName = "%sOverride".formatted(classDecl.getPrettyQualifiedName());
		var overrideConstructorRequired = implementRequiredConstructor(classDecl, overrideClassName);
		insertDeclarationsToInnerType(classDecl, overrideClassName, overrideConstructorRequired, necessaryMethods);
	}

	public void writeConstructorDirectInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var params = getParamsForExecutableInvocation(constructorDecl);
		var code = "new %s(%s);".formatted(containingClass.getSimpleName(), params);

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

		insertDeclarationsToInnerType(containingClass, innerTypeName, constructor, "");
		addInstructionToClientMain(exceptions, "new %s(%s);".formatted(innerTypeName, paramsValue));
	}

	public void writeExceptionCatch(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "try {\n\t\t\tthrow %s;\n\t\t} catch (%s e) {}".formatted(constructor, classDecl.getSimpleName());

		addInstructionToClientMain(code);
	}

	public void writeExceptionThrow(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "throw %s;".formatted(constructor);

		var exceptions = new ArrayList<String>();
		if (classDecl.isCheckedException()) {
			exceptions.add(classDecl.getSimpleName());
		}

		addInstructionToClientMain(exceptions, code);
	}

	public void writeExceptionThrows(ClassDecl classDecl) {
		this.exceptions.add(classDecl.getSimpleName());
	}

	public void writeEnumValueRead(EnumValueDecl enumValueDecl, EnumDecl containingEnum) {
		var caller = getContainingTypeAccessForTypeMember(containingEnum, enumValueDecl);
		var enumValueReadCode = "%s %sVal = %s.%s;".formatted(containingEnum.getSimpleName(), enumValueDecl.getPrettyQualifiedName(), caller, enumValueDecl.getSimpleName());

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
		var caller = fieldDecl.isStatic() ? containingType.getSimpleName() : "this";
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

		var caller = fieldDecl.isStatic() ? containingType.getSimpleName() : "this";
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

		insertDeclarationsToInnerType(interfaceDecl, interfaceName, "", "");
	}

	public void writeInterfaceImplementation(InterfaceDecl interfaceDecl) {
		var necessaryMethods = implementNecessaryMethods(interfaceDecl);

		var interfaceName = "%sMinimal".formatted(interfaceDecl.getPrettyQualifiedName());
		insertDeclarationsToInnerType(interfaceDecl, interfaceName, "", necessaryMethods);
		addInstructionToClientMain("new %s();".formatted(interfaceName));

		var fullInterfaceName = "%sFull".formatted(interfaceDecl.getPrettyQualifiedName());
		insertDeclarationsToInnerType(interfaceDecl, fullInterfaceName, "", necessaryMethods);

		var overrideInterfaceName = "%sOverride".formatted(interfaceDecl.getPrettyQualifiedName());
		insertDeclarationsToInnerType(interfaceDecl, overrideInterfaceName, "", necessaryMethods);
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
		var caller = methodDecl.isStatic() ? containingType.getSimpleName() : "this";
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

		insertDeclarationsToInnerType(containingType, innerTypeName, "", overrideMethod);

		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		var methodReturn = getReturnHandleForMethod(methodDecl, "Ove");
		var caller = methodDecl.isStatic() ? innerTypeName : "new %s()".formatted(innerTypeName);
		var params = getParamsForExecutableInvocation(methodDecl);
		addInstructionToClientMain(exceptions, "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params));
	}

	public void writeTypeReference(TypeDecl typeDecl) {
		var imports = getImportsForType(typeDecl);
		this.imports.addAll(imports);

		var code = "%s %sRef;".formatted(typeDecl.getSimpleName(), typeDecl.getPrettyQualifiedName());

		addInstructionToClientMain(code);
	}

	public void writeClientFile() {
		try {
			var sortedImports = imports.stream().sorted().map("import %s;"::formatted).collect(Collectors.joining("\n"));
			var innerTypesCode = innerTypes.values().stream()
					.map(InnerType::generateTypeCode)
					.collect(Collectors.joining("\n\n\t"));
			var sortedExceptions = formatExceptionNames(exceptions.stream().toList());
			var exceptionsCode = sortedExceptions.isBlank() ? "" : " throws %s".formatted(sortedExceptions);
			var notThrowingCode = String.join("\n\t\t", notThrowingInstructions);
			var throwingCode = String.join("\n\t\t", throwingInstructions);
			var tryCatchCode = String.join("\n\t\t", tryCatchInstructions);

			var fullCode = FULL_CLIENT_FILE_TEMPLATE.formatted(
					clientPackageName,
					sortedImports,
					Constants.CLIENT_FILENAME,
					innerTypesCode,
					exceptionsCode,
					notThrowingCode,
					exceptionsCode,
					throwingCode,
					tryCatchCode
			).getBytes();

			var packagePath = clientPackageName.replace(".", "/");
			var filePath = outputDir.resolve("%s/FullClient.java".formatted(packagePath));
			filePath.toFile().getParentFile().mkdirs();

			Files.write(filePath, fullCode);
		} catch (IOException e) {
			LOGGER.error("Error writing client code to file: {}", e.getMessage());
		}
	}

	private void insertDeclarationsToInnerType(TypeDecl superType, String typeName, String constructors, String methods) {
		if (innerTypes.containsKey(typeName)) {
			var innerType = innerTypes.get(typeName);
			if (!constructors.isBlank()) innerType.constructors.add(constructors);
			if (!methods.isBlank()) innerType.methods.add(methods);
		} else {
			var innerType = new InnerType();
			innerType.typeName = typeName;
			innerType.superType = superType;
			if (!constructors.isBlank()) innerType.constructors.add(constructors);
			if (!methods.isBlank()) innerType.methods.add(methods);

			innerTypes.put(typeName, innerType);
		}
	}

	private void addInstructionToClientMain(String code) {
		addInstructionToClientMain(List.of(), code);
	}

	private void addInstructionToClientMain(List<String> exceptions, String code) {
		this.exceptions.addAll(exceptions);

		if (exceptions.isEmpty()) {
			this.notThrowingInstructions.add(code);
		} else {
			this.throwingInstructions.add(code);
			this.tryCatchInstructions.add("try { %s } catch (%s ignored) {}".formatted(code, formatExceptionNames(exceptions, " | ")));
		}
	}

	private static List<String> getImportsForType(TypeDecl typeDecl) {
		return List.of(typeDecl.getQualifiedName());
	}

	private void addNewMethodToInnerType(String className, String methodName, String methodBody, List<String> exceptions, TypeDecl containingType) {
		var newMethod = generateMethodDeclaration(methodName, methodBody, exceptions);

		insertDeclarationsToInnerType(containingType, className, "", newMethod);
		addInstructionToClientMain(exceptions, "new %s().%s();".formatted(className, methodName));
	}

	private void addNewMethodToInnerType(String className, String methodName, String methodBody, TypeDecl containingType) {
		addNewMethodToInnerType(className, methodName, methodBody, List.of(), containingType);
	}

	private static String concatDeclarations(String... declarations) {
		return Arrays.stream(declarations)
				.filter(decl -> !decl.isBlank())
				.collect(Collectors.joining("\n\n"));
	}

	private static String getContainingTypeAccessForTypeMember(TypeDecl typeDecl, TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.isStatic()) return typeDecl.getSimpleName();
		else if (typeDecl instanceof InterfaceDecl) return typeDecl.getSimpleName();
		else if (typeDecl instanceof EnumDecl enumDecl) return generateAccessToFirstEnumValue(enumDecl);
		else if (typeDecl instanceof RecordDecl recordDecl) return generateConstructorInvocationForRecord(recordDecl);
		else if (typeDecl instanceof ClassDecl classDecl) return generateEasiestConstructorInvocationForClass(classDecl);

		throw new IllegalArgumentException("Type member must be static, or type must be enum or class");
	}

	private static String implementRequiredConstructor(TypeDecl typeDecl, String className) {
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

	private static String generateAccessToFirstEnumValue(EnumDecl enumDecl) {
		return "%s.%s".formatted(enumDecl.getSimpleName(), enumDecl.getValues().getFirst());
	}

	private static String generateEasiestConstructorInvocationForClass(ClassDecl classDecl) {
		var sortedConstructors = getSortedConstructors(classDecl);

		if (sortedConstructors.isEmpty()) return "new %s()".formatted(classDecl.getSimpleName());

		var params = getParamsForExecutableInvocation(sortedConstructors.getFirst());
		return "new %s(%s)".formatted(classDecl.getSimpleName(), params);
	}

	private static String generateConstructorInvocationForRecord(RecordDecl recordDecl) {
		var recordsComponents = recordDecl.getRecordComponents();

		if (recordsComponents.isEmpty()) return "new %s()".formatted(recordDecl.getSimpleName());

		var params = getValuesForRecordComponents(recordsComponents);
		return "new %s(%s)".formatted(recordDecl.getSimpleName(), params);
	}

	private static String implementNecessaryMethods(TypeDecl typeDecl) {
		return typeDecl
				.getAllMethodsToImplement()
				.map(ClientWriter::overrideMethod)
				.collect(Collectors.joining("\n\n"));
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

	private static List<String> getExceptionsForExecutableInvocation(ExecutableDecl executableDecl) {
		return executableDecl.getThrownCheckedExceptions().stream()
				.map(ITypeReference::getQualifiedName)
				.toList();
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

	private static String overrideMethod(MethodDecl methodDecl) {
		return "\t@Override\n" + implementMethod(methodDecl);
	}

	private static String implementMethod(MethodDecl methodDecl) {
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

		public List<String> constructors = new ArrayList<>();
		public List<String> methods = new ArrayList<>();

		public String generateTypeCode() {
			var constructors = String.join("\n\n", this.constructors);
			var methods = String.join("\n\n", this.methods);
			var typeBody = concatDeclarations(constructors, methods);

			if (typeBody.isBlank() && superType.isInterface()) {
				return INTERFACE_EXTENSION_TEMPLATE.formatted(typeName, superType.getSimpleName());
			}

			var template = superType.isInterface() ? INTERFACE_IMPLEMENTATION_TEMPLATE : CLASS_EXTENSION_TEMPLATE;

			return String.join("\n\t", template.formatted(typeName, superType.getSimpleName(), typeBody).split("\n"));
		}
	}
}
