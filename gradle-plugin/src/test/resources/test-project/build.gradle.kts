plugins {
    id("java")
    // The roseau plugin.  Make sure it has been published to mavenLocal first:
    //   cd ../../.. && gradle publishToMavenLocal
    id("io.github.alien-tools.roseau") version "0.7.0-SNAPSHOT"
}

// ------------------------------------------------------------------
// 1.  Build the v1 (baseline) JAR and publish it to a file-based
//    Maven repository so Roseau can resolve it.
// ------------------------------------------------------------------

val baselineSrc = layout.projectDirectory.dir("baseline-src")
val baselineClasses = layout.buildDirectory.dir("baseline-classes")
val baselineJar = layout.buildDirectory.file("baseline/v1-lib-1.0.0.jar")
val localMavenRepo = layout.buildDirectory.dir("local-maven-repo")

val compileBaseline by tasks.registering(JavaCompile::class) {
    description = "Compiles the v1 baseline sources"
    source = fileTree(baselineSrc)
    destinationDirectory = baselineClasses
    classpath = sourceSets.main.get().compileClasspath
    options.release = 8
}

val jarBaseline by tasks.registering(Jar::class) {
    description = "Packages the v1 baseline into a JAR"
    dependsOn(compileBaseline)
    archiveFileName = "v1-lib-1.0.0.jar"
    destinationDirectory = layout.buildDirectory.dir("baseline")
    from(baselineClasses)
}

val publishBaseline by tasks.registering(Copy::class) {
    description = "Publishes the v1 baseline JAR to a local Maven file-repository"
    dependsOn(jarBaseline)
    from(jarBaseline)
    into(localMavenRepo.map { it.dir("test/v1-lib/1.0.0") })
    rename { "v1-lib-1.0.0.jar" }

    doLast {
        // Minimal POM so the Maven resolver can locate the artifact
        val pomDir = localMavenRepo.get().dir("test/v1-lib/1.0.0").asFile
        pomDir.resolve("v1-lib-1.0.0.pom").writeText("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>test</groupId>
              <artifactId>v1-lib</artifactId>
              <version>1.0.0</version>
            </project>
        """.trimIndent())
    }
}

// The roseauCheck task must wait until the baseline has been published
tasks.named("roseauCheck") {
    dependsOn(publishBaseline)
}

// ------------------------------------------------------------------
// 2.  Roseau plugin configuration
// ------------------------------------------------------------------

roseau {
    // Maven coordinates of the library we're analysing
    mvnCoord = "test:v1-lib"

    // Baseline version — resolved from the local file repo
    v1 = "1.0.0"

    // When v2 is omitted the current project's JAR (jar task) is used.
    // That's exactly what we want: compare baseline-src  vs  src/main/java.

    // Fail if breaking changes are found
    failOnBreaking = true

    // Directory where reports are written
    reportsDir = layout.buildDirectory.dir("reports/roseau")

    // The local file repo where we published the baseline
    mvnRepo {
        maven { url = uri(localMavenRepo.get().asFile.toURI()) }
    }

    // -- Exclude certain symbols from the report --
    excludes {
        // Exclude by regex (example — not used in this test project)
        // names = listOf("pkg\\.Util.*")

        // Exclude by annotation
        // annotation("java.lang.Deprecated")
        // annotation("org.apiguardian.api.API") { arg("status", "INTERNAL") }
    }

    // -- Generate reports in multiple formats --
    reports {
        csv("roseau.csv")
        html("roseau.html")
        json("roseau.json")
    }
}
