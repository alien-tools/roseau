package com.github.maracas.roseau.smoke;

import com.github.maracas.roseau.extractors.jar.AsmAPIExtractor;
import com.github.maracas.roseau.extractors.sources.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.extractors.sources.SpoonUtils;
import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import spoon.reflect.CtModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PopularLibrariesTestIT {

	static Stream<String> libraries() {
		return Stream.of(
			"org.assertj:assertj-core:3.27.3",
			"commons-codec:commons-codec:1.18.0",
			"com.google.guava:guava:32.1.3-jre",
			"org.apache.commons:commons-lang3:3.17.0",
			"commons-io:commons-io:2.18.0",
			"org.eclipse.collections:eclipse-collections-api:11.1.0",
			//"org.springframework:spring-core:6.1.5",
			"io.dropwizard:dropwizard-core:4.0.1",
			"io.projectreactor:reactor-core:3.6.3",
			"org.reactivestreams:reactive-streams:1.0.4",
			"org.apache.kafka:kafka-clients:3.6.0",
			"com.google.code.gson:gson:2.10.1",
			"org.junit.jupiter:junit-jupiter-api:5.10.1",
			"com.squareup:javapoet:1.13.0",
			"org.jooq:joor-java-8:0.9.15",
			"joda-time:joda-time:2.12.5",
			"com.google.auto.service:auto-service:1.1.1",
			"com.google.dagger:dagger:2.55",
			"ch.qos.logback:logback-core:1.5.16",
			//"ch.qos.logback:logback-classic:1.5.16",
			//"org.apache.logging.log4j:log4j-core:2.24.3", // external libs
			"org.apache.logging.log4j:log4j-api:2.24.3",
			"org.slf4j:slf4j-simple:2.0.16",
			"org.slf4j:slf4j-api:2.0.16",
			//"com.fasterxml.jackson.core:jackson-core:2.18.2", // shaded
			"org.apache.httpcomponents.client5:httpclient5:5.4.2",
			//"fr.inria.gforge.spoon:spoon-core:11.2.0",
			"commons-logging:commons-logging:1.3.5",
			//"org.springframework:spring-web:6.2.2",
			"com.h2database:h2:2.3.232",
			"org.hamcrest:hamcrest:3.0",
			//"org.springframework:spring-beans:6.2.2",
			"org.osgi:org.osgi.core:6.0.0",
			//"com.alibaba:fastjson:2.0.54",
			"commons-collections:commons-collections:3.2.2",
			"org.json:json:20250107",
			"commons-beanutils:commons-beanutils:1.10.0"
			//"com.squareup.retrofit2:retrofit:2.11.0"
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("libraries")
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	void analyzeLibrary(String libraryGAV) {
		String[] parts = libraryGAV.split(":");
		String groupId = parts[0];
		String artifactId = parts[1];
		String version = parts[2];

		try {
			Path binaryJar = downloadBinaryJar(groupId, artifactId, version);
			Path sourcesJar = downloadSourcesJar(groupId, artifactId, version);
			Path sourcesDir = extractSourcesJar(sourcesJar);

			// JAR API
			Stopwatch sw = Stopwatch.createStarted();
			AsmAPIExtractor jarExtractor = new AsmAPIExtractor();
			API jarApi = jarExtractor.extractAPI(binaryJar);
			long jarApiTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			// Sources parsing
			CtModel model = SpoonUtils.buildModel(sourcesDir, Duration.ofMinutes(1));
			long parsingTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			// Sources API
			SpoonAPIExtractor sourcesExtractor = new SpoonAPIExtractor();
			API sourcesApi = sourcesExtractor.extractAPI(model);
			long sourcesApiTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			APIDiff jarToSourcesDiff = new APIDiff(jarApi, sourcesApi);
			List<BreakingChange> jarToSourcesBCs = jarToSourcesDiff.diff();
			APIDiff sourcesToJarDiff = new APIDiff(sourcesApi, jarApi);
			List<BreakingChange> sourcesToJarBCs = sourcesToJarDiff.diff();
			long diffTime = sw.elapsed().toMillis();

			// Stats
			long loc = countLinesOfCode(sourcesDir);
			long numTypes = sourcesApi.getAllTypes().count();
			int numMethods = sourcesApi.getAllTypes()
				.mapToInt(type -> type.getDeclaredMethods().size())
				.sum();
			int numFields = sourcesApi.getAllTypes()
				.mapToInt(type -> type.getDeclaredFields().size())
				.sum();

			System.out.printf("Processed %s (%d LoC, %d types, %d methods, %d fields)%n" +
					"\tParsing: %dms API: %sms Diff: %dms%n" +
					"\tJAR API: %dms%n" +
					"\tJAR to Sources BCs: %d%n" +
					"\tSources to JAR BCs: %d%n",
				libraryGAV, loc, numTypes, numMethods, numFields, parsingTime, sourcesApiTime, diffTime, jarApiTime,
				jarToSourcesBCs.size(), sourcesToJarBCs.size());

			System.out.println("JAR to Sources API diff:");
			diffAPIs(jarApi, sourcesApi);
			System.out.println("Sources to JAR API diff:");
			diffAPIs(sourcesApi, jarApi);

			if (!jarToSourcesBCs.isEmpty() || !sourcesToJarBCs.isEmpty()) {
				System.out.println("JAR to Sources BCs:");
				System.out.println(jarToSourcesBCs.stream()
					.map(BreakingChange::toString).collect(Collectors.joining("\n")));
				System.out.println("Sources to JAR BCs:");
				System.out.println(sourcesToJarBCs.stream()
					.map(BreakingChange::toString).collect(Collectors.joining("\n")));
			}

			cleanup(sourcesJar);
			cleanup(binaryJar);
			cleanup(sourcesDir);

			// Check everything went well
			assertFalse(sourcesApi.getAllTypes().findAny().isEmpty());
			assertFalse(jarApi.getAllTypes().findAny().isEmpty());
			assertTrue(jarToSourcesBCs.isEmpty());
			assertTrue(sourcesToJarBCs.isEmpty());
		} catch (Exception e) {
			fail("Failed to process " + libraryGAV, e);
		}
	}

	private Path downloadSourcesJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-sources.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private Path downloadBinaryJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private Path download(String url) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.build();

		Path tempFile = Files.createTempFile("sources-", ".jar");
		HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

		if (response.statusCode() != 200) {
			Files.deleteIfExists(tempFile);
			throw new IOException("Failed to download JAR: HTTP " + response.statusCode());
		}

		return tempFile;
	}

	private Path extractSourcesJar(Path jarPath) throws IOException {
		Path outputDir = Files.createTempDirectory("sources-");
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				Path entryPath = outputDir.resolve(entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(entryPath);
				} else {
					Files.createDirectories(entryPath.getParent());
					try (InputStream is = jar.getInputStream(entry)) {
						Files.copy(is, entryPath);
					}
				}
			}
		}
		return outputDir;
	}

	private long countLinesOfCode(Path sourcesDir) throws IOException {
		return Files.walk(sourcesDir)
			.filter(path -> path.toString().endsWith(".java"))
			.mapToLong(path -> {
				try (BufferedReader reader = Files.newBufferedReader(path)) {
					return reader.lines().count();
				} catch (IOException e) {
					System.err.println("Failed to count lines in " + path + ": " + e.getMessage());
					return 0;
				}
			})
			.sum();
	}

	private void cleanup(Path path) throws IOException {
		if (path.toFile().isDirectory())
			deleteDirectory(path);
		else
			Files.deleteIfExists(path);
	}

	private void deleteDirectory(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.walk(path)
				.sorted(Comparator.reverseOrder())
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						System.err.println("Failed to delete " + p + ": " + e.getMessage());
					}
				});
		}
	}

	private static boolean diffAPIs(API api1, API api2) {
		boolean equal = true;

		for (TypeDecl type1 : api1.getAllTypes().toList()) {
			Optional<TypeDecl> type2 = api2.findType(type1.getQualifiedName());

			if (type2.isEmpty()) {
				System.out.printf("%s %s is missing in the second API%n",
					type1.getClass().getSimpleName(), type1.getQualifiedName());
				equal = false;
			} else {
				if (!diffTypeDecl(type1, type2.get())) {
					equal = false;
				}
			}
		}

		return equal;
	}

	private static boolean unsortedListsMatch(List<?> list1, List<?> list2) {
		return new HashSet<>(list1).equals(new HashSet<>(list2));
	}

	private static boolean diffTypeDecl(TypeDecl type1, TypeDecl type2) {
		boolean equal = true;
		String typeName = type1.getClass().getSimpleName() + " " + type1.getQualifiedName();

		if (!type1.getQualifiedName().equals(type2.getQualifiedName())) {
			System.out.printf("Type name mismatch: %s vs %s%n", type1.getQualifiedName(), type2.getQualifiedName());
			equal = false;
		}

		if (!type1.getSimpleName().equals(type2.getSimpleName())) {
			System.out.printf("Type name mismatch: %s vs %s%n", type1.getSimpleName(), type2.getSimpleName());
			equal = false;
		}

		if (type1.getVisibility() != type2.getVisibility()) {
			System.out.printf("Visibility mismatch for %s: %s vs %s%n", typeName, type1.getVisibility(), type2.getVisibility());
			equal = false;
		}

		if (!type1.getModifiers().equals(type2.getModifiers())) {
			System.out.printf("Modifiers mismatch for %s: %s vs %s%n", typeName, type1.getModifiers(), type2.getModifiers());
			equal = false;
		}

		if (!unsortedListsMatch(type1.getImplementedInterfaces(), type2.getImplementedInterfaces())) {
			equal = false;
			System.out.printf("Implemented interfaces mismatch for %s: %s vs %s%n", typeName, type1.getImplementedInterfaces(), type2.getImplementedInterfaces());
		}

		if (!diffFields(type1.getDeclaredFields(), type2.getDeclaredFields())) {
			equal = false;
		}

		if (!diffMethods(type1.getDeclaredMethods(), type2.getDeclaredMethods())) {
			equal = false;
		}

		if (!type1.getEnclosingType().equals(type2.getEnclosingType())) {
			System.out.printf("Enclosing type mismatch for type %s%n", type1.getQualifiedName());
			equal = false;
		}

		if (!type1.getFormalTypeParameters().equals(type2.getFormalTypeParameters())) {
			System.out.printf("Formal type parameters mismatch for type %s: %s vs %s%n",
				type1.getQualifiedName(), type1.getFormalTypeParameters(), type2.getFormalTypeParameters());
		}

		if (type1 instanceof ClassDecl class1 && type2 instanceof ClassDecl class2) {
			if (!diffConstructors(class1.getConstructors(), class2.getConstructors())) {
				equal = false;
			}

			if (!class1.getSuperClass().equals(class2.getSuperClass())) {
				System.out.printf("Super class mismatch for %s: %s vs %s%n", type1.getQualifiedName(), class1.getSuperClass(), class2.getSuperClass());
				equal = false;
			}
		}

		return equal;
	}

	private static boolean diffFields(List<FieldDecl> fields1, List<FieldDecl> fields2) {
		boolean equal = true;
		Map<String, FieldDecl> fieldMap1 = fields1.stream().collect(Collectors.toMap(FieldDecl::getSimpleName, f -> f));
		Map<String, FieldDecl> fieldMap2 = fields2.stream().collect(Collectors.toMap(FieldDecl::getSimpleName, f -> f));

		for (String fieldName : fieldMap1.keySet()) {
			FieldDecl field1 = fieldMap1.get(fieldName);

			if (!fieldMap2.containsKey(fieldName)) {
				System.out.printf("Field %s is missing in the second API%n", field1.getQualifiedName());
				equal = false;
				break;
			}

			FieldDecl field2 = fieldMap2.get(fieldName);

			if (!field1.getType().equals(field2.getType())) {
				System.out.printf("Field type mismatch for %s: %s vs %s%n", field1.getQualifiedName(), field1.getType().getQualifiedName(), field2.getType().getQualifiedName());
				equal = false;
			}

			if (field1.getVisibility() != field2.getVisibility()) {
				System.out.printf("Field visibility mismatch for %s: %s vs %s%n", field1.getQualifiedName(), field1.getVisibility(), field2.getVisibility());
				equal = false;
			}

			if (!field1.getModifiers().equals(field2.getModifiers())) {
				System.out.printf("Field modifiers mismatch for %s: %s vs %s%n", field1.getQualifiedName(), field1.getModifiers(), field2.getModifiers());
				equal = false;
			}
		}

		return equal;
	}

	private static boolean diffMethods(List<MethodDecl> methods1, List<MethodDecl> methods2) {
		boolean equal = true;
		Map<String, MethodDecl> methodMap1 = methods1.stream().collect(Collectors.toMap(MethodDecl::getErasure, m -> m));
		Map<String, MethodDecl> methodMap2 = methods2.stream().collect(Collectors.toMap(MethodDecl::getErasure, m -> m));

		for (String methodErasure : methodMap1.keySet()) {
			MethodDecl method1 = methodMap1.get(methodErasure);
			String methodFqn = method1.getContainingType().getQualifiedName() + "#" + method1.getSignature();

			if (!methodMap2.containsKey(methodErasure)) {
				System.out.printf("Method %s is missing in the second API%n", methodFqn);
				equal = false;
				break;
			}

			MethodDecl method2 = methodMap2.get(methodErasure);

			if (!method1.getType().getQualifiedName().equals(method2.getType().getQualifiedName())) {
				System.out.printf("Method return type mismatch for %s: %s vs %s%n", methodFqn, method1.getType().getQualifiedName(), method2.getType().getQualifiedName());
				equal = false;
			}

			if (method1.getVisibility() != method2.getVisibility()) {
				System.out.printf("Method visibility mismatch for %s: %s vs %s%n", methodFqn, method1.getVisibility(), method2.getVisibility());
				equal = false;
			}

			if (!method1.getModifiers().equals(method2.getModifiers())) {
				System.out.printf("Method modifiers mismatch for %s: %s vs %s%n", methodFqn, method1.getModifiers(), method2.getModifiers());
				equal = false;
			}

			if (!diffParameters(method1.getParameters(), method2.getParameters())) {
				equal = false;
			}

			if (!method1.getFormalTypeParameters().equals(method2.getFormalTypeParameters())) {
				System.out.printf("Formal type parameters mismatch for method %s: %s vs %s%n",
					method1.getQualifiedName(), method1.getFormalTypeParameters(), method2.getFormalTypeParameters());
			}

			if (!unsortedListsMatch(method1.getThrownExceptions(), method2.getThrownExceptions())) {
				System.out.printf("Thrown exceptions mismatch for method %s: %s vs %s%n",
					method1.getQualifiedName(), method1.getThrownExceptions(), method2.getThrownExceptions());
			}
		}

		return equal;
	}

	private static boolean diffConstructors(List<ConstructorDecl> constructors1, List<ConstructorDecl> constructors2) {
		boolean equal = true;
		Map<String, ConstructorDecl> constructorMap1 = constructors1.stream().collect(Collectors.toMap(ConstructorDecl::getErasure, c -> c));
		Map<String, ConstructorDecl> constructorMap2 = constructors2.stream().collect(Collectors.toMap(ConstructorDecl::getErasure, c -> c));

		for (String constructorErasure : constructorMap1.keySet()) {
			ConstructorDecl constructor1 = constructorMap1.get(constructorErasure);
			String consFqn = constructor1.getContainingType().getQualifiedName() + "#" + constructor1.getSignature();

			if (!constructorMap2.containsKey(constructorErasure)) {
				System.out.printf("Constructor %s is missing in the second API%n", consFqn);
				equal = false;
				break;
			}

			ConstructorDecl constructor2 = constructorMap2.get(constructorErasure);

			if (constructor1.getVisibility() != constructor2.getVisibility()) {
				System.out.printf("Constructor visibility mismatch for %s: %s vs %s%n", consFqn, constructor1.getVisibility(), constructor2.getVisibility());
				equal = false;
			}

			if (!constructor1.getModifiers().equals(constructor2.getModifiers())) {
				System.out.printf("Constructor modifiers mismatch for %s: %s vs %s%n", consFqn, constructor1.getModifiers(), constructor2.getModifiers());
				equal = false;
			}

			if (!diffParameters(constructor1.getParameters(), constructor2.getParameters())) {
				equal = false;
			}

			if (!constructor1.getFormalTypeParameters().equals(constructor2.getFormalTypeParameters())) {
				System.out.printf("Formal type parameters mismatch for constructor %s: %s vs %s%n",
					constructor1.getQualifiedName(), constructor1.getFormalTypeParameters(), constructor2.getFormalTypeParameters());
			}

			if (!unsortedListsMatch(constructor1.getThrownExceptions(), constructor2.getThrownExceptions())) {
				System.out.printf("Thrown exceptions mismatch for method %s: %s vs %s%n",
					constructor1.getQualifiedName(), constructor1.getThrownExceptions(), constructor2.getThrownExceptions());
			}
		}

		return equal;
	}

	private static boolean diffParameters(List<ParameterDecl> parameters1, List<ParameterDecl> parameters2) {
		boolean equal = true;
		for (int i = 0; i < parameters1.size(); i++) {
			ParameterDecl param1 = parameters1.get(i);
			ParameterDecl param2 = parameters2.get(i);

			if (!param1.type().equals(param2.type())) {
				System.out.printf("Parameter type mismatch: %s vs %s%n", param1.type().getQualifiedName(), param2.type().getQualifiedName());
				equal = false;
			}

			if (param1.isVarargs() != param2.isVarargs()) {
				System.out.printf("Parameter varargs mismatch: %s vs %s%n", param1.isVarargs(), param2.isVarargs());
				equal = false;
			}
		}

		return equal;
	}
}
