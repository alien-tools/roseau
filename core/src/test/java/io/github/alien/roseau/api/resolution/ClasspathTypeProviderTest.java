package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClasspathTypeProviderTest {
	private AsmTypesExtractor extractor;
	private ClasspathTypeProvider provider;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		extractor = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
		provider = new ClasspathTypeProvider(extractor, List.of());
	}

	@AfterEach
	void tearDown() throws IOException {
		provider.close();
	}

	@Test
	void does_not_use_roseau_classpath() {
		var result = provider.findType("io.github.alien.roseau.Roseau");
		assertThat(result).isEmpty();
	}

	@Test
	void classpath_class() throws IOException {
		var sources = Map.of("pkg.C", """
			package pkg;
			public class C {}""");
		var jar = tempDir.resolve("test.jar");

		try (var _ = TestUtils.buildJar(sources, jar)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar));
			var result = provider.findType("pkg.C").orElseThrow();
			assertThat(result).isInstanceOf(ClassDecl.class);
		}
	}

	@Test
	void classpath_interface() throws IOException {
		var sources = Map.of("pkg.I", """
			package pkg;
			public interface I {}""");
		var jar = tempDir.resolve("test.jar");

		try (var _ = TestUtils.buildJar(sources, jar)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar));
			var result = provider.findType("pkg.I").orElseThrow();
			assertThat(result).isInstanceOf(InterfaceDecl.class);
		}
	}

	@Test
	void unknown_type_is_empty() {
		var result = provider.findType("pkg.C");
		assertThat(result).isEmpty();
	}

	@Test
	void classpath_nested_type() throws IOException {
		var sources = Map.of("pkg.Outer", """
			package pkg;
			public class Outer {
				public static class Nested {}
			}""");
		var jar = tempDir.resolve("test.jar");

		try (var _ = TestUtils.buildJar(sources, jar)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar));
			var outer = provider.findType("pkg.Outer").orElseThrow();
			var nested = provider.findType("pkg.Outer$Nested").orElseThrow();
			assertThat(outer).isInstanceOf(ClassDecl.class);
			assertThat(nested).isInstanceOf(ClassDecl.class);
		}
	}

	@Test
	void classpath_precedence() throws IOException {
		var sources1 = Map.of("pkg.C", """
			package pkg;
			public class C {
				public void m1() {}
			}""");
		var jar1 = tempDir.resolve("jar1.jar");
		var sources2 = Map.of("pkg.C", """
			package pkg;
			public class C {
				public void m2() {}
			}""");
		var jar2 = tempDir.resolve("jar2.jar");

		try (var j1 = TestUtils.buildJar(sources1, jar1);
		     var j2 = TestUtils.buildJar(sources2, jar2)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar1, jar2));
			var result = provider.findType("pkg.C").orElseThrow();
			assertThat(result).isInstanceOf(ClassDecl.class);
			assertThat(result.getDeclaredMethods())
				.extracting(MethodDecl::getSimpleName)
				.containsExactly("m1");
		}
	}

	@Test
	void classpath_multiple_jars() throws IOException {
		var sources1 = Map.of("pkg1.C1", """
			package pkg1;
			public class C1 {}""");
		var jar1 = tempDir.resolve("jar1.jar");
		var sources2 = Map.of("pkg2.C2", """
			package pkg2;
			public class C2 {}""");
		var jar2 = tempDir.resolve("jar2.jar");

		try (var j1 = TestUtils.buildJar(sources1, jar1);
		     var j2 = TestUtils.buildJar(sources2, jar2)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar1, jar2));
			var c1 = provider.findType("pkg1.C1").orElseThrow();
			var c2 = provider.findType("pkg2.C2").orElseThrow();
			assertThat(c1).isInstanceOf(ClassDecl.class);
			assertThat(c2).isInstanceOf(ClassDecl.class);
		}
	}

	@Test
	void classpath_no_anonymous() throws IOException {
		var sources = Map.of("pkg.C", """
			package pkg;
			public class C {
				public Runnable r = new Runnable() { public void run() {} };			
			}""");
		var jar = tempDir.resolve("jar1.jar");

		try (var _ = TestUtils.buildJar(sources, jar)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar));
			var anonymous = provider.findType("pkg.C$1");
			assertThat(anonymous).isEmpty();
		}
	}

	@Test
	void wrong_type_kind_is_empty() throws IOException {
		var sources = Map.of("pkg.I", """
			package pkg;
			public interface I {}""");
		var jar = tempDir.resolve("test.jar");

		try (var _ = TestUtils.buildJar(sources, jar)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar));
			var result = provider.findType("com.example.MyClass", ClassDecl.class);
			assertThat(result).isEmpty();
		}
	}

	@Test
	void default_package() throws IOException {
		var sources = Map.of("C", """
			public class C {}""");
		var jar = tempDir.resolve("test.jar");

		try (var _ = TestUtils.buildJar(sources, jar)) {
			provider = new ClasspathTypeProvider(extractor, List.of(jar));
			var result = provider.findType("C").orElseThrow();
			assertThat(result.getQualifiedName()).isEqualTo("C");
		}
	}

	@Test
	void stdlib_object_class() {
		var result = provider.findType("java.lang.Object").orElseThrow();
		assertThat(result).isInstanceOf(ClassDecl.class);
	}

	@Test
	void stdlib_string_class() {
		var result = provider.findType("java.lang.String").orElseThrow();
		assertThat(result).isInstanceOf(ClassDecl.class);
	}

	@Test
	void stdlib_sql_module() {
		var result = provider.findType("java.sql.SQLData", InterfaceDecl.class).orElseThrow();
		assertThat(result).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void stdlib_javax_swing() {
		var result = provider.findType("javax.swing.tree.TreeModel").orElseThrow();
		assertThat(result).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void stdlib_inner_class() {
		var result = provider.findType("java.util.Map$Entry").orElseThrow();
		assertThat(result).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void stdlib_inner_enum() {
		var result = provider.findType("java.lang.Thread$State").orElseThrow();
		assertThat(result).isInstanceOf(EnumDecl.class);
	}

	@Test
	void stdlib_unknown_class() {
		var result = provider.findType("java.lang.Unknown");
		assertThat(result).isEmpty();
	}

	@Test
	void stdlib_unknown_package() {
		var result = provider.findType("java.nonexisting.Cls");
		assertThat(result).isEmpty();
	}

	@Test
	void stdlib_unexpected_decl() {
		var result = provider.findType("java.lang.String", InterfaceDecl.class);
		assertThat(result).isEmpty();
	}
}
