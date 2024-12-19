package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.combinatorial.client.visit.ClientGenerator;

import java.nio.file.Path;

public class ClientsGenerator {
    public static void main(String[] args) {
        String apiDir = args.length >= 1 ? args[0] : "generated/api/v1";
        String outputDir = args.length >= 2 ? args[1] : "generated/clients";

        Path apiPath = Path.of(apiDir);
        if (!apiPath.toFile().exists()) {
            System.err.println("API directory " + apiDir + " does not exist");
            System.exit(1);
        }

        try {
            APIExtractor apiExtractor = new SpoonAPIExtractor();
            var api = apiExtractor.extractAPI(apiPath);

            var clientWriter = new ClientWriter(Path.of(outputDir));
            if (!clientWriter.createOutputDir()) {
                System.exit(1);
            }

            new ClientGenerator(clientWriter).$(api).visit();
        } catch (Exception e) {
            System.err.println("Failed to extract API from " + apiDir);
            System.err.println(e.getMessage());

            System.exit(1);
        }
    }
}
