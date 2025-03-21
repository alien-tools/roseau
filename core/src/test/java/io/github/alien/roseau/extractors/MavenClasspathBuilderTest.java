package io.github.alien.roseau.extractors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenClasspathBuilderTest {
	@TempDir
	Path wd;
	Path validPom;
	Path invalidPom;

	@BeforeEach
	void setUp() throws IOException {
		validPom = wd.resolve("valid-pom.xml");
		invalidPom = wd.resolve("invalid-pom.xml");
		Files.writeString(validPom, """
			<?xml version="1.0" encoding="UTF-8"?>
			<project xmlns="http://maven.apache.org/POM/4.0.0"
			         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
			    <modelVersion>4.0.0</modelVersion>
			    <groupId>org.example</groupId>
			    <artifactId>example</artifactId>
			    <version>1.0</version>
			    <dependencies>
			        <dependency>
			            <groupId>org.apache.commons</groupId>
			            <artifactId>commons-lang3</artifactId>
			            <version>3.12.0</version>
			        </dependency>
			        <dependency>
			            <groupId>com.google.guava</groupId>
			            <artifactId>guava</artifactId>
			            <version>31.1-jre</version>
			            <scope>test</scope>
			        </dependency>
			    </dependencies>
			</project>""");
		Files.writeString(invalidPom, "<nope>");
	}

	@Test
	void unknown_file() {
		var builder = new MavenClasspathBuilder();
		var path = Path.of("unknown");
		assertThrows(IllegalArgumentException.class, () -> builder.buildClasspath(path));
	}

	@Test
	void valid_pom() {
		var builder = new MavenClasspathBuilder();
		var cp = builder.buildClasspath(validPom);
		assertThat(cp.stream().map(Path::toString).toList(), hasItems(
			endsWith("org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar"),
			endsWith("com/google/guava/guava/31.1-jre/guava-31.1-jre.jar")));
	}

	@Test
	void invalid_pom() {
		var builder = new MavenClasspathBuilder();
		var cp = builder.buildClasspath(invalidPom);
		assertThat(cp, is(empty()));
	}
}