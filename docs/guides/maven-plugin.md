# Maven Plug-in

The Maven plug-in runs Roseau during `verify` and compares the current artifact against a baseline.

This guide covers setup and the most common configurations. The exhaustive parameter list lives in [Maven Plug-in Options](../reference/maven-plugin.md).

## Minimal Setup

Add the plug-in to `pom.xml` and provide a baseline:

```xml
<plugin>
  <groupId>io.github.alien-tools</groupId>
  <artifactId>roseau-maven-plugin</artifactId>
  <version>&lt;version&gt;</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <baselineJar>${project.basedir}/old.jar</baselineJar>
  </configuration>
</plugin>
```

Then run:

```bash
mvn verify
```

The plug-in runs during the `verify` phase. It compares the JAR produced by the current build against the baseline and prints any breaking changes to the Maven log.

## Choose a Baseline

The baseline can be a local JAR file or Maven coordinates resolved from repositories.

**Local JAR file**

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
</configuration>
```

**Maven coordinates**

```xml
<configuration>
  <baselineVersion>
    <groupId>com.example</groupId>
    <artifactId>my-library</artifactId>
    <version>1.0.0</version>
  </baselineVersion>
</configuration>
```

!!! note
    When both `baselineJar` and `baselineVersion` are provided, `baselineVersion` takes precedence.

## Fail the Build on Breaking Changes

By default, the plug-in reports breaking changes but does not fail the build.

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <!-- Fail on any breaking change -->
  <failOnIncompatibility>true</failOnIncompatibility>
</configuration>
```

For finer control:

| Option | Fails when |
| --- | --- |
| `failOnIncompatibility` | any breaking change is found |
| `failOnBinaryIncompatibility` | a binary-breaking change is found |
| `failOnSourceIncompatibility` | a source-breaking change is found |

## Filter by Compatibility Kind

Report only binary-breaking or only source-breaking changes:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <binaryOnly>true</binaryOnly>
</configuration>
```

Or:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <sourceOnly>true</sourceOnly>
</configuration>
```

## Generate Reports

Report files are written under `reportDirectory`, which defaults to `${project.build.directory}/roseau`.

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <reports>
    <report>
      <file>report.csv</file>
      <format>CSV</format>
    </report>
    <report>
      <file>report.html</file>
      <format>HTML</format>
    </report>
  </reports>
</configuration>
```

Available formats: `CSV`, `HTML`, `JSON`, `MD`.

## Provide a Classpath or YAML Config

The plug-in automatically includes the current project's compile-scope dependencies in the classpath.

To add extra entries shared by both baseline and current artifact:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <classpath>
    <path>/path/to/extra-dependency.jar</path>
  </classpath>
</configuration>
```

To add entries only for the baseline:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <baselineClasspath>
    <path>/path/to/old-dependency.jar</path>
  </baselineClasspath>
</configuration>
```

You can also use a POM file to derive classpath entries:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <classpathPom>/path/to/pom.xml</classpathPom>
</configuration>
```

Supply a `roseau.yaml` file when the same exclusions or report definitions already exist for the CLI:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <configFile>roseau.yaml</configFile>
</configuration>
```

Maven parameters override YAML values.

## Export API Models

Export the API models as JSON for archival or further analysis:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <exportBaselineApi>${project.build.directory}/roseau/baseline-api.json</exportBaselineApi>
  <exportCurrentApi>${project.build.directory}/roseau/current-api.json</exportCurrentApi>
</configuration>
```

## Skip Execution

Skip the plug-in from the command line:

```bash
mvn verify -Droseau.skip=true
```

Or in the POM:

```xml
<configuration>
  <skip>true</skip>
</configuration>
```

!!! note
    The plug-in automatically skips projects with `pom` packaging.
