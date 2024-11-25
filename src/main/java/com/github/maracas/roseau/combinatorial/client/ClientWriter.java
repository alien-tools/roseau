package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.TypeDecl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

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

    public void writeCodeInFile(ClassDecl classDecl) {
        writeTypeReference(classDecl);

        writeClassInstantiation(classDecl);
        writeClassInheritance(classDecl);
    }

    private void writeClassInheritance(ClassDecl classDecl) {
        if (!classDecl.isExported() || classDecl.isEffectivelyFinal()) return;

        // TODO: Better handle if class is abstract
        if (classDecl.isEffectivelyAbstract()) return;

        var imports = getImportsForType(classDecl);
        var name = "%sClassInheritance".formatted(classDecl.getPrettyQualifiedName());
        var code = "%s\n\nclass %s extends %s {}".formatted(imports, name, classDecl.getSimpleName());

        writeCodeInFile(name, code);
    }

    private void writeClassInstantiation(ClassDecl classDecl) {
        if (!classDecl.isExported()) return;

        var imports = getImportsForType(classDecl);
        var name = "%sClassInstantiation".formatted(classDecl.getPrettyQualifiedName());
        var code = "new %s();".formatted(classDecl.getSimpleName());

        writeCodeInMain(imports, name, code);
    }

    private void writeTypeReference(TypeDecl typeDecl) {
        if (!typeDecl.isExported()) return;

        var imports = getImportsForType(typeDecl);
        var name = "%sTypeReference".formatted(typeDecl.getPrettyQualifiedName());
        var code = "%s ref;".formatted(typeDecl.getSimpleName());

        writeCodeInMain(imports, name, code);
    }

    private String getImportsForType(TypeDecl classDecl) {
        return "import %s;".formatted(classDecl.getQualifiedName());
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
