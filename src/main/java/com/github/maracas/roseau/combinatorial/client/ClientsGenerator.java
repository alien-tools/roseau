package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.combinatorial.client.visit.ClientGenerator;

import java.nio.file.Path;

public class ClientsGenerator {
    public static void main(String[] args) {
        String apiDir = args.length >= 1 ? args[0] : "generated/api/v1";
        String outputDir = args.length >= 2 ? args[1] : "generated/clients";

        APIExtractor apiExtractor = new SpoonAPIExtractor();
        var api = apiExtractor.extractAPI(Path.of(apiDir));

        var clientWriter = new ClientWriter(Path.of(outputDir));
        if (!clientWriter.createOutputDir()) {
            System.exit(1);
        }

        new ClientGenerator(clientWriter).$(api).generate();
    }
}
