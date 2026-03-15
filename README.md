# Roseau: Breaking Change Analysis for Java Libraries

Roseau (/ʁozo/) is a **fast** and **accurate** tool for detecting breaking changes between library versions, similar to tools like [japicmp](https://github.com/siom79/japicmp/) or [Revapi](https://github.com/revapi/revapi/).
Whether you're a library maintainer or upgrading dependencies in your projects, Roseau helps ensure backward compatibility across versions.
Roseau analyzes both JAR files and source code, is highly configurable, and includes a [dedicated Maven plug-in](guides/maven-plugin.md).

## Key Features

  - Detects both binary-level and source-level breaking changes
  - Accurate and customizable definition of the API surface (using visibilities, modules, annotations, naming conventions, etc.) 
  - Indifferently analyzes JAR files (using [ASM](https://asm.ow2.io/)) or Java source code (using [JDT](https://github.com/eclipse-jdt/eclipse.jdt.core))
  - Excellent accuracy and performance
  - Supports Java up to version 25 (including records, sealed types, modules, etc.)
  - Outputs reports in CSV, HTML, JSON, and Markdown formats
  - Highly configurable, CLI-first, and scriptable

Like other JAR-based tools, Roseau integrates smoothly into CI pipelines and can analyze artifacts from remote repositories such as Maven Central.
Unlike others, Roseau can also analyze source code directly, making it ideal for checking commits, pull requests, or local changes in an IDE, as well as libraries hosted on platforms like GitHub for which compiled JARs are not readily available.

## In a nutshell

  1. Roseau infers the exact API of each version of the library to analyze
  2. It performs side-by-side comparison of the two APIs to detect any breaking changes

Roseau builds lightweight, technology-agnostic API models that list all the exported symbols in a library—including types, methods, and fields—along with their properties. These models can be easily serialized and stored as JSON for further analysis or archival.
Roseau relies on either [JDT](https://github.com/eclipse-jdt/eclipse.jdt.core) to extract API models from source code, and on [ASM](https://asm.ow2.io/) to extract API models from bytecode.


The breaking change detection algorithm is efficient, agnostic of the underlying parsing technology, and is [extensively tested](core/src/test/java/io/github/alien/roseau/diff).
The list of source-level and binary-level breaking changes considered in Roseau is specified [here](core/src/main/java/io/github/alien/roseau/diff/changes/BreakingChangeKind.java) and matches the [Java Language Specification](https://docs.oracle.com/javase/specs/).

## Usage

### As a standalone CLI tool

Download the latest stable version of the CLI JAR from the [releases page](https://github.com/alien-tools/roseau/releases) or build the latest version locally (Java 25 required): 

```bash
$ git clone https://github.com/alien-tools/roseau.git
$ cd roseau && ./mvnw package -DskipTests
$ alias roseau='java -jar $PWD/cli/target/roseau-cli-<version>-jar-with-dependencies.jar --help' 
```

Identify breaking changes between two versions, either from compiled JARs or source trees:

```
$ roseau --diff --v1 /path/to/v1.jar --v2 /path/to/v2.jar
Breaking changes found: 3 (2 binary-breaking, 2 source-breaking)
✗ com.pkg.A TYPE_REMOVED
  ✗ binary-breaking ✗ source-breaking
  → com/pkg/A.java:4
⚠ com.pkg.B.f FIELD_NOW_STATIC
  ✗ binary-breaking ✓ source-compatible
  → com/pkg/B.java:18
★ com.pkg.C TYPE_NEW_ABSTRACT_METHOD [toOverride()]
  ✓ binary-compatible ✗ source-breaking
  → com/pkg/C.java:210
```

Roseau supports different modes, output formats, and options:

```
$ roseau --help
Usage: roseau [-hVv] [--binary-only] [--fail-on-bc] [--plain] [--source-only]
              [--api-json=<path>] [--classpath=<path>[,<path>...]]
              [--config=<path>] [--ignored=<path>] [--pom=<path>] [--v1=<path>]
              [--v1-classpath=<path>[,<path>...]] [--v1-pom=<path>]
              [--v2=<path>] [--v2-classpath=<path>[,<path>...]]
              [--v2-pom=<path>] [--report=<format=path>]... (--api | --diff)
      --api               Serialize the API model of --v1; see --api-json
      --diff              Compute breaking changes between versions --v1 and --v2
      --v1=<path>         Path to the first version of the library; a JAR file or a source directory (e.g., src/main/java)
      --v2=<path>         Path to the second version of the library; a JAR file or a source directory (e.g., src/main/java)
      --api-json=<path>   Where to serialize the Json API model of --v1 in --api mode
      --report=<format=path> Write a breaking changes report in the given format to the given path; repeatable (formats: CLI, CSV, HTML, JSON, MD)
      --classpath=<path>[,<path>...] A colon-separated list of JARs to include in the classpath (Windows: semi-colon), shared by --v1 and --v2
      --pom=<path>        A pom.xml file to extract the classpath from, shared by --v1 and --v2
      --v1-classpath=<path>[,<path>...] A --classpath for --v1
      --v2-classpath=<path>[,<path>...] A --classpath for --v2
      --v1-pom=<path>     A --pom for --v1
      --v2-pom=<path>     A --pom for --v2
      --binary-only       Only report binary-breaking changes
      --source-only       Only report source-breaking changes
      --ignored=<path>    Do not report the breaking changes listed in the given CSV file; this CSV file shares the same structure as a CSV report
      --config=<path>     A roseau.yaml config file; CLI options take precedence over these options
      --fail-on-bc        Return with exit code 1 if breaking changes are detected
      --plain             Disable ANSI colors, output plain text
  -v, --verbose           Increase verbosity (-v, -vv).
  -h, --help              Show this help message and exit.
  -V, --version           Print version information and exit.
```

### As a Java library

The main programmatic entry points live in `Roseau`. In most cases, you configure two `Library` instances, build their APIs, and diff them:

```java
Library v1 = Library.of(Path.of("/path/to/library-v1.jar"));
Library v2 = Library.builder()
  .location(Path.of("/path/to/library-v2.jar"))
  .classpath(List.of(Path.of("/path/to/dependency.jar")))
  .pom(Path.of("/path/to/pom.xml"))
  .build();

API apiV1 = Roseau.buildAPI(v1);
API apiV2 = Roseau.buildAPI(v2);
RoseauReport report = Roseau.diff(apiV1, apiV2);
report.getBreakingChanges().forEach(System.out::println);
```

### As a Maven plug-in

Roseau also provides a Maven plug-in that compares the current artifact against a baseline during the `verify` phase.
The minimal setup is to bind the `check` goal and provide a baseline:

```xml
<plugin>
  <groupId>io.github.alien-tools</groupId>
  <artifactId>roseau-maven-plugin</artifactId>
  <version>${roseau.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <!-- Compare against a baseline JAR -->
    <baselineJar>${project.basedir}/old.jar</baselineJar>
    <!-- Compare against a previous version -->
    <baselineVersion>
      <groupId>com.group</groupId>
      <artifactId>my-artifact</artifactId>
      <version>1.0.1</version>
    </baselineVersion>
    <failOnIncompatibility>true</failOnIncompatibility>
  </configuration>
</plugin>
```

## Configuration
Roseau accepts a YAML configuration file supplied using the `--config` option. If an option is specified both on the CLI and in the configuration file, the CLI option takes precedence. Example:

```yaml
common:
  classpath:
    pom: /path/to/pom.xml
    jars: [ /path/to/dependency.jar ]
v1:
  apiReport: ./reports/v1.json
v2:
  apiReport: ./reports/v2.json
diff:
  ignore: ignored-breaking-changes.csv
  binaryOnly: true
reports:
  - file: ./reports/guava.html
    format: HTML
  - file: ./reports/guava.csv
    format: CSV
```

### Ignoring breaking changes on specific types and symbols
Roseau can be configured to ignore breaking changes on symbols matching a given regular expression or annotated with a specific annotation:

```yaml
common:
  excludes:
    names: [ com\.google\.common\..* ]
    annotations:
      - name: com.google.common.annotations.Beta
      - name: org.apiguardian.api.API
        args: { status: org.apiguardian.api.API$Status.INTERNAL }
```

### Ignoring specific breaking changes
Breaking changes are sometimes necessary and intended. To avoid reporting the same breaking changes over and over against a given baseline, Roseau can be configured to ignore/accept specific breaking changes and stop reporting them using a dedicated CSV file supplied using the `--ignored` option:

```csv
type;symbol;kind
pkg.T;pkg.T.m();METHOD_REMOVED
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
