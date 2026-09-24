# Roseau: Breaking Change Analysis for Java Libraries

[![Maven Central](https://img.shields.io/maven-central/v/io.github.alien-tools/roseau-core?style=flat-square&color=blue)](https://central.sonatype.com/namespace/io.github.alien-tools)
[![Build](https://img.shields.io/github/actions/workflow/status/alien-tools/roseau/build-main.yml?branch=main&style=flat-square)](https://github.com/alien-tools/roseau/actions/workflows/build-main.yml)
[![Documentation](https://img.shields.io/badge/docs-alien--tools.github.io-informational?style=flat-square)](https://alien-tools.github.io/roseau/)
[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![License: MIT](https://img.shields.io/badge/license-MIT-success?style=flat-square)](LICENSE)

Roseau (/ʁozo/) is a **fast** and **[accurate](https://github.com/alien-tools/api-evolution-benchmark)** tool for detecting breaking changes between library versions, similar to tools like [japicmp](https://github.com/siom79/japicmp/) or [Revapi](https://github.com/revapi/revapi/).
Whether you're a library maintainer or upgrading dependencies in your projects, Roseau helps ensure backward compatibility across versions.

The official user documentation is available at [https://alien-tools.github.io/roseau/](https://alien-tools.github.io/roseau/), including an example [HTML report](https://alien-tools.github.io/roseau/example-report.html).

## Key Features

  - Detects both binary-level and source-level breaking changes
  - Accurate and customizable definition of the API surface (using visibilities, module declarations, annotations, naming conventions)
  - Analyzes both JAR files (using [ASM](https://asm.ow2.io/)) and Java source code (using [JDT](https://github.com/eclipse-jdt/eclipse.jdt.core))
  - Supports Java up to version 25 (including records, sealed types, modules, etc.)
  - Covers [an extensive list of breaking changes](core/src/main/java/io/github/alien/roseau/diff/changes/BreakingChangeKind.java) matching the [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se25/html/index.html), backed by a [thorough test suite](core/src/test/java/io/github/alien/roseau/diff)
  - Excellent accuracy and performance
  - Outputs reports in CSV, HTML, JSON, and Markdown formats
  - CLI-first, with a Maven plug-in and Gradle integration

Like other JAR-based tools, Roseau integrates smoothly into CI pipelines and can analyze artifacts from remote repositories such as Maven Central.
Unlike others, Roseau can also analyze source code directly, making it ideal for checking commits, pull requests, or local changes in an IDE, as well as libraries hosted on platforms like GitHub for which compiled JARs are not readily available.

## Usage

### As a standalone CLI tool

Download the [latest release](https://github.com/alien-tools/roseau/releases/latest) of Roseau: either a standalone archive for your platform, which bundles its own Java runtime, or the executable JAR.
On Linux:

```bash
$ curl -fsSL https://github.com/alien-tools/roseau/releases/download/v0.7.0/roseau-0.7.0-linux-x86_64.zip -o roseau.zip && unzip -q roseau.zip
$ alias roseau="$PWD/roseau-0.7.0-linux-x86_64/bin/roseau"
```

<details>
<summary>Building from sources</summary>
Note that building from sources requires Java 25.

```bash
$ git clone https://github.com/alien-tools/roseau.git
$ cd roseau && ./mvnw package -DskipTests
$ alias roseau='java -jar $PWD/cli/target/roseau-<version>.jar'
```
</details>

Identify breaking changes between two versions, passed as local JARs or source trees, or fetched remotely from Maven. See the [CLI reference](https://alien-tools.github.io/roseau/reference/cli/) for all options.

```
$ roseau --diff --v1 com.google.guava:guava:33.4.0-jre --v2 com.google.guava:guava:33.6.0-jre
Breaking Changes found: 7 (3 binary-breaking, 7 source-breaking)
★ com.google.common.graph.Graph TYPE_NEW_ABSTRACT_METHOD [asNetwork()]
  ✓ binary-compatible ✗ source-breaking
  → com/google/common/graph/Graph.java
✗ com.google.thirdparty.publicsuffix.PublicSuffixPatterns TYPE_REMOVED
  ✗ binary-breaking ✗ source-breaking
  → com/google/thirdparty/publicsuffix/PublicSuffixPatterns.java
[...]
$ roseau --diff --v1 /path/to/v1.jar --v2 /path/to/v2/src/main/java
[...]
```

### As a Maven plug-in

Roseau also provides a Maven plug-in that compares the current artifact against a baseline during the `verify` phase.
The minimal setup is to bind the `check` goal and provide a baseline.
See the [Maven guide](https://alien-tools.github.io/roseau/guides/maven-plugin/) for a complete setup.

```xml
<plugin>
  <groupId>io.github.alien-tools</groupId>
  <artifactId>roseau-maven-plugin</artifactId>
  <version>0.7.0</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <baselineDependency>
      <groupId>com.group</groupId>
      <artifactId>my-artifact</artifactId>
      <version>1.0.1</version>
    </baselineDependency>
    <failOnIncompatibility>true</failOnIncompatibility>
  </configuration>
</plugin>
```

### In a Gradle build

Gradle builds can run Roseau by resolving the published CLI artifact and invoking it with a `JavaExec` task; see the [Gradle guide](https://alien-tools.github.io/roseau/guides/gradle/) for a complete setup, as well as the [JUnit](https://github.com/junit-team/junit-framework/blob/main/gradle/plugins/backward-compatibility/src/main/kotlin/junitbuild/compatibility/roseau/RoseauDiff.kt) and [Caffeine](https://github.com/ben-manes/caffeine/blob/master/gradle/plugins/src/main/kotlin/quality/roseau.caffeine.gradle.kts) builds for real-world integrations.

### As a Java library

Roseau's API is published on [Maven Central](https://central.sonatype.com/namespace/io.github.alien-tools) as `roseau-core`.
The main programmatic entry point is `io.github.alien.roseau.Roseau`. In most cases, you configure two `Library` instances, build their APIs, and diff them:

```java
Library v1 = Library.of(Path.of("/path/to/library-v1.jar"));
Library v2 = Library.builder()
  .location(Path.of("/path/to/library-v2.jar"))
  .classpath(List.of(Path.of("/path/to/dependency.jar")))
  .build();

API apiV1 = Roseau.buildAPI(v1);
API apiV2 = Roseau.buildAPI(v2);
RoseauReport report = Roseau.diff(apiV1, apiV2);
report.getBreakingChanges().forEach(System.out::println);
```

## Configuration
Roseau accepts a YAML configuration file supplied using the `--config` option.
Options also set on the CLI or in the Maven plug-in take precedence.
See the [YAML configuration guide](https://alien-tools.github.io/roseau/guides/config/) for the full reference.

```yaml
common:
  excludes: # Exclude certain APIs from compatibility checks
    names: [ com\.library\.internal\..* ] # Package naming conventions
    annotations:
      - name: com.google.common.annotations.Beta # Exclude @Beta APIs
      - name: org.apiguardian.api.API # Exclude @API(status = INTERNAL) APIs
        args: { status: org.apiguardian.api.API$Status.INTERNAL }
  classpath:
    pom: /path/to/pom.xml
    jars: [ /path/to/dependency.jar ]
diff:
  ignore: ignored-breaking-changes.csv # Ignore a list of intentional known breaking changes
  binaryOnly: true # Report binary incompatibilities only
reports:
  - file: ./reports/bcs.html
    format: HTML
  - file: ./reports/bcs.csv
    format: CSV
```

## Citing Roseau
If you use Roseau for academic purposes, please cite: [Roseau: Fast, Accurate, Source-based Breaking Change Analysis in Java](https://hal.science/hal-05176866/document). Corentin Latappy, Thomas Degueule, Jean-Rémy Falleri, Romain Robbes, Lina Ochoa. In _IEEE International Conference on Software Maintenance and Evolution_ (ICSME 2025).

```bibtex
@inproceedings{roseau,
    author    = {Corentin Latappy and Thomas Degueule and Jean-Rémy Falleri and Romain Robbes and Lina Ochoa},
    title     = {{Roseau}: Fast, Accurate, Source-based Breaking Change Analysis in {Java}},
    booktitle = {{IEEE} International Conference on Software Maintenance and Evolution, {ICSME} 2025, Auckland, New Zealand, September 7-12, 2025},
    pages     = {517--528},
    doi       = {10.1109/ICSME64153.2025.00053},
    year      = {2025}
}
```

## License
This repository—and all its content—is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).  („• ‿ •„)
