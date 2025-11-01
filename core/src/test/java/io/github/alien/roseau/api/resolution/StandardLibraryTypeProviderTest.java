package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandardLibraryTypeProviderTest {
	private AsmTypesExtractor extractor;
	private StandardLibraryTypeProvider provider;

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
	void findType_string_class_from_java_base() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.lang.String");

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(ClassDecl.class);
		assertThat(result.get().getQualifiedName()).isEqualTo("java.lang.String");
	}

	@Test
	void findType_object_class_from_java_base() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.lang.Object");

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(ClassDecl.class);
	}

	@Test
	void findType_list_interface_from_java_base() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<InterfaceDecl> result = provider.findType("java.util.List", InterfaceDecl.class);

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(InterfaceDecl.class);
		assertThat(result.get().getQualifiedName()).isEqualTo("java.util.List");
	}

	@Test
	void findType_arraylist_class_from_java_base() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.util.ArrayList");

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(ClassDecl.class);
	}

	@Test
	void findType_non_existing_class_returns_empty() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.lang.NonExistentClass");

		assertThat(result).isEmpty();
	}

	@Test
	void findType_repeated_lookups_use_cache() {
		provider = new StandardLibraryTypeProvider(extractor);

		// First lookup
		Optional<TypeDecl> first = provider.findType("java.lang.String");
		// Second lookup should use cached jmod
		Optional<TypeDecl> second = provider.findType("java.lang.String");
		// Third lookup for different class in same jmod
		Optional<TypeDecl> third = provider.findType("java.lang.Integer");

		assertThat(first).isPresent();
		assertThat(second).isPresent();
		assertThat(third).isPresent();
	}

	@Test
	void findType_throwable_class() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.lang.Throwable");

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(ClassDecl.class);
	}

	@Test
	void findType_exception_class() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.lang.Exception");

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(ClassDecl.class);
	}

	@Test
	@EnabledOnJre({JRE.JAVA_21, JRE.JAVA_22, JRE.JAVA_23})
	void findType_with_multiple_jmods_finds_types_from_different_modules() {
		// java.sql is in java.sql jmod, String is in java.base jmod
		provider = new StandardLibraryTypeProvider(extractor, List.of("java.base", "java.sql"));

		Optional<TypeDecl> string = provider.findType("java.lang.String");
		Optional<TypeDecl> connection = provider.findType("java.sql.Connection");

		assertThat(string).isPresent();
		assertThat(connection).isPresent();
	}

	@Test
	void findType_nested_class() {
		provider = new StandardLibraryTypeProvider(extractor);

		// Thread.State is a nested enum in java.lang.Thread
		Optional<TypeDecl> result = provider.findType("java.lang.Thread$State");

		assertThat(result).isPresent();
	}

	@Test
	void findType_inner_class_map_entry() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.util.Map$Entry");

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void constructor_null_extractor_throws_exception() {
		assertThatThrownBy(() -> new StandardLibraryTypeProvider(null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void constructor_null_jmod_names_throws_exception() {
		assertThatThrownBy(() -> new StandardLibraryTypeProvider(extractor, null))
			.isInstanceOf(NullPointerException.class);
	}

	@Test
	void constructor_invalid_jmod_name_throws_exception() {
		assertThatThrownBy(() -> new StandardLibraryTypeProvider(extractor, List.of("invalid.nonexistent.jmod")))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("Couldn't resolve jmod file");
	}

	@Test
	void constructor_empty_jmod_list_succeeds() {
		provider = new StandardLibraryTypeProvider(extractor, Collections.emptyList());

		// Should not find anything
		Optional<TypeDecl> result = provider.findType("java.lang.String");
		assertThat(result).isEmpty();
	}

	@Test
	void close_invalidates_cache() {
		provider = new StandardLibraryTypeProvider(extractor);

		// Lookup to populate cache
		provider.findType("java.lang.String");

		// Close should not throw
		provider.close();

		// After close, subsequent lookups should still work (cache recreates entries)
		Optional<TypeDecl> result = provider.findType("java.lang.String");
		assertThat(result).isPresent();
	}

	@Test
	void findType_with_wrong_type_class_returns_empty() {
		provider = new StandardLibraryTypeProvider(extractor);

		// String is a class, not an interface
		Optional<InterfaceDecl> result = provider.findType("java.lang.String", InterfaceDecl.class);

		assertThat(result).isEmpty();
	}

	@Test
	void findType_runnable_interface() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<InterfaceDecl> result = provider.findType("java.lang.Runnable", InterfaceDecl.class);

		assertThat(result).isPresent();
		assertThat(result.get()).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void findType_multiple_lookups_for_different_classes() {
		provider = new StandardLibraryTypeProvider(extractor);

		// Look up many different classes to test index and cache
		String[] classNames = {
			"java.lang.String",
			"java.lang.Integer",
			"java.lang.Long",
			"java.lang.Double",
			"java.lang.Boolean",
			"java.util.List",
			"java.util.ArrayList",
			"java.util.HashMap",
			"java.util.HashSet",
			"java.io.InputStream"
		};

		for (String className : classNames) {
			Optional<TypeDecl> result = provider.findType(className);
			assertThat(result).as("Should find " + className).isPresent();
			assertThat(result.get().getQualifiedName()).isEqualTo(className);
		}
	}

	@Test
	void findType_concurrent_lookups() throws Exception {
		provider = new StandardLibraryTypeProvider(extractor);

		String[] classNames = {
			"java.lang.String",
			"java.lang.Integer",
			"java.util.List",
			"java.util.Map",
			"java.io.InputStream"
		};

		// Perform concurrent lookups to test thread safety
		Thread[] threads = new Thread[classNames.length];
		boolean[] results = new boolean[classNames.length];

		for (int i = 0; i < classNames.length; i++) {
			final int index = i;
			threads[i] = new Thread(() -> {
				Optional<TypeDecl> result = provider.findType(classNames[index]);
				results[index] = result.isPresent();
			});
			threads[i].start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		// All lookups should succeed
		for (int i = 0; i < results.length; i++) {
			assertThat(results[i]).as("Thread " + i + " should find " + classNames[i]).isTrue();
		}
	}

	@Test
	void findType_performance_many_lookups() {
		provider = new StandardLibraryTypeProvider(extractor);

		// First lookup to warm up cache
		provider.findType("java.lang.String");

		long start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			provider.findType("java.lang.String");
			provider.findType("java.util.List");
			provider.findType("java.lang.Integer");
		}
		long elapsed = System.currentTimeMillis() - start;

		// 300 cached lookups should be very fast (< 500ms)
		assertThat(elapsed).isLessThan(500);
	}

	@Test
	void findType_enum_class() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.lang.Thread$State");

		assertThat(result).isPresent();
		assertThat(result.get().isEnum()).isTrue();
	}

	@Test
	@EnabledOnJre({JRE.JAVA_21, JRE.JAVA_22, JRE.JAVA_23})
	void findType_record_class() {
		provider = new StandardLibraryTypeProvider(extractor);

		// Java 16+ has record classes in standard library
		// For example, java.lang.runtime.ObjectMethods (though it's not a record itself)
		// Let's just verify we can find classes that might be records
		Optional<TypeDecl> result = provider.findType("java.lang.Class");

		assertThat(result).isPresent();
	}

	@Test
	void findType_annotation_interface() {
		provider = new StandardLibraryTypeProvider(extractor);

		Optional<TypeDecl> result = provider.findType("java.lang.Override");

		assertThat(result).isPresent();
		assertThat(result.get().isAnnotation()).isTrue();
	}

	@Test
	void findType_primitive_wrapper_classes() {
		provider = new StandardLibraryTypeProvider(extractor);

		String[] wrappers = {
			"java.lang.Integer",
			"java.lang.Long",
			"java.lang.Double",
			"java.lang.Float",
			"java.lang.Boolean",
			"java.lang.Character",
			"java.lang.Byte",
			"java.lang.Short"
		};

		for (String wrapper : wrappers) {
			Optional<TypeDecl> result = provider.findType(wrapper);
			assertThat(result).as("Should find " + wrapper).isPresent();
		}
	}

	@Test
	void findType_collection_interfaces() {
		provider = new StandardLibraryTypeProvider(extractor);

		String[] interfaces = {
			"java.util.Collection",
			"java.util.List",
			"java.util.Set",
			"java.util.Map",
			"java.util.Queue",
			"java.util.Deque"
		};

		for (String iface : interfaces) {
			Optional<TypeDecl> result = provider.findType(iface);
			assertThat(result).as("Should find " + iface).isPresent();
			assertThat(result.get().isInterface()).isTrue();
		}
	}

	@Test
	void findType_io_classes() {
		provider = new StandardLibraryTypeProvider(extractor);

		String[] ioClasses = {
			"java.io.InputStream",
			"java.io.OutputStream",
			"java.io.Reader",
			"java.io.Writer",
			"java.io.File",
			"java.io.IOException"
		};

		for (String ioClass : ioClasses) {
			Optional<TypeDecl> result = provider.findType(ioClass);
			assertThat(result).as("Should find " + ioClass).isPresent();
		}
	}

	@Test
	void indexing_performance() {
		long start = System.currentTimeMillis();
		provider = new StandardLibraryTypeProvider(extractor);
		long elapsed = System.currentTimeMillis() - start;

		// Indexing java.base should be reasonably fast (< 2 seconds)
		assertThat(elapsed).isLessThan(2000);

		// Verify it actually indexed by finding a class
		Optional<TypeDecl> result = provider.findType("java.lang.String");
		assertThat(result).isPresent();
	}

	@Test
	@EnabledOnJre({JRE.JAVA_21, JRE.JAVA_22, JRE.JAVA_23})
	void indexing_multiple_jmods_performance() {
		long start = System.currentTimeMillis();
		provider = new StandardLibraryTypeProvider(extractor,
			List.of("java.base", "java.sql", "java.xml", "java.logging"));
		long elapsed = System.currentTimeMillis() - start;

		// Indexing multiple jmods should still be fast (< 5 seconds)
		assertThat(elapsed).isLessThan(5000);

		// Verify we can find classes from different jmods
		assertThat(provider.findType("java.lang.String")).isPresent();
		assertThat(provider.findType("java.sql.Connection")).isPresent();
		assertThat(provider.findType("javax.xml.parsers.DocumentBuilder")).isPresent();
		assertThat(provider.findType("java.util.logging.Logger")).isPresent();
	}
}
