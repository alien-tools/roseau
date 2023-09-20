# Roseau


Roseau is a Java-centric tool designed to identify breaking changes between two 
library versions by taking their source codes, extracting their APIs, 
and performing a side-by-side comparison. It aims to help developers 
track potential compatibility issues before upgrading to newer library versions.




## Motivation

The first motivation behind Roseau lies in the fundamental importance of 
detecting breaking changes within software libraries.
In the ever-changing world of software, updates and  enhancements are 
introduced by library developers all the time.
However, these changes can also lead to API modifications,
potentially causing compatibility issues for dependent applications.

The second motivation stems from the limitations of current tools. While
several API checkers already tackle this issue and detect these 
incompatibilities, most of them rely on JAR files as inputs, 
which can be restrictive when these files are not readily available.
Roseau addresses this limitation by directly analyzing source code, 
providing a more flexible and comprehensive solution for API breaking 
change detection.







## Description

Roseau works its magic by harnessing the power of [Spoon](https://spoon.gforge.inria.fr/),
a powerful framework that parses
the source files of the provided Java libraries to construct a well-structured Abstract 
Syntax Tree (AST). Using this tool, Roseau extracts the APIs of the given  library
versions. This includes classes, methods, fields, and other relevant components 
defining the interface.

A side by side comparison is then performed to detect and categorize potential breaking changes. 
The selection of differences (BCs) considered was drawn from various sources, including [Oracle's documentation about binary compatibility](https://docs.oracle.com/javase/specs/jls/se7/html/jls-13.html#jls-13.4.4),
[japicmp's breaking changes enumeration](https://github.com/siom79/japicmp/blob/68425b08dd7835a4e9c0e64c6f6eaf3bd7281069/japicmp/src/main/java/japicmp/model/JApiCompatibilityChange.java), 
and [Revapi's list of API Differences](https://revapi.org/revapi-java/0.28.1/differences.html).
This approach ensures that both binary and source incompatibilities are identified and brought to the fore.

When the analysis is complete, Roseau generates a report detailing all the 
breaking changes detected between the two versions. Library developers can then fearlessly 
navigate the complex landscape of version upgrades.


## Usage

To use Roseau, follow these steps:

1. Clone the repository:

```
git clone https://github.com/labri-progress/roseau.git
```
2. Navigate to the project directory and build it using Maven:
```
mvn package
```
3. Run it with either of the following commands:
- **Option 1** : Using the provided shell script `roseau.sh`:
```
./roseau.sh /path/to/version1 /path/to/version2
```
- **Option 2** : Using the JAR file directly:
```
java -jar target/roseau-0.0.1-SNAPSHOT-jar-with-dependencies.jar /path/to/version1 /path/to/version2
```

In both cases, Roseau will analyze the provided library versions and generate a report 
containing all the detected breaking changes, along with the Java types & elements in which they occurred (methods, fields, 
constructors and types), their exact positions, and their nature (addition, deletion,
or mutation). This detailed report will be both displayed in the terminal and organized in a CSV file named
`breaking_changes_report.csv`.



## Tests
Roseau's efficiency was evaluated using Kamil Jezek and Jens Dietrich's [API evolution data corpus](https://github.com/kjezek/api-evolution-data-corpus), a benchmark 
renowned for assessing API checkers and comparing them. Building upon this established tool,
we introduced new features to enhance the evaluation process. In fact, we not only atomized all the breaking change
files to ensure a more granular evaluation, but we also incorporated an assessment of precision, recall, 
and performance.

With this [upgraded version of the benchmark](https://github.com/labri-progress/api-evolution-data-corpus), Roseau achieved a remarkable precision rate of 88.54% and a perfect recall rate of 100%. 
In other words, it successfully identified **every single** breaking change in the corpus.

## License
This repository—and all its content—is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).  („• ‿ •„) 






