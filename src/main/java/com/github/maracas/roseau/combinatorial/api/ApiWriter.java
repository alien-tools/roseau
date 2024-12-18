package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.visit.APIPrettyPrinter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ApiWriter {
    private final Path outputDir;

    public ApiWriter(Path outputDir) {
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

    public void write(API api) {
        var prettyPrinter = new APIPrettyPrinter();

        api.getAllTypes().forEach(t -> {
            var file = outputDir.resolve(t.getQualifiedName().replace('.', '/') + ".java");

            try {
                var code = prettyPrinter.$(t).print();

                file.toFile().createNewFile();
                Files.writeString(file, code);
                System.out.println("Generated " + file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
