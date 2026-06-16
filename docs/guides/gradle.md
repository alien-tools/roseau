# Gradle

Roseau does not currently provide a dedicated Gradle plug-in. Gradle builds can still run Roseau by resolving the published CLI artifact and invoking it with a `JavaExec` task.

## Minimal setup

Add a resolvable configuration for the Roseau CLI, then register a verification task:

```kotlin title="build.gradle.kts"
val roseau by configurations.creating

dependencies {
  roseau("io.github.alien-tools:roseau-cli:0.6.0")
}

tasks.register<JavaExec>("roseauCheck") {
  group = "verification"
  description = "Checks API breaking changes with Roseau"

  dependsOn(tasks.named("jar"))

  classpath = roseau
  mainClass.set("io.github.alien.roseau.cli.RoseauCLI")
  javaLauncher.set(
    javaToolchains.launcherFor {
      languageVersion.set(JavaLanguageVersion.of(25))
    },
  )

  doFirst {
    val currentJar = tasks.named<Jar>("jar").get().archiveFile.get().asFile
    val reportsDir = layout.buildDirectory.dir("reports/roseau").get().asFile

    args(
      "--diff",
      "--v1", "com.example:my-library:1.2.3",
      "--v2", currentJar.absolutePath,
      "--classpath", sourceSets.main.get().compileClasspath.asPath,
      "--plain",
      "--fail-on-bc",
      "--report", "HTML=${reportsDir.resolve("report.html")}",
      "--report", "CSV=${reportsDir.resolve("report.csv")}",
    )
  }
}

tasks.named("check") {
  dependsOn("roseauCheck")
}
```

Then run:

```bash
./gradlew roseauCheck
```

`--v1` is the baseline release, usually the latest released Maven coordinates for the artifact. `--v2` is the JAR produced by the current build.
If your project does not use the standard `jar` task for the artifact you publish, replace `jar` with the task that produces that artifact.
