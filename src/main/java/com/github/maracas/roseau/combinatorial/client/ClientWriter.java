package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ClientWriter {
    private final Path outputDir;

    private static final String FILE_TEMPLATE = """
            package generated.clients;
            
            %s""";

    private static final String MAIN_CLASS_TEMPLATE = """
            %s
            
            public class %s {
                public static void main(String[] args) {
                    %s
                }
            }
            """;

    private static final String CLASS_INHERITANCE_TEMPLATE = """
            %s
            
            class %s extends %s {
            %s
            }
            """;

    private static final String ABSTRACT_CLASS_INHERITANCE_TEMPLATE = """
            %s
            
            abstract class %s extends %s {
            %s
            }
            """;

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
        var methodsImplemented = implementNecessaryMethods(classDecl);

        var code = CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, classDecl.getSimpleName(), methodsImplemented);

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
            var code = CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingClass.getSimpleName(), constructorSuper);

            writeCodeInFile(name, code);
        }
    }

    public void writeFieldRead(FieldDecl fieldDecl, TypeDecl containingType) {
        var imports = getImportsForType(containingType);
        var name = "%sFieldRead".formatted(fieldDecl.getPrettyQualifiedName());

        if (containingType.isClass() && containingType.isAbstract()) {
            var fieldReadInAbstractClass = "\tpublic void aNewMethodToReadFieldInAbstractClass() {\n\t\tvar val = this.%s;\n\t}".formatted(fieldDecl.getSimpleName());
            var code = ABSTRACT_CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingType.getSimpleName(), fieldReadInAbstractClass);

            writeCodeInFile(name, code);
        } else if (fieldDecl.isPublic()) {
            var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
            var fieldReadCode = "var val = %s.%s;".formatted(caller, fieldDecl.getSimpleName());

            writeCodeInMain(imports, name, fieldReadCode);
        } else if (fieldDecl.isProtected()) {
            var fieldReadInMethodCode = "\tpublic void aNewMethodToReadProtectedField() {\n\t\tvar val = this.%s;\n\t}".formatted(fieldDecl.getSimpleName());
            var code = CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingType.getSimpleName(), fieldReadInMethodCode);

            writeCodeInFile(name, code);
        }
    }

    public void writeFieldWrite(FieldDecl fieldDecl, TypeDecl containingType) {
        var imports = getImportsForType(containingType);
        var name = "%sFieldWrite".formatted(fieldDecl.getPrettyQualifiedName());
        var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());

        if (containingType.isClass() && containingType.isAbstract()) {
            var fieldWriteInAbstractClass = "\tpublic void aNewMethodToWriteFieldInAbstractClass() {\n\t\tthis.%s = %s;\n\t}".formatted(fieldDecl.getSimpleName(), value);
            var code = ABSTRACT_CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingType.getSimpleName(), fieldWriteInAbstractClass);

            writeCodeInFile(name, code);
        } else if (fieldDecl.isPublic()) {
            var caller = getContainingTypeAccessForTypeMember(containingType, fieldDecl);
            var fieldWriteCode = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

            writeCodeInMain(imports, name, fieldWriteCode);
        } else if (fieldDecl.isProtected()) {
            var fieldWriteInMethodCode = "\tpublic void aNewMethodToWriteProtectedField() {\n\t\tthis.%s = %s;\n\t}".formatted(fieldDecl.getSimpleName(), value);
            var code = CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, containingType.getSimpleName(), fieldWriteInMethodCode);

            writeCodeInFile(name, code);
        }
    }

    public void writeInterfaceExtension(InterfaceDecl interfaceDecl) {
        var imports = getImportsForType(interfaceDecl);
        var name = "%sInterfaceExtension".formatted(interfaceDecl.getPrettyQualifiedName());

        var code = """
                %s
                
                interface %s extends %s {}
                """.formatted(imports, name, interfaceDecl.getSimpleName());

        writeCodeInFile(name, code);
    }

    public void writeInterfaceImplementation(InterfaceDecl interfaceDecl) {
        var imports = getImportsForType(interfaceDecl);
        var name = "%sInterfaceImplementation".formatted(interfaceDecl.getPrettyQualifiedName());
        var methodsImplemented = implementNecessaryMethods(interfaceDecl);

        var code = """
                %s
                
                class %s implements %s {
                %s
                }
                """.formatted(imports, name, interfaceDecl.getSimpleName(), methodsImplemented);

        writeCodeInFile(name, code);
    }

    public void writeMethodInvocation(MethodDecl methodDecl, ClassDecl originalClass) {
        var imports = getImportsForType(originalClass);
        var name = "%sMethodInvocation".formatted(methodDecl.getPrettyQualifiedName());
        var caller = getClassAccessForTypeMember(originalClass, methodDecl);
        var params = getParamsForExecutableInvocation(methodDecl);

        if (methodDecl.isPublic()) {
            var methodInvocationCode = "%s.%s(%s);".formatted(caller, methodDecl.getSimpleName(), params);

            writeCodeInMain(imports, name, methodInvocationCode);
        } else if (methodDecl.isProtected()) {
            var methodInvocationInMethodCode = "\tpublic void aNewMethodToInvokeProtectedMethod() {\n\t\tthis.%s(%s);\n\t}".formatted(methodDecl.getSimpleName(), params);
            var code = CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, originalClass.getSimpleName(), methodInvocationInMethodCode);

            writeCodeInFile(name, code);
        }
    }

    public void writeMethodOverride(MethodDecl methodDecl, ClassDecl originalClass) {
        var imports = getImportsForType(originalClass);
        var name = "%sMethodOverride".formatted(methodDecl.getPrettyQualifiedName());

        var methodImplemented = methodDecl.isStatic()
                ? implementMethod(methodDecl)
                : overrideMethod(methodDecl);

        var code = CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, originalClass.getSimpleName(), methodImplemented);

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

    private String getClassAccessForTypeMember(ClassDecl classDecl, TypeMemberDecl typeMemberDecl) {
        if (typeMemberDecl.isStatic()) {
            return classDecl.getSimpleName();
        } else {
            var sortedConstructors = classDecl.getConstructors().stream()
                    .sorted(Comparator.comparingInt(c -> c.getParameters().size()))
                    .toList();

            if (sortedConstructors.isEmpty()) {
                return "new %s()".formatted(classDecl.getSimpleName());
            }

            var params = getParamsForExecutableInvocation(sortedConstructors.getFirst());
            return "new %s(%s)".formatted(classDecl.getSimpleName(), params);
        }
    }

    private String getParamsForExecutableInvocation(ExecutableDecl executableDecl) {
        return executableDecl.getParameters().stream()
                .map(p -> getDefaultValueForType(p.type().getQualifiedName()))
                .collect(Collectors.joining(", "));
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

        return methodReturnTypeName.equals("void")
                ? "\t" + methodSignature + " {}"
                : "\t%s { return %s; }".formatted(methodSignature, getDefaultValueForType(methodReturnTypeName));
    }

    private String getDefaultValueForType(String typeName) {
        return switch (typeName) {
            case "int", "long", "float", "double", "byte", "short" -> "0";
            case "char" -> "'c'";
            case "boolean" -> "false";
            default -> "null";
        };
    }

    private void writeCodeInMain(String imports, String clientName, String code) {
        var mainClassCode = MAIN_CLASS_TEMPLATE.formatted(imports, clientName, String.join("\n", code));

        writeCodeInFile(clientName, mainClassCode);
    }

    private void writeCodeInFile(String fileName, String code) {
        try {
            var fullCode = FILE_TEMPLATE.formatted(code).getBytes();

            Files.write(Paths.get(outputDir.toString(), fileName + ".java"), fullCode);
        } catch (IOException e) {
            System.err.println("Error writing client code to file: " + e.getMessage());
        }
    }
}
