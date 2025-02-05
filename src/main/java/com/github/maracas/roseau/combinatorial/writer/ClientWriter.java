package com.github.maracas.roseau.combinatorial.writer;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.client.ClientTemplates;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClientWriter extends AbstractWriter {
	private static final String clientsPackageName = Constants.CLIENTS_FOLDER;

	public ClientWriter(Path outputDir) {
		super(outputDir);
	}

	public void writeClassInheritance(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);
		var name = "%sClassInheritance".formatted(classDecl.getPrettyQualifiedName());
		var constructorRequired = implementRequiredConstructor(classDecl, name);
		var methodsImplemented = implementNecessaryMethods(classDecl);
		var classBody = constructorRequired + "\n" + methodsImplemented;

		var code = ClientTemplates.CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, classDecl.getSimpleName(), classBody);

		writeCodeInFile(name, code);
	}

	public void writeConstructorInvocation(ConstructorDecl constructorDecl, ClassDecl containingClass) {
		var imports = getImportsForType(containingClass);
		var name = "%sConstructorInvocation".formatted(constructorDecl.getPrettyQualifiedName());
		var params = getParamsForExecutableInvocation(constructorDecl);
		var exceptions = getExceptionsForExecutableInvocation(constructorDecl);

		if (constructorDecl.isPublic()) {
			var code = "new %s(%s);".formatted(containingClass.getSimpleName(), params);

			writeCodeInMain(imports, name, code, exceptions);
		} else if (constructorDecl.isProtected()) {
			var constructorSuper = "\t%s()%s {\n\t\tsuper(%s);\n\t}".formatted(
					name,
					exceptions.isBlank() ? "" : " throws %s".formatted(exceptions),
					params
			);
			var code = ClientTemplates.CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingClass.getSimpleName(), constructorSuper);

			writeCodeInFile(name, code);
		}
	}

	public void writeExceptionCatch(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);
		var name = "%sExceptionCatch".formatted(classDecl.getPrettyQualifiedName());

		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "try {\n\t\t\tthrow %s;\n\t\t} catch (%s e) {}".formatted(constructor, classDecl.getSimpleName());

		writeCodeInMain(imports, name, code);
	}

	public void writeExceptionThrow(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);
		var name = "%sExceptionThrow".formatted(classDecl.getPrettyQualifiedName());

		var constructor = generateEasiestConstructorInvocationForClass(classDecl);
		var code = "throw %s;".formatted(constructor);

		if (classDecl.isCheckedException()) {
			writeCodeInMain(imports, name, code, classDecl.getSimpleName());
		} else if (classDecl.isUncheckedException()) {
			writeCodeInMain(imports, name, code);
		}
	}

	public void writeExceptionThrows(ClassDecl classDecl) {
		var imports = getImportsForType(classDecl);
		var name = "%sExceptionThrows".formatted(classDecl.getPrettyQualifiedName());

		writeCodeInMain(imports, name, "", classDecl.getSimpleName());
	}

	public void writeFieldRead(FieldDecl fieldDecl, TypeDecl containingType) {
		var imports = getImportsForType(containingType);
		var name = "%sFieldRead".formatted(fieldDecl.getPrettyQualifiedName());

		String methodName = null, template = null;
		if (containingType.isClass() && containingType.isAbstract()) {
			methodName = "aNewMethodToReadFieldInAbstractClass";
			template = ClientTemplates.ABSTRACT_CLASS_INHERITANCE_TEMPLATE;
		} else if (fieldDecl.isPublic()) {
			var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
			var fieldReadCode = "var val = %s.%s;".formatted(caller, fieldDecl.getSimpleName());

			writeCodeInMain(imports, name, fieldReadCode);
		} else if (fieldDecl.isProtected()) {
			methodName = "aNewMethodToReadProtectedField";
			template = ClientTemplates.CLASS_INHERITANCE_TEMPLATE;
		}

		if (methodName == null || containingType.isSealed()) return;

		var methodCode = "var val = this.%s;".formatted(fieldDecl.getSimpleName());
		var method = generateMethodDeclaration(methodName, methodCode);
		var code = template.formatted(imports, name, containingType.getSimpleName(), method);

		writeCodeInFile(name, code);
	}

	public void writeFieldWrite(FieldDecl fieldDecl, TypeDecl containingType) {
		var imports = getImportsForType(containingType);
		var name = "%sFieldWrite".formatted(fieldDecl.getPrettyQualifiedName());
		var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());

		String methodName = null, template = null;
		if (containingType.isClass() && containingType.isAbstract()) {
			methodName = "aNewMethodToWriteFieldInAbstractClass";
			template = ClientTemplates.ABSTRACT_CLASS_INHERITANCE_TEMPLATE;
		} else if (fieldDecl.isPublic()) {
			var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
			var fieldWriteCode = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

			writeCodeInMain(imports, name, fieldWriteCode);
		} else if (fieldDecl.isProtected()) {
			methodName = "aNewMethodToWriteProtectedField";
			template = ClientTemplates.CLASS_INHERITANCE_TEMPLATE;
		}

		if (methodName == null || containingType.isSealed()) return;

		var methodCode = "this.%s = %s;".formatted(fieldDecl.getSimpleName(), value);
		var method = generateMethodDeclaration(methodName, methodCode);
		var code = template.formatted(imports, name, containingType.getSimpleName(), method);

		writeCodeInFile(name, code);
	}

	public void writeRecordComponentRead(RecordComponentDecl recordComponentDecl, RecordDecl containingRecord) {
		var imports = getImportsForType(containingRecord);
		var name = "%sRecordComponentRead".formatted(recordComponentDecl.getPrettyQualifiedName());

		var caller = getContainingTypeAccessForTypeMember(containingRecord, recordComponentDecl);
		var recordComponentReadCode = "var val = %s.%s();".formatted(caller, recordComponentDecl.getSimpleName());

		writeCodeInMain(imports, name, recordComponentReadCode);
	}

	public void writeInterfaceExtension(InterfaceDecl interfaceDecl) {
		var imports = getImportsForType(interfaceDecl);
		var name = "%sInterfaceExtension".formatted(interfaceDecl.getPrettyQualifiedName());

		var code = ClientTemplates.INTERFACE_EXTENSION_TEMPLATE.formatted(imports, name, interfaceDecl.getSimpleName());

		writeCodeInFile(name, code);
	}

	public void writeInterfaceImplementation(InterfaceDecl interfaceDecl) {
		var imports = getImportsForType(interfaceDecl);
		var name = "%sInterfaceImplementation".formatted(interfaceDecl.getPrettyQualifiedName());
		var methodsImplemented = implementNecessaryMethods(interfaceDecl);

		var code = ClientTemplates.INTERFACE_IMPLEMENTATION_TEMPLATE.formatted(imports, name, interfaceDecl.getSimpleName(), methodsImplemented);

		writeCodeInFile(name, code);
	}

	public void writeMethodInvocation(MethodDecl methodDecl, ClassDecl containingClass) {
		var imports = getImportsForType(containingClass);
		var name = "%sMethodInvocation".formatted(methodDecl.getPrettyQualifiedName());
		var caller = getContainingTypeAccessForTypeMember(containingClass, methodDecl);
		var params = getParamsForExecutableInvocation(methodDecl);
		var exceptions = getExceptionsForExecutableInvocation(methodDecl);

		String methodName = null, template = null;
		if (methodDecl.isAbstract() || containingClass.isAbstract()) {
			methodName = "aNewMethodToInvokeMethodInAbstractClass";
			template = ClientTemplates.ABSTRACT_CLASS_INHERITANCE_TEMPLATE;
		} else if (methodDecl.isPublic()) {
			var methodInvocationCode = "%s.%s(%s);".formatted(caller, methodDecl.getSimpleName(), params);

			writeCodeInMain(imports, name, methodInvocationCode, exceptions);
		} else if (methodDecl.isProtected()) {
			methodName = "aNewMethodToInvokeProtectedMethod";
			template = ClientTemplates.CLASS_INHERITANCE_TEMPLATE;
		}

		if (methodName == null || containingClass.isSealed()) return;

		var methodCode = "this.%s(%s);".formatted(methodDecl.getSimpleName(), params);
		var method = generateMethodDeclaration(methodName, methodCode, exceptions);
		var code = template.formatted(imports, name, containingClass.getSimpleName(), method);

		writeCodeInFile(name, code);
	}

	public void writeMethodOverride(MethodDecl methodDecl, ClassDecl containingClass) {
		var imports = getImportsForType(containingClass);
		var name = "%sMethodOverride".formatted(methodDecl.getPrettyQualifiedName());

		var constructorRequired = implementRequiredConstructor(containingClass, name);
		var methodImplemented = methodDecl.isStatic()
				? implementMethod(methodDecl)
				: overrideMethod(methodDecl);
		var classBody = constructorRequired + "\n" + methodImplemented;

		var code = methodDecl.isAbstract() || containingClass.isAbstract()
				? ClientTemplates.ABSTRACT_CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingClass.getSimpleName(), classBody)
				: ClientTemplates.CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingClass.getSimpleName(), classBody);

		writeCodeInFile(name, code);
	}

	public void writeTypeReference(TypeDecl typeDecl) {
		var imports = getImportsForType(typeDecl);
		var name = "%sTypeReference".formatted(typeDecl.getPrettyQualifiedName());

		var code = "%s ref;".formatted(typeDecl.getSimpleName());

		writeCodeInMain(imports, name, code);
	}

	private void writeCodeInMain(String imports, String clientName, String code) {
		var mainClassCode = ClientTemplates.MAIN_CLASS_TEMPLATE.formatted(imports, clientName, code);

		writeCodeInFile(clientName, mainClassCode);
	}

	private void writeCodeInMain(String imports, String clientName, String code, String exceptions) {
		if (exceptions.isBlank()) {
			writeCodeInMain(imports, clientName, code);
			return;
		}

		var mainClassCode = ClientTemplates.MAIN_THROWING_CLASS_TEMPLATE.formatted(imports, clientName, exceptions, code);

		writeCodeInFile(clientName, mainClassCode);
	}

	private void writeCodeInFile(String fileName, String code) {
		try {
			var fullCode = ClientTemplates.FILE_TEMPLATE.formatted(clientsPackageName, code).getBytes();

			var packagePath = clientsPackageName.replace(".", "/");
			var filePath = outputDir.resolve("%s/%s.java".formatted(packagePath, fileName));
			var file = filePath.toFile();
			file.getParentFile().mkdirs();

			Files.write(filePath, fullCode);
		} catch (IOException e) {
			System.err.println("Error writing client code to file: " + e.getMessage());
		}
	}

	private static String getImportsForType(TypeDecl typeDecl) {
		return "import %s;".formatted(typeDecl.getQualifiedName());
	}

	private static String getContainingTypeAccessForTypeMember(TypeDecl typeDecl, TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.isStatic()) return typeDecl.getSimpleName();
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

		return params.isBlank() && exceptions.isBlank()
				? ""
				: "\t%s()%s {\n\t\tsuper(%s);\n\t}\n".formatted(
				className,
				exceptions.isBlank() ? "" : " throws %s".formatted(exceptions),
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
				.map(p -> getDefaultValueForType(p.type().getQualifiedName()))
				.collect(Collectors.joining(", "));
	}

	private static String getValuesForRecordComponents(List<RecordComponentDecl> recordComponents) {
		return recordComponents.stream()
				.map(rC -> getDefaultValueForType(rC.getType().getQualifiedName()))
				.collect(Collectors.joining(", "));
	}

	private static String getExceptionsForExecutableInvocation(ExecutableDecl executableDecl) {
		return executableDecl.getThrownCheckedExceptions().stream()
				.map(TypeReference::getQualifiedName)
				.collect(Collectors.joining(", "));
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

	private static String generateMethodDeclaration(String functionName, String functionCode, String exceptions) {
		return exceptions.isBlank()
				? generateMethodDeclaration(functionName, functionCode)
				: "\tpublic void %s() throws %s {\n\t\t%s\n\t}".formatted(functionName, exceptions, functionCode);
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
		return classDecl.getConstructors().stream()
				.sorted(Comparator.comparingInt(c -> c.getParameters().size()))
				.toList();
	}
}
