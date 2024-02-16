# Roseau
Roseau is a static analysis tool designed to identify breaking changes between two versions of a library, similar to other tools like [japicmp](https://github.com/siom79/japicmp/), [Revapi](https://github.com/revapi/revapi/) or [Clirr](https://github.com/ebourg/clirr).
In contrast with other tools, Roseau takes *source code* as input rather than pre-compiled JARs which are not always readily available or obtainable.
This makes it particularly suitable for analyzing libraries hosted on software forges such as GitHub (for instance analyzing the impact of a pull request), or for analyzing library evolution directly within an IDE.

Contrary to other tools, Roseau works as follows:
  - It first extracts the API of each version of the library to analyze
  - It then performs side-by-side comparison/diff of the two API models to infer the list of breaking changes

Roseau's API models are technology-agnostic and can be easily serialized and stored for later analyses.
API models embody the exported symbols of a software library (types, methods, and fields) and their properties.
Roseau's current sole implementation of source code-to-API inference relies on [Spoon](https://github.com/INRIA/spoon), but it would be trivial to implement alternative implementations using [javaparser](https://github.com/javaparser/javaparser/) or binary-to-API inference using e.g. [ASM](https://asm.ow2.io/).
The breaking change inference algorithm is completely agnostic of the underlying parsing technology and only works at the level of API models.

## Breaking changes
The list of breaking changes considered in Roseau is drawn from various sources, including the [Java Language Specification](https://docs.oracle.com/javase/specs/), [japicmp's implementation](https://github.com/siom79/japicmp/blob/68425b08dd7835a4e9c0e64c6f6eaf3bd7281069/japicmp/src/main/java/japicmp/model/JApiCompatibilityChange.java), 
and [Revapi's list of API Differences](https://revapi.org/revapi-java/0.28.1/differences.html).
We consider both source and binary compatibility changes.

## Usage

### As a standalone CLI tool

```
$ git clone https://github.com/alien-tools/roseau.git
$ mvn package appassembler:assemble
$ target/appassembler/bin/roseau --diff --v1 /path/to/version1 --v2 /path/to/version2
```

```
$ target/appassembler/bin/roseau
Usage: roseau [--api] [--diff] [--fail] [--verbose] [--json=<apiPath>]
              [--report=<reportPath>] --v1=<libraryV1> [--v2=<libraryV2>]
      --api              Build and serialize the API model of --v1
      --diff             Compute the breaking changes between versions --v1 and
                           --v2
      --fail             Command returns an error when there are breaking
                           changes detected
      --json=<apiPath>   Where to serialize the JSON API model of --v1;
                           defaults to api.json
      --report=<reportPath>
                         Where to write the breaking changes report; defaults
                           to report.csv
      --v1=<libraryV1>   Path to the sources of the first version of the library
      --v2=<libraryV2>   Path to the sources of the second version of the
                           library
      --verbose          Print debug information
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
$ git bc # BCs in unstaged changes
$ git bc HEAD # BCs in uncommitted changes (including staged ones)
$ git bc --staged # BCs in staged changes
$ git bc path/to/File.java # BCs in specific file
$ git bc main..feature # BCs between two branches
$ git bc HEAD~2 HEAD # BCs between two commits
```

## Tests
Roseau's efficiency was evaluated using Kamil Jezek and Jens Dietrich's [API evolution data corpus](https://github.com/kjezek/api-evolution-data-corpus), a benchmark for evaluating the accuracy of breaking change detection tools.
We introduced new features to enhance the evaluation process. In fact, we not only atomized all the breaking change files to ensure a more granular evaluation, but we also incorporated an assessment of precision, recall, 
and performance.
With this [upgraded version of the benchmark](https://github.com/labri-progress/api-evolution-data-corpus), Roseau currently achieves a precision of 85% and a recall of 91%.

## License
This repository—and all its content—is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).  („• ‿ •„) 
