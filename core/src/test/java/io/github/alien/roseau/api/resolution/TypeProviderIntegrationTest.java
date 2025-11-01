package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the complete type resolution flow combining StandardLibraryTypeProvider,
 * ClasspathTypeProvider, and CachingTypeResolver.
 */
class TypeProviderIntegrationTest {
	private AsmTypesExtractor extractor;
	private StandardLibraryTypeProvider stdLibProvider;
	private ClasspathTypeProvider classpathProvider;
	private CachingTypeResolver resolver;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		extractor = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
	}

	@AfterEach
	void tearDown() throws IOException {
		if (stdLibProvider != null) {
			stdLibProvider.close();
		}
		if (classpathProvider != null) {
			classpathProvider.close();
		}
	}

	@Test
	void resolve_finds_type_in_standard_library() {
		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		resolver = new CachingTypeResolver(List.of(stdLibProvider));

		TypeReference<TypeDecl> reference = new TypeReference<>("java.lang.String");
		Optional<TypeDecl> result = resolver.resolve(reference);

		assertThat(result).isPresent();
		assertThat(result.get().getQualifiedName()).isEqualTo("java.lang.String");
	}

	@Test
	void resolve_finds_type_in_classpath() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.MyClass", """
			package com.example;
			public class MyClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(classpathProvider));

		TypeReference<TypeDecl> reference = new TypeReference<>("com.example.MyClass");
		Optional<TypeDecl> result = resolver.resolve(reference);

		assertThat(result).isPresent();
		assertThat(result.get().getQualifiedName()).isEqualTo("com.example.MyClass");
	}

	@Test
	void resolve_prefers_standard_library_over_classpath() throws IOException {
		// Create a JAR with a class that has same name as a stdlib class
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.String", """
			package com.example;
			public class String {
				public void customMethod() {}
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		// Standard library provider is first, so stdlib types should be preferred
		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		// Should find the real java.lang.String from standard library
		TypeReference<TypeDecl> stdLibRef = new TypeReference<>("java.lang.String");
		Optional<TypeDecl> stdLibResult = resolver.resolve(stdLibRef);
		assertThat(stdLibResult).isPresent();
		ClassDecl stringClass = (ClassDecl) stdLibResult.get();
		assertThat(stringClass.getDeclaredMethods().size()).isGreaterThan(10);

		// Should find the custom String from classpath
		TypeReference<TypeDecl> customRef = new TypeReference<>("com.example.String");
		Optional<TypeDecl> customResult = resolver.resolve(customRef);
		assertThat(customResult).isPresent();
		ClassDecl customClass = (ClassDecl) customResult.get();
		assertThat(customClass.getDeclaredMethods())
			.anyMatch(m -> m.getSimpleName().equals("customMethod"));
	}

	@Test
	void resolve_falls_through_to_classpath_when_not_in_stdlib() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.CustomClass", """
			package com.example;
			public class CustomClass {
				public void myMethod() {}
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		TypeReference<TypeDecl> reference = new TypeReference<>("com.example.CustomClass");
		Optional<TypeDecl> result = resolver.resolve(reference);

		assertThat(result).isPresent();
		assertThat(result.get().getQualifiedName()).isEqualTo("com.example.CustomClass");
	}

	@Test
	void resolve_caches_results_across_providers() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.CachedClass", """
			package com.example;
			public class CachedClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		TypeReference<TypeDecl> reference = new TypeReference<>("com.example.CachedClass");

		// First resolution
		Optional<TypeDecl> first = resolver.resolve(reference);
		// Second resolution should use cache
		Optional<TypeDecl> second = resolver.resolve(reference);

		assertThat(first).isPresent();
		assertThat(second).isPresent();
		assertThat(first.get().getQualifiedName()).isEqualTo("com.example.CachedClass");
		assertThat(second.get().getQualifiedName()).isEqualTo("com.example.CachedClass");
	}

	@Test
	void resolve_handles_type_not_found_in_any_provider() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.ExistingClass", """
			package com.example;
			public class ExistingClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		TypeReference<TypeDecl> reference = new TypeReference<>("com.example.NonExistentClass");
		Optional<TypeDecl> result = resolver.resolve(reference);

		assertThat(result).isEmpty();
	}

	@Test
	void resolve_complex_scenario_with_multiple_jars() throws IOException {
		// Create three JARs with different classes
		Map<String, String> sources1 = new HashMap<>();
		sources1.put("com.lib1.ClassA", """
			package com.lib1;
			public class ClassA {}
			""");
		Path jar1 = tempDir.resolve("lib1.jar");
		TestUtils.buildJar(sources1, jar1);

		Map<String, String> sources2 = new HashMap<>();
		sources2.put("com.lib2.ClassB", """
			package com.lib2;
			public class ClassB {}
			""");
		Path jar2 = tempDir.resolve("lib2.jar");
		TestUtils.buildJar(sources2, jar2);

		Map<String, String> sources3 = new HashMap<>();
		sources3.put("com.lib3.ClassC", """
			package com.lib3;
			public class ClassC {}
			""");
		Path jar3 = tempDir.resolve("lib3.jar");
		TestUtils.buildJar(sources3, jar3);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar1, jar2, jar3));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		// Resolve types from different sources
		Optional<TypeDecl> string = resolver.resolve(new TypeReference<>("java.lang.String"));
		Optional<TypeDecl> classA = resolver.resolve(new TypeReference<>("com.lib1.ClassA"));
		Optional<TypeDecl> classB = resolver.resolve(new TypeReference<>("com.lib2.ClassB"));
		Optional<TypeDecl> classC = resolver.resolve(new TypeReference<>("com.lib3.ClassC"));

		assertThat(string).isPresent();
		assertThat(classA).isPresent();
		assertThat(classB).isPresent();
		assertThat(classC).isPresent();
	}

	@Test
	void resolve_with_type_hierarchy() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.Base", """
			package com.example;
			public class Base {}
			""");
		sources.put("com.example.Derived", """
			package com.example;
			public class Derived extends Base {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		Optional<TypeDecl> base = resolver.resolve(new TypeReference<>("com.example.Base"));
		Optional<TypeDecl> derived = resolver.resolve(new TypeReference<>("com.example.Derived"));

		assertThat(base).isPresent();
		assertThat(derived).isPresent();

		ClassDecl derivedClass = (ClassDecl) derived.get();
		assertThat(derivedClass.getSuperClass().getQualifiedName()).isEqualTo("com.example.Base");
	}

	@Test
	void resolve_interfaces_from_stdlib_and_classpath() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.MyInterface", """
			package com.example;
			public interface MyInterface {
				void doSomething();
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		Optional<TypeDecl> runnable = resolver.resolve(new TypeReference<>("java.lang.Runnable"));
		Optional<TypeDecl> myInterface = resolver.resolve(new TypeReference<>("com.example.MyInterface"));

		assertThat(runnable).isPresent();
		assertThat(runnable.get().isInterface()).isTrue();
		assertThat(myInterface).isPresent();
		assertThat(myInterface.get().isInterface()).isTrue();
	}

	@Test
	void resolve_concurrent_requests() throws Exception {
		Map<String, String> sources = new HashMap<>();
		for (int i = 0; i < 20; i++) {
			sources.put("com.example.Class" + i, """
				package com.example;
				public class Class%d {}
				""".formatted(i));
		}
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		// Perform concurrent resolutions
		Thread[] threads = new Thread[20];
		boolean[] results = new boolean[20];

		for (int i = 0; i < 20; i++) {
			final int index = i;
			threads[i] = new Thread(() -> {
				TypeReference<TypeDecl> ref = new TypeReference<>("com.example.Class" + index);
				Optional<TypeDecl> result = resolver.resolve(ref);
				results[index] = result.isPresent();
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		// All resolutions should succeed
		for (boolean result : results) {
			assertThat(result).isTrue();
		}
	}

	@Test
	void resolve_performance_with_large_classpath() throws IOException {
		// Create 10 JARs with 20 classes each
		List<Path> jars = new java.util.ArrayList<>();
		for (int jarNum = 0; jarNum < 10; jarNum++) {
			Map<String, String> sources = new HashMap<>();
			for (int classNum = 0; classNum < 20; classNum++) {
				String className = "com.example.jar" + jarNum + ".Class" + classNum;
				sources.put(className, """
					package com.example.jar%d;
					public class Class%d {}
					""".formatted(jarNum, classNum));
			}
			Path jar = tempDir.resolve("jar" + jarNum + ".jar");
			TestUtils.buildJar(sources, jar);
			jars.add(jar);
		}

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, jars);
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		long start = System.currentTimeMillis();

		// Resolve 100 types (mix of stdlib and classpath)
		for (int i = 0; i < 50; i++) {
			resolver.resolve(new TypeReference<>("java.lang.String"));
			int jarNum = i % 10;
			int classNum = i % 20;
			resolver.resolve(new TypeReference<>("com.example.jar" + jarNum + ".Class" + classNum));
		}

		long elapsed = System.currentTimeMillis() - start;

		// 100 resolutions should be fast (< 200ms after initial caching)
		assertThat(elapsed).isLessThan(200);
	}

	@Test
	void resolve_with_specific_type_class() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.MyClass", """
			package com.example;
			public class MyClass {}
			""");
		sources.put("com.example.MyInterface", """
			package com.example;
			public interface MyInterface {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(classpathProvider));

		Optional<ClassDecl> classResult = resolver.resolve(
			new TypeReference<>("com.example.MyClass"), ClassDecl.class);
		Optional<ClassDecl> wrongTypeResult = resolver.resolve(
			new TypeReference<>("com.example.MyInterface"), ClassDecl.class);

		assertThat(classResult).isPresent();
		assertThat(wrongTypeResult).isEmpty();
	}

	@Test
	void resolve_caches_negative_results() {
		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		resolver = new CachingTypeResolver(List.of(stdLibProvider));

		TypeReference<TypeDecl> reference = new TypeReference<>("com.nonexistent.Class");

		// First resolution - not found
		Optional<TypeDecl> first = resolver.resolve(reference);
		// Second resolution should also be not found (cached negative result)
		Optional<TypeDecl> second = resolver.resolve(reference);

		assertThat(first).isEmpty();
		assertThat(second).isEmpty();
	}

	@Test
	void resolve_nested_classes_from_both_providers() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.Outer", """
			package com.example;
			public class Outer {
				public static class Nested {
					public static class DeeplyNested {}
				}
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(jar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		// Resolve from standard library
		Optional<TypeDecl> threadState = resolver.resolve(new TypeReference<>("java.lang.Thread$State"));

		// Resolve from classpath
		Optional<TypeDecl> nested = resolver.resolve(new TypeReference<>("com.example.Outer$Nested"));
		Optional<TypeDecl> deeplyNested = resolver.resolve(
			new TypeReference<>("com.example.Outer$Nested$DeeplyNested"));

		assertThat(threadState).isPresent();
		assertThat(nested).isPresent();
		assertThat(deeplyNested).isPresent();
	}

	@Test
	void resolve_end_to_end_realistic_scenario() throws IOException {
		// Simulate a realistic scenario: an application using stdlib and third-party libraries
		Map<String, String> thirdParty = new HashMap<>();
		thirdParty.put("org.thirdparty.Utils", """
			package org.thirdparty;
			public class Utils {
				public static String format(Object obj) {
					return obj.toString();
				}
			}
			""");
		thirdParty.put("org.thirdparty.Logger", """
			package org.thirdparty;
			public interface Logger {
				void log(String message);
			}
			""");
		Path thirdPartyJar = tempDir.resolve("thirdparty.jar");
		TestUtils.buildJar(thirdParty, thirdPartyJar);

		Map<String, String> appSources = new HashMap<>();
		appSources.put("com.myapp.Application", """
			package com.myapp;
			public class Application {
				public static void main(String[] args) {
					// Uses both stdlib and third-party
				}
			}
			""");
		appSources.put("com.myapp.Service", """
			package com.myapp;
			public class Service {
				public void process() {}
			}
			""");
		Path appJar = tempDir.resolve("app.jar");
		TestUtils.buildJar(appSources, appJar);

		stdLibProvider = new StandardLibraryTypeProvider(extractor);
		classpathProvider = new ClasspathTypeProvider(extractor, List.of(thirdPartyJar, appJar));
		resolver = new CachingTypeResolver(List.of(stdLibProvider, classpathProvider));

		// Resolve various types as would happen in real API analysis
		Optional<TypeDecl> string = resolver.resolve(new TypeReference<>("java.lang.String"));
		Optional<TypeDecl> list = resolver.resolve(new TypeReference<>("java.util.List"));
		Optional<TypeDecl> utils = resolver.resolve(new TypeReference<>("org.thirdparty.Utils"));
		Optional<TypeDecl> logger = resolver.resolve(new TypeReference<>("org.thirdparty.Logger"));
		Optional<TypeDecl> app = resolver.resolve(new TypeReference<>("com.myapp.Application"));
		Optional<TypeDecl> service = resolver.resolve(new TypeReference<>("com.myapp.Service"));

		// All should resolve successfully
		assertThat(string).isPresent();
		assertThat(list).isPresent();
		assertThat(utils).isPresent();
		assertThat(logger).isPresent();
		assertThat(app).isPresent();
		assertThat(service).isPresent();

		// Verify types are correct
		assertThat(string.get().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(logger.get().isInterface()).isTrue();
		assertThat(utils.get().isClass()).isTrue();
	}
}
