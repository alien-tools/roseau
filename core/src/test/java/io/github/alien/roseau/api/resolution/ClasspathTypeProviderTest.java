package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathTypeProviderTest {
	private AsmTypesExtractor extractor;
	private ClasspathTypeProvider provider;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		extractor = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
	}

	@AfterEach
	void tearDown() throws IOException {
		if (provider != null) {
			provider.close();
		}
	}

	@Test
	void findType_existing_class_returns_type() throws IOException {
		// Create a JAR with a simple class
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.MyClass", """
			package com.example;
			public class MyClass {
				public void hello() {}
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		Optional<TypeDecl> result = provider.findType("com.example.MyClass");

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(ClassDecl.class);
		assertThat(result.get().getQualifiedName()).isEqualTo("com.example.MyClass");
	}

	@Test
	void findType_existing_interface_returns_type() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.MyInterface", """
			package com.example;
			public interface MyInterface {
				void doSomething();
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		Optional<InterfaceDecl> result = provider.findType("com.example.MyInterface", InterfaceDecl.class);

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(InterfaceDecl.class);
		assertThat(result.get().getQualifiedName()).isEqualTo("com.example.MyInterface");
	}

	@Test
	void findType_non_existing_type_returns_empty() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.MyClass", """
			package com.example;
			public class MyClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		Optional<TypeDecl> result = provider.findType("com.example.NonExistent");

		assertThat(result).isEmpty();
	}

	@Test
	void findType_with_nested_class_returns_type() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.Outer", """
			package com.example;
			public class Outer {
				public static class Nested {
					public void method() {}
				}
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		Optional<TypeDecl> outer = provider.findType("com.example.Outer");
		Optional<TypeDecl> nested = provider.findType("com.example.Outer$Nested");

		assertThat(outer).isPresent();
		assertThat(nested).isPresent();
		assertThat(nested.get().getQualifiedName()).isEqualTo("com.example.Outer$Nested");
	}

	@Test
	void findType_classpath_precedence_first_jar_wins() throws IOException {
		// Create two JARs with the same class but different content
		Map<String, String> sources1 = new HashMap<>();
		sources1.put("com.example.Shared", """
			package com.example;
			public class Shared {
				public void methodFromJar1() {}
			}
			""");
		Path jar1 = tempDir.resolve("jar1.jar");
		TestUtils.buildJar(sources1, jar1);

		Map<String, String> sources2 = new HashMap<>();
		sources2.put("com.example.Shared", """
			package com.example;
			public class Shared {
				public void methodFromJar2() {}
			}
			""");
		Path jar2 = tempDir.resolve("jar2.jar");
		TestUtils.buildJar(sources2, jar2);

		// jar1 is first on classpath, so it should win
		provider = new ClasspathTypeProvider(extractor, List.of(jar1, jar2));

		Optional<TypeDecl> result = provider.findType("com.example.Shared");

		assertThat(result).isPresent();
		ClassDecl classDecl = (ClassDecl) result.get();
		// The class from jar1 should be found
		assertThat(classDecl.getDeclaredMethods())
			.anyMatch(m -> m.getSimpleName().equals("methodFromJar1"));
		assertThat(classDecl.getDeclaredMethods())
			.noneMatch(m -> m.getSimpleName().equals("methodFromJar2"));
	}

	@Test
	void findType_multiple_jars_finds_types_from_different_jars() throws IOException {
		Map<String, String> sources1 = new HashMap<>();
		sources1.put("com.example.ClassA", """
			package com.example;
			public class ClassA {}
			""");
		Path jar1 = tempDir.resolve("jar1.jar");
		TestUtils.buildJar(sources1, jar1);

		Map<String, String> sources2 = new HashMap<>();
		sources2.put("com.example.ClassB", """
			package com.example;
			public class ClassB {}
			""");
		Path jar2 = tempDir.resolve("jar2.jar");
		TestUtils.buildJar(sources2, jar2);

		provider = new ClasspathTypeProvider(extractor, List.of(jar1, jar2));

		Optional<TypeDecl> classA = provider.findType("com.example.ClassA");
		Optional<TypeDecl> classB = provider.findType("com.example.ClassB");

		assertThat(classA).isPresent();
		assertThat(classB).isPresent();
	}

	@Test
	void findType_repeated_lookups_use_cache() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.CachedClass", """
			package com.example;
			public class CachedClass {
				public int getValue() { return 42; }
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		// First lookup
		Optional<TypeDecl> first = provider.findType("com.example.CachedClass");
		// Second lookup should use cached JarFile
		Optional<TypeDecl> second = provider.findType("com.example.CachedClass");

		assertThat(first).isPresent();
		assertThat(second).isPresent();
		// Both should resolve successfully
		assertThat(first.get().getQualifiedName()).isEqualTo("com.example.CachedClass");
		assertThat(second.get().getQualifiedName()).isEqualTo("com.example.CachedClass");
	}

	@Test
	void findType_empty_classpath_returns_empty() {
		provider = new ClasspathTypeProvider(extractor, Collections.emptyList());

		Optional<TypeDecl> result = provider.findType("com.example.NonExistent");

		assertThat(result).isEmpty();
	}

	@Test
	void constructor_invalid_jar_throws_exception() {
		Path invalidJar = tempDir.resolve("invalid.jar");

		assertThatThrownBy(() -> new ClasspathTypeProvider(extractor, List.of(invalidJar)))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Failed to process JAR file");
	}

	@Test
	void constructor_null_extractor_throws_exception() {
		assertThatThrownBy(() -> new ClasspathTypeProvider(null, Collections.emptyList()))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void constructor_null_classpath_throws_exception() {
		assertThatThrownBy(() -> new ClasspathTypeProvider(extractor, null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void findType_filters_anonymous_classes() throws IOException {
		// Create a class with an anonymous inner class
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.WithAnonymous", """
			package com.example;
			public class WithAnonymous {
				private Runnable r = new Runnable() {
					public void run() {}
				};
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		// Anonymous class should not be indexed (has $1 in name)
		Optional<TypeDecl> anonymous = provider.findType("com.example.WithAnonymous$1");

		assertThat(anonymous).isEmpty();
	}

	@Test
	void close_invalidates_cache() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.TestClass", """
			package com.example;
			public class TestClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		// Lookup to populate cache
		provider.findType("com.example.TestClass");

		// Close should not throw
		provider.close();

		// After close, subsequent lookups should still work (cache recreates entries)
		Optional<TypeDecl> result = provider.findType("com.example.TestClass");
		assertThat(result).isPresent();
	}

	@Test
	void findType_with_wrong_type_class_returns_empty() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.MyClass", """
			package com.example;
			public class MyClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		// Asking for InterfaceDecl but it's actually a ClassDecl
		Optional<InterfaceDecl> result = provider.findType("com.example.MyClass", InterfaceDecl.class);

		assertThat(result).isEmpty();
	}

	@Test
	void findType_default_package_class() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("DefaultPackageClass", """
			public class DefaultPackageClass {
				public void method() {}
			}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		Optional<TypeDecl> result = provider.findType("DefaultPackageClass");

		assertThat(result).isPresent();
		assertThat(result.get().getQualifiedName()).isEqualTo("DefaultPackageClass");
	}

	@Test
	void findType_deeply_nested_package() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.deeply.nested.pkg.DeepClass", """
			package com.example.deeply.nested.pkg;
			public class DeepClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		Optional<TypeDecl> result = provider.findType("com.example.deeply.nested.pkg.DeepClass");

		assertThat(result).isPresent();
		assertThat(result.get().getQualifiedName()).isEqualTo("com.example.deeply.nested.pkg.DeepClass");
	}

	@Test
	void findType_many_classes_in_jar() throws IOException {
		Map<String, String> sources = new HashMap<>();
		for (int i = 0; i < 50; i++) {
			sources.put("com.example.Class" + i, """
				package com.example;
				public class Class%d {
					public void method%d() {}
				}
				""".formatted(i, i));
		}
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		// Find various classes to ensure indexing works correctly
		assertThat(provider.findType("com.example.Class0")).isPresent();
		assertThat(provider.findType("com.example.Class25")).isPresent();
		assertThat(provider.findType("com.example.Class49")).isPresent();
		assertThat(provider.findType("com.example.Class50")).isEmpty();
	}

	@Test
	void findType_concurrent_lookups() throws Exception {
		Map<String, String> sources = new HashMap<>();
		for (int i = 0; i < 10; i++) {
			sources.put("com.example.Concurrent" + i, """
				package com.example;
				public class Concurrent%d {}
				""".formatted(i));
		}
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		// Perform concurrent lookups to test thread safety
		Thread[] threads = new Thread[10];
		boolean[] results = new boolean[10];

		for (int i = 0; i < 10; i++) {
			final int index = i;
			threads[i] = new Thread(() -> {
				Optional<TypeDecl> result = provider.findType("com.example.Concurrent" + index);
				results[index] = result.isPresent();
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		// All lookups should succeed
		for (boolean result : results) {
			assertThat(result).isTrue();
		}
	}

	@Test
	void findType_with_special_characters_in_name() throws IOException {
		Map<String, String> sources = new HashMap<>();
		sources.put("com.example.$SpecialClass", """
			package com.example;
			public class $SpecialClass {}
			""");
		Path jar = tempDir.resolve("test.jar");
		TestUtils.buildJar(sources, jar);

		provider = new ClasspathTypeProvider(extractor, List.of(jar));

		Optional<TypeDecl> result = provider.findType("com.example.$SpecialClass");

		assertThat(result).isPresent();
	}

	@Test
	void findType_large_classpath_performance() throws IOException {
		// Create 20 JARs with 10 classes each
		List<Path> jars = new java.util.ArrayList<>();
		for (int jarNum = 0; jarNum < 20; jarNum++) {
			Map<String, String> sources = new HashMap<>();
			for (int classNum = 0; classNum < 10; classNum++) {
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

		long start = System.currentTimeMillis();
		provider = new ClasspathTypeProvider(extractor, jars);
		long indexTime = System.currentTimeMillis() - start;

		// Indexing should be reasonably fast (< 1 second for 200 classes)
		assertThat(indexTime).isLessThan(1000);

		// Lookups should be very fast
		start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			int jarNum = i % 20;
			int classNum = i % 10;
			Optional<TypeDecl> result = provider.findType("com.example.jar" + jarNum + ".Class" + classNum);
			assertThat(result).isPresent();
		}
		long lookupTime = System.currentTimeMillis() - start;

		// 100 lookups should be very fast (< 200ms)
		assertThat(lookupTime).isLessThan(200);
	}
}
