# Roseau
Roseau is a *fast* and *accurate* tool designed to identify breaking changes between two versions of a library, similar to other tools like [japicmp](https://github.com/siom79/japicmp/), [RevApi](https://github.com/revapi/revapi/) or [Clirr](https://github.com/ebourg/clirr).
It supports Java versions up to 21.
In contrast with other tools, Roseau accepts *source code* as input in addition to pre-compiled JARs which are not always readily available or obtainable.
This makes it particularly suitable for analyzing commits, pull requests, or local code in an IDE, as well as libraries hosted on software forges such as GitHub.

In a nutshell:
  - Roseau first extracts the API of each version of the library to analyze
  - Roseau then performs side-by-side comparison of the two API models to infer the list of breaking changes

Roseau's API models are lightweight, technology-agnostic, and can be easily serialized and stored for later analyses.
API models embody the exported symbols of a software library (types, methods, and fields) and their properties.
Roseau relies on a customized version of [Spoon/JDT](https://github.com/INRIA/spoon) to extract API models from source code, and on [ASM](https://asm.ow2.io/) to extract API models from bytecode.
The breaking change inference algorithm is completely agnostic of the underlying parsing technology.

## Breaking changes
The list of breaking changes considered in Roseau is drawn from various sources, including the [Java Language Specification](https://docs.oracle.com/javase/specs/), [japicmp's implementation](https://github.com/siom79/japicmp/blob/68425b08dd7835a4e9c0e64c6f6eaf3bd7281069/japicmp/src/main/java/japicmp/model/JApiCompatibilityChange.java), [Revapi's list of API Differences](https://revapi.org/revapi-java/0.28.1/differences.html), the [API evolution benchmark](https://github.com/kjezek/api-evolution-data-corpus) and our own tests.
We consider both source-level and binary-level compatibility changes.

## Usage

### As a standalone CLI tool

```
$ git clone https://github.com/alien-tools/roseau.git
$ mvn package appassembler:assemble
$ target/appassembler/bin/roseau --diff --v1 /path/to/v1.jar --v2 /path/to/v2.jar
$ target/appassembler/bin/roseau --diff --v1 /path/to/sources-v1 --v2 /path/to/sources-v2
  CLASS_NOW_ABSTRACT com.pkg.ClassNowAbstract
    com/pkg/ClassNowAbstract.java:4
  METHOD_REMOVED com.pkg.Interface.m(int)
    com/pkg/Interface.java:18
```

Roseau supports different modes, output formats, and options:

```
$ cli/target/appassembler/bin/roseau --help
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

### Git Integration

Roseau can easily be integrated with Git to compare arbitrary commits, refs, branches, etc.
The following minimal `.gitconfig` registers Roseau as a difftool aliased to `bc`:

```
[difftool "roseau"]
  cmd = /path/to/roseau --diff --v1 "$LOCAL" --v2 "$REMOTE"
[alias]
  bc = difftool -d -t roseau
```

Then, Roseau can be invoked on Git objects using the usual syntax, for example:

```
$ git bc                   # BCs in unstaged changes
$ git bc HEAD              # BCs in uncommitted changes (including staged ones)
$ git bc --staged          # BCs in staged changes
$ git bc path/to/File.java # BCs in specific file
$ git bc main..feature     # BCs between two branches
$ git bc HEAD~2 HEAD       # BCs between two commits
```

## License
This repository—and all its content—is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).  („• ‿ •„) 
