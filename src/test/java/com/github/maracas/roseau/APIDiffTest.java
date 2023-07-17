package com.github.maracas.roseau;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.nio.file.Path;

class APIDiffTest {

    APIDiff diff;


    @BeforeEach
    void setUp() {

        Path v1 = Path.of("src/test/resources/api-extractor-tests/without-modules/v1");
        Launcher launcher1 = new MavenLauncher(v1.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
        launcher1.getEnvironment().setNoClasspath(true);
        APIExtractor extractor1 = new APIExtractor(launcher1.buildModel());

        Path v2 = Path.of("src/test/resources/api-extractor-tests/without-modules/v2");
        Launcher launcher2 = new MavenLauncher(v2.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
        launcher2.getEnvironment().setNoClasspath(true);
        APIExtractor  extractor2 = new APIExtractor(launcher2.buildModel());

        diff = new APIDiff(extractor1.dataProcessing(extractor1), extractor2.dataProcessing(extractor2));
    }


    @Test
    void breaking_changes_testing() {

        // Structuring the breaking changes
        diff.diffTesting();

        // Printing the breaking changes
        //System.out.println("\nTHE BREAKING CHANGES LIST :\n");
        diff.diffPrinting();

        // diff.trying();

    }


}