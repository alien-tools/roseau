package com.github.maracas.roseau;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.diff.APIDiff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.nio.file.Files;
import java.nio.file.Path;


class APIDiffTest {

    APIDiff diff;

    public Launcher launcherFor(Path location) {
        Launcher launcher ;

        if (Files.exists(location.resolve("pom.xml")))

            launcher = new MavenLauncher(location.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
        else {
            launcher = new Launcher();
            launcher.getEnvironment().setComplianceLevel(11);

            launcher.addInputResource(location.toString());

        }
        // Ignore missing types/classpath related errors
        launcher.getEnvironment().setNoClasspath(true);
        // Proceed even if we find the same type twice; affects the precision of the result
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        // Ignore files with syntax/JLS violations and proceed
        launcher.getEnvironment().setIgnoreSyntaxErrors(true);
        return launcher;
    }

    @BeforeEach
    void setUp() {


        Path v1 = Path.of("src/test/resources/api-extractor-tests/without-modules/v1");
        Launcher launcher1 = launcherFor(v1);

        SpoonAPIExtractor extractor1 = new SpoonAPIExtractor(launcher1.buildModel());

        Path v2 = Path.of("src/test/resources/api-extractor-tests/without-modules/v2");
        Launcher launcher2 = launcherFor(v2);

        SpoonAPIExtractor extractor2 = new SpoonAPIExtractor(launcher2.buildModel());

        diff = new APIDiff(extractor1.extractAPI(), extractor2.extractAPI());
    }


    @Test
    void breaking_changes_testing() {

        // Structuring the breaking changes


        System.out.println(diff.toString());


    }


}