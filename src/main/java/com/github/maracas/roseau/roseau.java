package com.github.maracas.roseau;

import com.github.maracas.roseau.changes.BreakingChange;
import spoon.Launcher;
import spoon.MavenLauncher;

import java.nio.file.Path;

import java.util.List;


/**
 * The `roseau` class is the main entry point of the project.
 */

public class roseau {



    public static void main(String[] args) {

        try{

            String path1 = args[0];
            String path2 = args[1];

            Path v1 = Path.of(path1);
            Launcher launcher1 = new MavenLauncher(v1.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
            launcher1.getEnvironment().setNoClasspath(true);
            APIExtractor extractor1 = new APIExtractor(launcher1.buildModel());

            Path v2 = Path.of(path2);
            Launcher launcher2 = new MavenLauncher(v2.toString(), MavenLauncher.SOURCE_TYPE.APP_SOURCE, new String[0]);
            launcher2.getEnvironment().setNoClasspath(true);
            APIExtractor extractor2 = new APIExtractor(launcher2.buildModel());

            APIDiff diff = new APIDiff(extractor1.extractingAPI(), extractor2.extractingAPI());

            List<BreakingChange> breakingChanges = diff.getBreakingChanges();
            diff.breakingChangesReport();
            System.out.println(diff.toString());


        } catch (spoon.SpoonException e) {
            System.err.println(" Please provide two valid paths ");

        }
    }

}
