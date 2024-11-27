package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.*;
import org.xmlet.htmlapifaster.S;

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

    public ClientWriter(Path outputDir) {
        this.outputDir = outputDir;
    }

    public boolean createOutputDir() {
        try {
            if (outputDir.toFile().exists()) return true;

            return outputDir.toFile().mkdirs();
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

    public void writeConstructorInvocation(ConstructorDecl constructorDecl, ClassDecl originalClass) {
        var imports = getImportsForType(originalClass);
        var name = "%sConstructorInvocation".formatted(constructorDecl.getPrettyQualifiedName());
        var params = getParamsForExecutableInvocation(constructorDecl);

        if (constructorDecl.isPublic()) {
            var code = "new %s(%s);".formatted(originalClass.getSimpleName(), params);

            writeCodeInMain(imports, name, code);
        } else if (constructorDecl.isProtected()) {
            var constructorSuper = "\t%s() {\n\t\tsuper(%s);\n\t}".formatted(name, params);
            var code = CLASS_INHERITANCE_TEMPLATE.formatted(imports, name, originalClass.getSimpleName(), constructorSuper);

            writeCodeInFile(name, code);
        }
    }

    public void writeFieldRead(FieldDecl fieldDecl, ClassDecl originalClass) {
        var imports = getImportsForType(originalClass);
        var name = "%sFieldRead".formatted(fieldDecl.getPrettyQualifiedName());

        var caller = getClassAccessForTypeMember(originalClass, fieldDecl);
        var code = "var val = %s.%s;".formatted(caller, fieldDecl.getSimpleName());

        writeCodeInMain(imports, name, code);
    }

    public void writeFieldWrite(FieldDecl fieldDecl, ClassDecl originalClass) {
        var imports = getImportsForType(originalClass);
        var name = "%sFieldWrite".formatted(fieldDecl.getPrettyQualifiedName());

        var caller = getClassAccessForTypeMember(originalClass, fieldDecl);
        var value = getDefaultValueForType(fieldDecl.getType().getQualifiedName());
        var code = "%s.%s = %s;".formatted(caller, fieldDecl.getSimpleName(), value);

        writeCodeInMain(imports, name, code);
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
        return "\t@Override\n\t" + implementMethod(methodDecl);
    }

    private String implementMethod(MethodDecl methodDecl) {
        var methodReturnTypeName = methodDecl.getType().getQualifiedName();
        var methodSignature = methodDecl.toString().replace("abstract ", "");

        return methodReturnTypeName.equals("void")
                ? methodSignature + " {}"
                : "%s { return %s; }".formatted(methodSignature, getDefaultValueForType(methodReturnTypeName));
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
