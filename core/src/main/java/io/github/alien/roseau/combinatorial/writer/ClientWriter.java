package io.github.alien.roseau.combinatorial.writer;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.client.ClientTemplates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ClientWriter extends AbstractWriter {
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
		var imports = getImportsForType(classDecl);
		var name = "%sClassInheritance".formatted(classDecl.getPrettyQualifiedName());
		var constructorRequired = implementRequiredConstructor(classDecl, name);
		var methodsImplemented = implementNecessaryMethods(classDecl);
		var classBody = constructorRequired.isBlank()
				? methodsImplemented
				: "%s\n%s".formatted(constructorRequired, methodsImplemented);

		var code = ClientTemplates.CLASS_EXTENSION_TEMPLATE.formatted(name, classDecl.getSimpleName(), classBody);

		addInnerTypeToClientMain(imports, code);
		addInstructionToClientMain(imports, "new %s().new %s();".formatted(Constants.CLIENT_FILENAME, name));
	}

	public void writeConstructorInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var imports = getImportsForType(containingClass);
		var paramTypes = formatParamTypeNames(constructorDecl.getParameters());
		var name = "%s%sConstructorInvocation".formatted(constructorDecl.getPrettyQualifiedName(), paramTypes);
		var params = getParamsForExecutableInvocation(constructorDecl);
		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);

		if (constructorDecl.isPublic()) {
			var code = "new %s(%s);".formatted(containingClass.getSimpleName(), params);

			addInstructionToClientMain(imports, exceptions, code);
		} else if (constructorDecl.isProtected()) {
			var formattedExceptions = formatExceptionNames(exceptions);

			var constructorSuper = "\t%s()%s {\n\t\tsuper(%s);\n\t}".formatted(
					name,
					formattedExceptions.isBlank() ? "" : " throws %s".formatted(formattedExceptions),
					params
			);
			var code = ClientTemplates.CLASS_EXTENSION_TEMPLATE.formatted(name, containingClass.getSimpleName(), constructorSuper);

			addInnerTypeToClientMain(imports, code);
		}
	}

	public void writeExceptionCatch(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);

		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "try {\n\t\t\tthrow %s;\n\t\t} catch (%s e) {}".formatted(constructor, classDecl.getSimpleName());

		addInstructionToClientMain(imports, code);
	}

	public void writeExceptionThrow(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);

		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "throw %s;".formatted(constructor);

		var exceptions = new ArrayList<String>();
		if (classDecl.isCheckedException()) {
			exceptions.add(classDecl.getSimpleName());
		}

		addInstructionToClientMain(imports, exceptions, code);
	}

	public void writeExceptionThrows(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);

		this.imports.addAll(imports);
		this.exceptions.add(classDecl.getSimpleName());
	}

	public void writeEnumValueRead(EnumValueDecl enumValueDecl, EnumDecl containingEnum) {
		var imports = getImportsForType(containingEnum);

		var caller = getContainingTypeAccessForTypeMember(containingEnum, enumValueDecl);
		var enumValueReadCode = "var %sVal = %s.%s;".formatted(enumValueDecl.getPrettyQualifiedName(), caller, enumValueDecl.getSimpleName());

		addInstructionToClientMain(imports, enumValueReadCode);
	}

	public void writeFieldRead(FieldDecl fieldDecl, TypeDecl containingType) {
		var imports = getImportsForType(containingType);
		var name = "%sFieldRead".formatted(fieldDecl.getPrettyQualifiedName());

		String methodName = null, template = null;
		if (containingType.isClass() && containingType.isAbstract()) {
			methodName = "aNewMethodToReadFieldInAbstractClass";
			template = ClientTemplates.ABSTRACT_CLASS_EXTENSION_TEMPLATE;
		} else if (fieldDecl.isPublic()) {
			var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
			var fieldReadCode = "var %sVal = %s.%s;".formatted(fieldDecl.getPrettyQualifiedName(), caller, fieldDecl.getSimpleName());

			addInstructionToClientMain(imports, fieldReadCode);
		} else if (fieldDecl.isProtected()) {
			methodName = "aNewMethodToReadProtectedField";
			template = ClientTemplates.CLASS_EXTENSION_TEMPLATE;
		}

		if (methodName == null || containingType.isSealed()) return;

		var caller = fieldDecl.isStatic() ? containingType.getSimpleName() : "this";
		var methodCode = "var val = %s.%s;".formatted(caller, fieldDecl.getSimpleName());
		var method = generateMethodDeclaration(methodName, methodCode);
		var code = template.formatted(name, containingType.getSimpleName(), method);

		addInnerTypeToClientMain(imports, code);
	}

	public void writeFieldWrite(FieldDecl fieldDecl, TypeDecl containingType) {
		var imports = getImportsForType(containingType);
		var name = "%sFieldWrite".formatted(fieldDecl.getPrettyQualifiedName());
		var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());

		String methodName = null, template = null;
		if (containingType.isClass() && containingType.isAbstract()) {
			methodName = "aNewMethodToWriteFieldInAbstractClass";
			template = ClientTemplates.ABSTRACT_CLASS_EXTENSION_TEMPLATE;
		} else if (fieldDecl.isPublic()) {
			var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
			var fieldWriteCode = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

			addInstructionToClientMain(imports, fieldWriteCode);
		} else if (fieldDecl.isProtected()) {
			methodName = "aNewMethodToWriteProtectedField";
			template = ClientTemplates.CLASS_EXTENSION_TEMPLATE;
		}

		if (methodName == null || containingType.isSealed()) return;

		var caller = fieldDecl.isStatic() ? containingType.getSimpleName() : "this";
		var methodCode = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);
		var method = generateMethodDeclaration(methodName, methodCode);
		var code = template.formatted(name, containingType.getSimpleName(), method);

		addInnerTypeToClientMain(imports, code);
	}

	public void writeRecordComponentRead(RecordComponentDecl recordComponentDecl, RecordDecl containingRecord) {
		var imports = getImportsForType(containingRecord);

		var caller = getContainingTypeAccessForTypeMember(containingRecord, recordComponentDecl);
		var recordComponentReadCode = "var %sVal = %s.%s();".formatted(recordComponentDecl.getPrettyQualifiedName(), caller, recordComponentDecl.getSimpleName());

		addInstructionToClientMain(imports, recordComponentReadCode);
	}

	public void writeInterfaceExtension(InterfaceDecl interfaceDecl) {
		var imports = getImportsForType(interfaceDecl);
		var name = "%sInterfaceExtension".formatted(interfaceDecl.getPrettyQualifiedName());

		var code = ClientTemplates.INTERFACE_EXTENSION_TEMPLATE.formatted(name, interfaceDecl.getSimpleName());

		addInnerTypeToClientMain(imports, code);
	}

	public void writeInterfaceImplementation(InterfaceDecl interfaceDecl) {
		var imports = getImportsForType(interfaceDecl);
		var name = "%sInterfaceImplementation".formatted(interfaceDecl.getPrettyQualifiedName());
		var methodsImplemented = implementNecessaryMethods(interfaceDecl);

		var code = ClientTemplates.INTERFACE_IMPLEMENTATION_TEMPLATE.formatted(name, interfaceDecl.getSimpleName(), methodsImplemented);

		addInnerTypeToClientMain(imports, code);
	}

	public void writeMethodInvocation(MethodDecl methodDecl, ClassDecl containingClass) {
		var imports = getImportsForType(containingClass);
		var paramTypes = formatParamTypeNames(methodDecl.getParameters());
		var name = "%s%sMethodInvocation".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);
		var params = getParamsForExecutableInvocation(methodDecl);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);

		String methodName = null, template = null;
		if (methodDecl.isAbstract() || containingClass.isAbstract()) {
			methodName = "aNewMethodToInvokeMethodInAbstractClass";
			template = ClientTemplates.ABSTRACT_CLASS_EXTENSION_TEMPLATE;
		} else if (methodDecl.isPublic()) {
			var caller = getContainingTypeAccessForTypeMember(containingClass, methodDecl);
			var methodInvocationCode = "%s.%s(%s);".formatted(caller, methodDecl.getSimpleName(), params);

			addInstructionToClientMain(imports, exceptions, methodInvocationCode);
		} else if (methodDecl.isProtected()) {
			methodName = "aNewMethodToInvokeProtectedMethod";
			template = ClientTemplates.CLASS_EXTENSION_TEMPLATE;
		}

		if (methodName == null || containingClass.isSealed()) return;

		var caller = methodDecl.isStatic() ? containingClass.getSimpleName() : "this";
		var methodCode = "%s.%s(%s);".formatted(caller, methodDecl.getSimpleName(), params);
		var method = generateMethodDeclaration(methodName, methodCode, exceptions);
		var code = template.formatted(name, containingClass.getSimpleName(), method);

		addInnerTypeToClientMain(imports, code);
	}

	public void writeMethodOverride(MethodDecl methodDecl, ClassDecl containingClass) {
		var imports = getImportsForType(containingClass);
		var paramTypes = formatParamTypeNames(methodDecl.getParameters());
		var name = "%s%sMethodOverride".formatted(methodDecl.getPrettyQualifiedName(), paramTypes);

		var constructorRequired = implementRequiredConstructor(containingClass, name);
		var methodImplemented = methodDecl.isStatic()
				? implementMethod(methodDecl)
				: overrideMethod(methodDecl);
		var classBody = constructorRequired.isBlank()
				? methodImplemented
				: "%s\n%s".formatted(constructorRequired, methodImplemented);

		var code = methodDecl.isAbstract() || containingClass.isAbstract()
				? ClientTemplates.ABSTRACT_CLASS_EXTENSION_TEMPLATE.formatted(name, containingClass.getSimpleName(), classBody)
				: ClientTemplates.CLASS_EXTENSION_TEMPLATE.formatted(name, containingClass.getSimpleName(), classBody);

		addInnerTypeToClientMain(imports, code);
	}

	public void writeTypeReference(TypeDecl typeDecl) {
		var imports = getImportsForType(typeDecl);

		var code = "%s %sRef;".formatted(typeDecl.getSimpleName(), typeDecl.getPrettyQualifiedName());

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

			var fullCode = ClientTemplates.FULL_CLIENT_FILE_TEMPLATE.formatted(
					clientPackageName,
					sortedImports,
					Constants.CLIENT_FILENAME,
					innerTypesCode,
					exceptionsCode,
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

	private static String getContainingTypeAccessForTypeMember(TypeDecl typeDecl, TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.isStatic()) return typeDecl.getSimpleName();
		else if (typeDecl instanceof InterfaceDecl) return typeDecl.getSimpleName();
		else if (typeDecl instanceof EnumDecl enumDecl) return generateAccessToFirstEnumValue(enumDecl);
		else if (typeDecl instanceof RecordDecl recordDecl) return generateConstructorInvocationForRecord(recordDecl);
		else if (typeDecl instanceof ClassDecl classDecl) return generateEasiestConstructorInvocationForClass(classDecl);

		throw new IllegalArgumentException("Type member must be static, or type must be enum or class");
	}

	private static String implementRequiredConstructor(ClassDecl classDecl, String className) {
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
		var methods = typeDecl.getAllMethodsToImplement();
		return methods.map(ClientWriter::overrideMethod).collect(Collectors.joining("\n\n"));
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

	private static String overrideMethod(MethodDecl methodDecl) {
		return "\t@Override\n" + implementMethod(methodDecl);
	}

	private static String implementMethod(MethodDecl methodDecl) {
		var methodReturnTypeName = methodDecl.getType().getQualifiedName();
		var methodSignature = methodDecl.toString().replace("abstract ", "");

		if (methodDecl.isNative()) {
			return "\t" + methodSignature + ";";
		}

		return methodReturnTypeName.equals("void")
				? "\t" + methodSignature + " {}"
				: "\t%s { return %s; }".formatted(methodSignature, getDefaultValueForType(methodReturnTypeName));
	}

	private static String generateMethodDeclaration(String functionName, String functionCode) {
		return "\tpublic void %s() {\n\t\t%s\n\t}".formatted(functionName, functionCode);
	}

	private static String generateMethodDeclaration(String functionName, String functionCode, List<String> exceptions) {
		var exceptionsFormatted = formatExceptionNames(exceptions);

		return exceptionsFormatted.isBlank()
				? generateMethodDeclaration(functionName, functionCode)
				: "\tpublic void %s() throws %s {\n\t\t%s\n\t}".formatted(functionName, exceptionsFormatted, functionCode);
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
