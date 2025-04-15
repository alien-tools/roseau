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
	private final List<String> innerTypes = new ArrayList<>();
	private final Set<String> exceptions = new HashSet<>();
	private final List<String> notThrowingInstructions = new ArrayList<>();
	private final List<String> throwingInstructions = new ArrayList<>();

	public ClientWriter(Path outputDir) {
		super(outputDir);
	}

	public void writeClassInheritance(ClassDecl classDecl) {
		var name = "%sClassInheritance".formatted(classDecl.getPrettyQualifiedName());

		var constructorRequired = implementRequiredConstructor(classDecl, name);
		var necessaryMethods = implementNecessaryMethods(classDecl);
		var classBody = concatDeclarations(constructorRequired, necessaryMethods);
		var code = CLASS_EXTENSION_TEMPLATE.formatted(name, classDecl.getSimpleName(), classBody);

		var imports = getImportsForType(classDecl);
		addInnerTypeToClientMain(imports, code);
		addInstructionToClientMain(imports, "new %s().new %s();".formatted(Constants.CLIENT_FILENAME, name));
	}

	public void writeConstructorDirectInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var params = getParamsForExecutableInvocation(constructorDecl);
		var code = "new %s(%s);".formatted(containingClass.getSimpleName(), params);

		var imports = getImportsForType(containingClass);
		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);
		addInstructionToClientMain(imports, exceptions, code);
	}

	public void writeConstructorInheritanceInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var paramTypes = formatParamTypeNames(constructorDecl.getParameters());
		var className = "%s%sConstructorInvocation".formatted(constructorDecl.getPrettyQualifiedName(), paramTypes);

		var params = getParamsForExecutableInvocation(constructorDecl);
		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);
		var formattedExceptions = formatExceptionNames(exceptions);

		var constructor = "\t%s()%s {\n\t\tsuper(%s);\n\t}".formatted(
				className,
				formattedExceptions.isBlank() ? "" : " throws %s".formatted(formattedExceptions),
				params
		);
		var necessaryMethods = implementNecessaryMethods(containingClass);
		var classBody = concatDeclarations(constructor, necessaryMethods);

		var classCode = CLASS_EXTENSION_TEMPLATE.formatted(className, containingClass.getSimpleName(), classBody);

		var imports = getImportsForType(containingClass);
		addInnerTypeToClientMain(imports, classCode);
		addInstructionToClientMain(imports, exceptions, "new %s().new %s();".formatted(Constants.CLIENT_FILENAME, className));
	}

	public void writeExceptionCatch(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "try {\n\t\t\tthrow %s;\n\t\t} catch (%s e) {}".formatted(constructor, classDecl.getSimpleName());

		var imports = getImportsForType(classDecl);
		addInstructionToClientMain(imports, code);
	}

	public void writeExceptionThrow(ClassDecl classDecl) {
		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "throw %s;".formatted(constructor);

		var exceptions = new ArrayList<String>();
		if (classDecl.isCheckedException()) {
			exceptions.add(classDecl.getSimpleName());
		}

		var imports = getImportsForType(classDecl);
		addInstructionToClientMain(imports, exceptions, code);
	}

	public void writeExceptionThrows(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);

		this.imports.addAll(imports);
		this.exceptions.add(classDecl.getSimpleName());
	}

	public void writeEnumValueRead(EnumValueDecl enumValueDecl, EnumDecl containingEnum) {
		var caller = getContainingTypeAccessForTypeMember(containingEnum, enumValueDecl);
		var enumValueReadCode = "%s %sVal = %s.%s;".formatted(containingEnum.getSimpleName(), enumValueDecl.getPrettyQualifiedName(), caller, enumValueDecl.getSimpleName());

		var imports = getImportsForType(containingEnum);
		addInstructionToClientMain(imports, enumValueReadCode);
	}

	public void writeFieldDirectRead(FieldDecl fieldDecl, TypeDecl containingType) {
		var type = fieldDecl.getType().getQualifiedName();
		var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
		var fieldReadCode = "%s %sVal = %s.%s;".formatted(type, fieldDecl.getPrettyQualifiedName(), caller, fieldDecl.getSimpleName());

		var imports = getImportsForType(containingType);
		addInstructionToClientMain(imports, fieldReadCode);
	}

	public void writeFieldInheritanceRead(FieldDecl fieldDecl, TypeDecl containingType) {
		var className = "%sFieldRead".formatted(fieldDecl.getPrettyQualifiedName());

		var type = fieldDecl.getType().getQualifiedName();
		var caller = fieldDecl.isStatic() ? containingType.getSimpleName() : "this";
		var readMethodBody = "%s val = %s.%s;".formatted(type, caller, fieldDecl.getSimpleName());

		generateAndInvokeNewMethodForType(className, "read", readMethodBody, containingType);

		if (!fieldDecl.isPublic()) return;

		var imports = getImportsForType(containingType);
		var fieldReadCode = "%s %sInhVal = new %s().%s;".formatted(type, fieldDecl.getPrettyQualifiedName(), className, fieldDecl.getSimpleName());
		addInstructionToClientMain(imports, fieldReadCode);
	}

	public void writeFieldDirectWrite(FieldDecl fieldDecl, TypeDecl containingType) {
		var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
		var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());
		var fieldWriteCode = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

		var imports = getImportsForType(containingType);
		addInstructionToClientMain(imports, fieldWriteCode);
	}

	public void writeFieldInheritanceWrite(FieldDecl fieldDecl, TypeDecl containingType) {
		var className = "%sFieldWrite".formatted(fieldDecl.getPrettyQualifiedName());

		var caller = fieldDecl.isStatic() ? containingType.getSimpleName() : "this";
		var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());
		var writeMethodBody = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

		generateAndInvokeNewMethodForType(className, "write", writeMethodBody, containingType);

		if (!fieldDecl.isPublic()) return;

		var imports = getImportsForType(containingType);
		var fieldWriteCode = "new %s().%s = %s;".formatted(className, fieldDecl.getSimpleName(), value);
		addInstructionToClientMain(imports, fieldWriteCode);
	}

	public void writeRecordComponentRead(RecordComponentDecl recordComponentDecl, RecordDecl containingRecord) {
		var type = recordComponentDecl.isVarargs() ? "%s[]".formatted(recordComponentDecl.getType().getQualifiedName()) : recordComponentDecl.getType().getQualifiedName();
		var caller = getContainingTypeAccessForTypeMember(containingRecord, recordComponentDecl);
		var recordComponentReadCode = "%s %sVal = %s.%s();".formatted(type, recordComponentDecl.getPrettyQualifiedName(), caller, recordComponentDecl.getSimpleName());

		var imports = getImportsForType(containingRecord);
		addInstructionToClientMain(imports, recordComponentReadCode);
	}

	public void writeInterfaceExtension(InterfaceDecl interfaceDecl) {
		var interfaceName = "%sInterfaceExtension".formatted(interfaceDecl.getPrettyQualifiedName());

		var code = INTERFACE_EXTENSION_TEMPLATE.formatted(interfaceName, interfaceDecl.getSimpleName());

		var imports = getImportsForType(interfaceDecl);
		addInnerTypeToClientMain(imports, code);
	}

	public void writeInterfaceImplementation(InterfaceDecl interfaceDecl) {
		var className = "%sInterfaceImplementation".formatted(interfaceDecl.getPrettyQualifiedName());
		var methodsImplemented = implementNecessaryMethods(interfaceDecl);

		var code = INTERFACE_IMPLEMENTATION_TEMPLATE.formatted(className, interfaceDecl.getSimpleName(), methodsImplemented);

		var imports = getImportsForType(interfaceDecl);
		addInnerTypeToClientMain(imports, code);
		addInstructionToClientMain(imports, "new %s().new %s();".formatted(Constants.CLIENT_FILENAME, className));
	}

	public void writeMethodDirectInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var caller = getContainingTypeAccessForTypeMember(containingType, methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);
		var methodReturn = getReturnHandleForMethod(methodDecl, "Dir");
		var methodInvocationCode = "%s%s.%s(%s);".formatted(methodReturn, caller, methodDecl.getSimpleName(), params);

		var imports = getImportsForType(containingType);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		addInstructionToClientMain(imports, exceptions, methodInvocationCode);
	}

	public void writeMethodInheritanceInvocation(MethodDecl methodDecl, TypeDecl containingType) {
		var paramTypes = formatParamTypeNames(methodDecl.getParameters());
		var className = "%s%sMethodInvocation".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);

		var caller = methodDecl.isStatic() ? containingType.getSimpleName() : "this";
		var params = getParamsForExecutableInvocation(methodDecl);
		var methodInvokeReturn = getReturnHandleForMethod(methodDecl, "InhInv");
		var invokeMethodBody = "%s%s.%s(%s);".formatted(methodInvokeReturn, caller, methodDecl.getSimpleName(), params);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);

		generateAndInvokeNewMethodForType(className, "invoke", invokeMethodBody, exceptions, containingType);

		if (!methodDecl.isPublic() || (containingType.isInterface() && methodDecl.isStatic())) return;

		var imports = getImportsForType(containingType);
		var methodDirectReturn = getReturnHandleForMethod(methodDecl, "InhDir");
		var methodInvocationCode = "%snew %s().%s(%s);".formatted(methodDirectReturn, className, methodDecl.getSimpleName(), params);
		addInstructionToClientMain(imports, exceptions, methodInvocationCode);
	}

	public void writeMethodOverride(MethodDecl methodDecl, TypeDecl containingType) {
		var paramTypes = formatParamTypeNames(methodDecl.getParameters());
		var className = "%s%sMethodOverride".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);

		var necessaryConstructor = implementRequiredConstructor(containingType, className);
		var overrideMethod = methodDecl.isStatic() ? implementMethod(methodDecl) : overrideMethod(methodDecl);
		var necessaryMethods = implementNecessaryMethods(containingType, methodDecl);
		var classBody = concatDeclarations(necessaryConstructor, overrideMethod, necessaryMethods);

		var template = containingType.isInterface() ? INTERFACE_IMPLEMENTATION_TEMPLATE : CLASS_EXTENSION_TEMPLATE;
		var classCode = template.formatted(className, containingType.getSimpleName(), classBody);

		var imports = getImportsForType(containingType);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);
		addInnerTypeToClientMain(imports, classCode);
		addInstructionToClientMain(imports, exceptions, "new %s().new %s().%s(%s);".formatted(Constants.CLIENT_FILENAME, className, methodDecl.getSimpleName(), params));
	}

	public void writeTypeReference(TypeDecl typeDecl) {
		var code = "%s %sRef;".formatted(typeDecl.getSimpleName(), typeDecl.getPrettyQualifiedName());

		var imports = getImportsForType(typeDecl);
		addInstructionToClientMain(imports, code);
	}

	public void writeClientFile() {
		try {
			var sortedImports = imports.stream().sorted().map("import %s;"::formatted).collect(Collectors.joining("\n"));
			var innerTypesCode = String.join("\n", innerTypes);
			var sortedExceptions = formatExceptionNames(exceptions.stream().toList());
			var exceptionsCode = sortedExceptions.isBlank() ? "" : " throws %s".formatted(sortedExceptions);
			var notThrowingCode = String.join("\n\t\t", notThrowingInstructions);
			var throwingCode = String.join("\n\t\t", throwingInstructions);

			var fullCode = FULL_CLIENT_FILE_TEMPLATE.formatted(
					clientPackageName,
					sortedImports,
					Constants.CLIENT_FILENAME,
					innerTypesCode,
					exceptionsCode,
					Constants.CLIENT_FILENAME,
					Constants.CLIENT_FILENAME,
					notThrowingCode,
					exceptionsCode,
					throwingCode
			).getBytes();

			var packagePath = clientPackageName.replace(".", "/");
			var filePath = outputDir.resolve("%s/FullClient.java".formatted(packagePath));
			filePath.toFile().getParentFile().mkdirs();

			Files.write(filePath, fullCode);
		} catch (IOException e) {
			LOGGER.error("Error writing client code to file: {}", e.getMessage());
		}
	}

	private void addInnerTypeToClientMain(List<String> imports, String code) {
		this.imports.addAll(imports);
		this.innerTypes.add(Arrays.stream(code.split("\n")).map("\t%s"::formatted).collect(Collectors.joining("\n")) + "\n");
	}

	private void addInstructionToClientMain(List<String> imports, String code) {
		addInstructionToClientMain(imports, List.of(), code);
	}

	private void addInstructionToClientMain(List<String> imports, List<String> exceptions, String code) {
		this.imports.addAll(imports);
		this.exceptions.addAll(exceptions);

		if (exceptions.isEmpty()) {
			this.notThrowingInstructions.add(code);
		} else {
			this.throwingInstructions.add(code);
		}
	}

	private static List<String> getImportsForType(TypeDecl typeDecl) {
		return List.of(typeDecl.getQualifiedName());
	}

	private void generateAndInvokeNewMethodForType(String className, String methodName, String methodBody, List<String> exceptions, TypeDecl containingType) {
		var necessaryConstructor = implementRequiredConstructor(containingType, className);
		var newMethod = generateMethodDeclaration(methodName, methodBody, exceptions);
		var necessaryMethods = implementNecessaryMethods(containingType);
		var classBody = concatDeclarations(necessaryConstructor, newMethod, necessaryMethods);

		var template = containingType.isInterface() ? INTERFACE_IMPLEMENTATION_TEMPLATE : CLASS_EXTENSION_TEMPLATE;
		var classCode = template.formatted(className, containingType.getSimpleName(), classBody);

		var imports = getImportsForType(containingType);
		addInnerTypeToClientMain(imports, classCode);
		addInstructionToClientMain(imports, exceptions, "new %s().new %s().%s();".formatted(Constants.CLIENT_FILENAME, className, methodName));
	}

	private void generateAndInvokeNewMethodForType(String className, String methodName, String methodBody, TypeDecl containingType) {
		generateAndInvokeNewMethodForType(className, methodName, methodBody, List.of(), containingType);
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

	private static String implementNecessaryMethods(TypeDecl typeDecl, MethodDecl ignoreMethod) {
		return typeDecl
				.getAllMethodsToImplement()
				.filter(methodDecl -> !methodDecl.equals(ignoreMethod))
				.map(ClientWriter::overrideMethod)
				.collect(Collectors.joining("\n\n"));
	}

	private static String implementNecessaryMethods(TypeDecl typeDecl) {
		return implementNecessaryMethods(typeDecl, null);
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

	private static String formatExceptionNames(List<String> exceptions) {
		return exceptions.stream().sorted().collect(Collectors.joining(", "));
	}
}
