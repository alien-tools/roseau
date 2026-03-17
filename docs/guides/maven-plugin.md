# Maven plug-in

Roseau's Maven plug-in runs during the `verify` phase and compares the currently built artifact against a user-defined baseline.
It can also be invoked directly after the artifact is built: `mvn package roseau:check`.
This guide covers setup and the most common configurations. The exhaustive parameter list lives in [Maven plug-in options](../reference/maven-plugin.md).

## Minimal Setup

Find the latest released version on [Maven Central](https://central.sonatype.com/artifact/io.github.alien-tools/roseau-maven-plugin). Then add the plug-in to `pom.xml` and provide a baseline:

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
    <baselineDependency>
      <groupId>com.example.project</groupId>
      <artifactId>example-core</artifactId>
      <version>2.1.4</version>
    </baselineDependency>
  </configuration>
</plugin>
```

Then run:

```bash
mvn verify
```

The plug-in runs during the `verify` phase. It compares the JAR produced by the current build against the baseline and prints any breaking changes to the Maven log.

!!! note
    The plug-in automatically skips projects with `pom` packaging.

## Choose a Baseline

The baseline can be a local JAR file or Maven coordinates resolved from remote repositories.

**Maven coordinates**

Use `baselineCoordinates` to pass coordinates as a single string. This is the recommended form for CI/CD pipelines where the baseline version changes between releases and should not be hard-coded in the POM.

```xml
<configuration>
  <baselineCoordinates>com.example.project:example-core:2.1.4</baselineCoordinates>
</configuration>
```

It can also be passed on the command line without modifying the POM:

```bash
mvn verify -Droseau.baselineCoordinates=com.example.project:example-core:2.1.4
```

Use `baselineDependency` for structured XML configuration:

```xml
<configuration>
  <baselineDependency>
    <groupId>com.example.project</groupId>
    <artifactId>example-core</artifactId>
    <version>2.1.4</version>
  </baselineDependency>
</configuration>
```

**Local JAR file**

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
</configuration>
```

When multiple baseline parameters are set, `baselineCoordinates` takes precedence over `baselineDependency`, which takes precedence over `baselineJar`.

## Multi-module projects

In a multi-module build, the plug-in configuration can be placed once in the parent POM and reused across all modules.

The key is that Maven interpolates `${project.groupId}` and `${project.artifactId}` at the level of each module, not the parent. Combined with a shared `previous.version` property, a single configuration covers every module automatically.
For instance, using `baselineDependency`:

```xml
<!-- Parent POM -->
<properties>
  <previous.version>1.2.3</previous.version>
</properties>

<build>
  <plugins>
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
        <baselineDependency>
          <groupId>${project.groupId}</groupId>
          <artifactId>${project.artifactId}</artifactId>
          <version>${previous.version}</version>
        </baselineDependency>
      </configuration>
    </plugin>
  </plugins>
</build>
```
New modules that have no previously published version will fail to resolve the baseline. Exclude them individually with `roseau.skip`:

```xml
<!-- In the new module's POM -->
<properties>
  <roseau.skip>true</roseau.skip>
</properties>
```

## Fail on incompatible changes

By default, the plug-in reports breaking changes as Maven warnings (`[WARN]`) but does not fail the build.

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <!-- Fail the build on any breaking change -->
  <failOnIncompatibility>true</failOnIncompatibility>
  <!-- Fail the build on binary-breaking changes -->
  <failOnBinaryIncompatibility>true</failOnBinaryIncompatibility>
  <!-- Fail the build on source-breaking changes -->
  <failOnSourceIncompatibility>true</failOnSourceIncompatibility>
</configuration>
```

## Narrow the scope of reported breaking changes

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

## Write report artifacts

Report files are written under `reportDirectory`, which defaults to `${project.build.directory}/roseau`. See [Reports](reports.md) for the available formats.

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

## Classpath and YAML configuration

The plug-in automatically includes the current project's compile-scope dependencies in the analysis classpath.
To add extra entries:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <classpath>
    <path>/path/to/common-dependency.jar</path>
  </classpath>
  <baselineClasspath>
    <path>/path/to/old-dependency.jar</path>
  </baselineClasspath>
</configuration>
```

Instead, the Maven plug-in can reuse an existing configuration file that specifies classpaths, exclusion policies, etc. Note that Maven parameters override YAML values.

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <configFile>roseau.yaml</configFile>
</configuration>
```

## Export API models

Export the API models as JSON for archival or further analysis:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <exportBaselineApi>${project.build.directory}/roseau/baseline-api.json</exportBaselineApi>
  <exportCurrentApi>${project.build.directory}/roseau/current-api.json</exportCurrentApi>
</configuration>
```
