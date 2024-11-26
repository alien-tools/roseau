package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.TypeDecl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ClientWriter {
    private final Path outputDir;

    private static final String FILE_TEMPLATE = """
            package generated.clients;

            %s
            """;

    private static final String MAIN_CLASS_TEMPLATE = """
            %s

            public class %s {
                public static void main(String[] args) {
                    %s
                }
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

        var code = """
        %s
        
        class %s extends %s {
        %s
        }
        """.formatted(imports, name, classDecl.getSimpleName(), methodsImplemented);

        writeCodeInFile(name, code);
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
            case "char" -> "'\\u0000'";
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
