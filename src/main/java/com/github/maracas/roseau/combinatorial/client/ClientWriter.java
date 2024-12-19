package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClientWriter {
    private final Path outputDir;

    public ClientWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public boolean createOutputDir() {
        try {
            File outputDirFile = outputDir.toFile();
            if (outputDirFile.exists()) {
                try {
                    FileUtils.cleanDirectory(outputDirFile);
                    return true;
                } catch (IOException e) {
                    System.err.println("Error cleaning output directory: " + e.getMessage());
                    return false;
                }
            }

            return outputDirFile.mkdirs();
        } catch (SecurityException e) {
            System.err.println("Error creating output directory: " + e.getMessage());
            return false;
        }
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

        if (constructorDecl.isPublic()) {
            var code = "new %s(%s);".formatted(containingClass.getSimpleName(), params);

            writeCodeInMain(imports, name, code);
        } else if (constructorDecl.isProtected()) {
            var constructorSuper = "\t%s() {\n\t\tsuper(%s);\n\t}".formatted(name, params);
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

        if (methodName == null) return;

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

        if (methodName == null) return;

        var methodCode = "this.%s = %s;".formatted(fieldDecl.getSimpleName(), value);
        var method = generateMethodDeclaration(methodName, methodCode);
        var code = template.formatted(imports, name, containingType.getSimpleName(), method);

        writeCodeInFile(name, code);
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
        var exceptions = methodDecl.getThrownCheckedExceptions().stream()
                .map(TypeReference::getQualifiedName)
                .collect(Collectors.joining(", "));

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

        if (methodName == null) return;

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

    private String getImportsForType(TypeDecl typeDecl) {
        return "import %s;".formatted(typeDecl.getQualifiedName());
    }

    private String getContainingTypeAccessForTypeMember(TypeDecl typeDecl, TypeMemberDecl typeMemberDecl) {
        return typeDecl.isClass() && !typeMemberDecl.isStatic()
                ? generateEasiestConstructorInvocationForClass((ClassDecl) typeDecl)
                : typeDecl.getSimpleName();
    }

    private String getParamsForExecutableInvocation(ExecutableDecl executableDecl) {
        return executableDecl.getParameters().stream()
                .map(p -> getDefaultValueForType(p.type().getQualifiedName()))
                .collect(Collectors.joining(", "));
    }

    private String implementRequiredConstructor(ClassDecl classDecl, String className) {
        var constructors = getSortedConstructors(classDecl);

        if (constructors.isEmpty()) return "";

        var params = getParamsForExecutableInvocation(constructors.getFirst());
        return params.isBlank()
                ? ""
                : "\t%s() {\n\t\tsuper(%s);\n\t}\n".formatted(className, params);
    }

    private String generateEasiestConstructorInvocationForClass(ClassDecl classDecl) {
        var sortedConstructors = getSortedConstructors(classDecl);

        if (sortedConstructors.isEmpty()) return "new %s()".formatted(classDecl.getSimpleName());

        var params = getParamsForExecutableInvocation(sortedConstructors.getFirst());
        return "new %s(%s)".formatted(classDecl.getSimpleName(), params);
    }

    private String implementNecessaryMethods(TypeDecl typeDecl) {
        var methods = typeDecl.getAllMethodsToImplement();
        return methods.map(this::overrideMethod).collect(Collectors.joining("\n\n"));
    }

    private String overrideMethod(MethodDecl methodDecl) {
        return "\t@Override\n" + implementMethod(methodDecl);
    }

    private String implementMethod(MethodDecl methodDecl) {
        var methodReturnTypeName = methodDecl.getType().getQualifiedName();
        var methodSignature = methodDecl.toString().replace("abstract ", "");

        if (methodDecl.isNative()) {
            return "\t" + methodSignature + ";";
        }

        return methodReturnTypeName.equals("void")
                ? "\t" + methodSignature + " {}"
                : "\t%s { return %s; }".formatted(methodSignature, getDefaultValueForType(methodReturnTypeName));
    }

    private String generateMethodDeclaration(String functionName, String functionCode) {
        return "\tpublic void %s() {\n\t\t%s\n\t}".formatted(functionName, functionCode);
    }

    private String generateMethodDeclaration(String functionName, String functionCode, String exceptions) {
        return exceptions.isBlank()
                ? generateMethodDeclaration(functionName, functionCode)
                : "\tpublic void %s() throws %s {\n\t\t%s\n\t}".formatted(functionName, exceptions, functionCode);
    }

    private String getDefaultValueForType(String typeName) {
        if (typeName.contains("String") && !typeName.contains("[]")) return "\"\"";

        return switch (typeName) {
            case "int", "long", "float", "double", "byte", "short" -> "0";
            case "char" -> "'c'";
            case "boolean" -> "false";
            default -> "null";
        };
    }

    private List<ConstructorDecl> getSortedConstructors(ClassDecl classDecl) {
        return classDecl.getConstructors().stream()
                .sorted(Comparator.comparingInt(c -> c.getParameters().size()))
                .toList();
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
            var fullCode = ClientTemplates.FILE_TEMPLATE.formatted(code).getBytes();

            Files.write(Paths.get(outputDir.toString(), fileName + ".java"), fullCode);
        } catch (IOException e) {
            System.err.println("Error writing client code to file: " + e.getMessage());
        }
    }
}
