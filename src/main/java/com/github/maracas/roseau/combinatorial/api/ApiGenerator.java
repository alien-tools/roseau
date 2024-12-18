package com.github.maracas.roseau.combinatorial.api;

import java.nio.file.Path;

public class ApiGenerator {
    public static void main(String[] args) {
        String outputDir = args.length >= 1 ? args[0] : "generated/api/v1";

        try {
            var apiWriter = new ApiWriter(Path.of(outputDir));
            if (!apiWriter.createOutputDir()) {
                System.exit(1);
            }

            var combinatorialApi = new CombinatorialApi();
            combinatorialApi.build();

            var api = combinatorialApi.getAPI();
            System.out.println("api="+api);
            apiWriter.write(api);
        } catch (Exception e) {
            System.err.println("Failed to build API in " + outputDir);
            System.err.println(e.getMessage());

            System.exit(1);
        }
    }
}
