# Roseau: Breaking Change Analysis for Java Libraries

Roseau is a **fast** and **accurate** tool for detecting breaking changes between two versions of a library, similar to other tools like [japicmp](https://github.com/siom79/japicmp/), [Revapi](https://github.com/revapi/revapi/) or [Clirr](https://github.com/ebourg/clirr).
Whether you're a library maintainer or a developer worrying about upgrading your dependencies, Roseau helps ensure backward compatibility across versions.

## Key Features

  - Detects both binary-level and source-level breaking changes
  - Indifferently analyzes Java source code (using [JDT](https://github.com/eclipse-jdt/eclipse.jdt.core) or [Spoon](https://github.com/INRIA/spoon)) and compiled JARs (using [ASM](https://asm.ow2.io/))
  - Excellent accuracy and performance
  - Supports Java up to version 21 (including records, sealed types, modules, etc.)
  - CLI-first and scriptable

Like other JAR-based tools, Roseau integrates smoothly into CI pipelines and can analyze artifacts from remote repositories such as Maven Central.
Unlike others, Roseau can also analyze source code directly, making it ideal for checking commits, pull requests, or local changes in an IDE, as well as libraries hosted on platforms like GitHub for which compiled JARs are not readily available.

## In a nutshell

  1. Roseau infers an API model from each version of the library to analyze
  2. It performs side-by-side comparison of the two API models to detect any breaking changes

Roseau builds lightweight, technology-agnostic API models that list all the exported symbols in a library—including types, methods, and fields—along with their properties. These models can be easily serialized and stored (e.g., as JSON) for further analysis or archival.
Roseau relies on either [JDT](https://github.com/eclipse-jdt/eclipse.jdt.core) or [Spoon](https://github.com/INRIA/spoon) to extract API models from source code, and on [ASM](https://asm.ow2.io/) to extract API models from bytecode.
The breaking change detection algorithm is completely agnostic of the underlying parsing technology.

The list of breaking changes considered in Roseau is specified [here](core/src/main/java/io/github/alien/roseau/diff/changes/BreakingChangeKind.java) and drawn from various sources, including the [Java Language Specification](https://docs.oracle.com/javase/specs/), [japicmp's implementation](https://github.com/siom79/japicmp/blob/68425b08dd7835a4e9c0e64c6f6eaf3bd7281069/japicmp/src/main/java/japicmp/model/JApiCompatibilityChange.java), [Revapi's list of API Differences](https://revapi.org/revapi-java/0.28.1/differences.html), the [API evolution benchmark](https://github.com/kjezek/api-evolution-data-corpus) and [our own extensive tests](core/src/test/java/io/github/alien/roseau/diff).
Roseau considers both source-level and binary-level compatibility changes.

## Usage

### As a standalone CLI tool

Download the latest stable version of the CLI JAR from the [releases page](https://github.com/alien-tools/roseau/releases) or build it locally: 

```bash
$ git clone https://github.com/alien-tools/roseau.git
$ mvn package
$ ls cli/target/roseau-cli-0.2.0-jar-with-dependencies.jar 
```

Identify breaking changes between two versions, either from source trees or compiled JARs:

```
$ java -jar roseau-cli-0.2.0-jar-with-dependencies.jar --diff --v1 /path/to/v1.jar --v2 /path/to/v2.jar
  CLASS_NOW_ABSTRACT com.pkg.ClassNowAbstract
    com/pkg/ClassNowAbstract.java:4
$ java -jar roseau-cli-0.2.0-jar-with-dependencies.jar --diff --v1 /path/to/sources-v1 --v2 /path/to/sources-v2
  METHOD_REMOVED com.pkg.Interface.m(int)
    com/pkg/Interface.java:18
```

Roseau supports different modes, output formats, and options:

```
$ java -jar roseau-cli-0.2.0-jar-with-dependencies.jar --help
Usage: roseau [--api] [--diff] [--fail] [--plain] [--verbose]
          [--classpath=<classpathString>] [--extractor=<extractorFactory>]
          [--format=<format>] [--json=<apiPath>] [--pom=<pom>]
          [--report=<reportPath>] --v1=<v1> [--v2=<v2>]
  --api               Serialize the API model of --v1; see --json
  --classpath=<classpathString>
                      A colon-separated list of elements to include in the classpath
  --diff              Compute breaking changes between versions --v1 and --v2
  --extractor=<extractorFactory>
                      API extractor to use: JDT or SPOON (from sources), ASM (from JARs)
  --fail              Return a non-zero code if breaking changes are detected
  --format=<format>   Format of the report; possible values: CSV, HTML, JSON
  --json=<apiPath>    Where to serialize the JSON API model of --v1; defaults to api.json
  --plain             Disable ANSI colors, output plain text
  --pom=<pom>         A pom.xml file to build a classpath from
  --report=<reportPath>
                      Where to write the breaking changes report
  --v1=<v1>           Path to the first version of the library; either a source directory or a JAR
  --v2=<v2>           Path to the second version of the library; either a source directory or a JAR
  --verbose           Print debug information
```

## Citing Roseau
If you use Roseau for academic purposes, please cite: [Roseau: Fast, Accurate, Source-based Breaking Change Analysis in Java](https://hal.science/hal-05176866/document). Corentin Latappy, Thomas Degueule, Jean-Rémy Falleri, Romain Robbes, Lina Ochoa. In _IEEE International Conference on Software Maintenance and Evolution_ (ICSME 2025).

```bibtex
@inproceedings{latappy25roseau,
    author    = {Corentin Latappy and Thomas Degueule and Jean-Rémy Falleri and Romain Robbes and Lina Ochoa},
    title     = {{Roseau}: Fast, Accurate, Source-based Breaking Change Analysis in {Java}},
    booktitle = {{IEEE} International Conference on Software Maintenance and Evolution, {ICSME} 2025, Auckland, New Zealand, September 7-12, 2025},
    publisher = {{IEEE}},
    year      = {2025}
}
```

## License
This repository—and all its content—is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).  („• ‿ •„) 
