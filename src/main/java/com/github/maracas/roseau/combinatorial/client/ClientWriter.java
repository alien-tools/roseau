package com.github.maracas.roseau.combinatorial.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class ClientWriter {
    private final Path outputDir;

    private static final String MAIN_CLASS_TEMPLATE = """
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
            return outputDir.toFile().mkdirs();
        } catch (SecurityException e) {
            System.err.println("Error creating output directory: " + e.getMessage());
            return false;
        }
    }

    public void writeClient(String clientName, String clientCode) {
        try {
            var fullClientCode = MAIN_CLASS_TEMPLATE.formatted(clientName, String.join("\n", clientCode));

            Files.write(Paths.get(outputDir.toString(), clientName + ".java"), fullClientCode.getBytes());
        } catch (IOException e) {
            System.err.println("Error writing client code to file: " + e.getMessage());
        }
    }
}
