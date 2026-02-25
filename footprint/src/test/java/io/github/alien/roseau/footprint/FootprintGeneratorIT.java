package io.github.alien.roseau.footprint;

import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FootprintGeneratorIT {
	@Test
	void generated_footprint_compiles_links_and_runs_on_full_fixture() throws Exception {
		Path sourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-it");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(sourceTree, generatedSource, "test.footprint", "Footprint");
			Path packageInfo = generatedSource.getParent().resolve("package-info.java");
			assertTrue(Files.exists(packageInfo), "Expected package-info companion for PACKAGE-target annotations");
			String packageInfoContent = Files.readString(packageInfo);
			assertTrue(packageInfoContent.contains("@fixture.full.PackageOnlyMarker"),
				"Expected PACKAGE-target annotation usages in package-info companion");

			Path apiBin = tempDir.resolve("api-bin");
			compileSources(allJavaSources(sourceTree), apiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			compileSources(generatedClientSources(generatedSource), clientBin, List.of("-classpath", apiBin.toString()));

			RunResult run = runMain(
				"test.footprint.Footprint",
				clientBin + System.getProperty("path.separator") + apiBin
			);

			assertEquals(0, run.exitCode(), "Footprint should run successfully on v1 fixture:\n" + run.output());
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void generated_footprint_contains_core_usage_forms() throws Exception {
		String generated = new FootprintService().generate(fixtureSourceTree(), "test.footprint", "Footprint");

		assertTrue(generated.contains(".class;"), "Expected type token usages");
		assertTrue(generated.contains("::"), "Expected method reference usages");
		assertTrue(generated.contains("::new"), "Expected constructor reference usages");
		assertTrue(generated.contains(" extends "), "Expected inheritance usages");
		assertTrue(generated.contains("new fixture.full.Service()"), "Expected interface implementation usages");
		assertTrue(generated.contains("catch (fixture.full.CheckedProblem"), "Expected checked-exception catch blocks");
		assertTrue(generated.contains("catch (java.lang.RuntimeException"), "Expected runtime-safe catch blocks");
		assertTrue(generated.contains("throws fixture.full.CheckedProblem"), "Expected throws-clause usages for checked exceptions");
		assertTrue(generated.contains("throw (fixture.full.CheckedProblem) null;"), "Expected throw usages for exception types");
		assertTrue(generated.contains("fixture.full.BaseApi"), "Expected core type references");
		assertTrue(generated.contains("fixture.full.DerivedApi"), "Expected subtype references");
		assertTrue(Pattern.compile("fixture\\.full\\.GenericHolder<[^>]+>").matcher(generated).find(),
			"Expected generic parameterized type usages");
		assertTrue(Pattern.compile("fixture\\.full\\.GenericContract<[^>]+>").matcher(generated).find(),
			"Expected generic interface parameterized usages");
		assertTrue(Pattern.compile("fixture\\.full\\.BoundedContract<[^>]+>").matcher(generated).find(),
			"Expected bounded-generic parameterized type usages");
		assertTrue(Pattern.compile(
			"private abstract static class BoundWitness\\d+ extends java\\.lang\\.Number implements java\\.lang\\.Comparable<BoundWitness\\d+> \\{")
				.matcher(generated).find(),
			"Expected class+interface intersection witness declarations");
		assertTrue(Pattern.compile(
			"private interface BoundWitness\\d+ extends java\\.lang\\.Runnable, java\\.lang\\.AutoCloseable \\{")
				.matcher(generated).find(),
			"Expected interface-only intersection witness declarations");
		assertTrue(Pattern.compile("fixture\\.full\\.IntersectionGeneric<BoundWitness\\d+>")
				.matcher(generated).find(),
			"Expected intersection-bound generic parameterized type usages");
		assertTrue(Pattern.compile("\\.<BoundWitness\\d+>pick\\(")
				.matcher(generated).find(),
			"Expected explicit intersection-bound method type-argument invocations");
		assertTrue(Pattern.compile("fixture\\.full\\.BaseApi\\s+upcastRef\\d+\\s*=\\s*\\(fixture\\.full\\.DerivedApi\\)\\s*null;")
				.matcher(generated).find(),
			"Expected explicit supertype upcast compatibility probes");
		assertTrue(generated.contains("throw (fixture.full.UncheckedProblem) null;"),
			"Expected uncaught throw probes for unchecked exceptions");
		assertTrue(Pattern.compile("@fixture\\.full\\.FullMarker")
				.matcher(generated).find(),
			"Expected annotation application usages");
		assertTrue(Pattern.compile("@fixture\\.full\\.RepeatableMarker\\s+@fixture\\.full\\.RepeatableMarker\\s+int\\s+repeatedAnnotationLocal\\d+\\s*=\\s*0;")
				.matcher(generated).find(),
			"Expected repeated annotation usages for repeatable annotations");
		assertTrue(Pattern.compile("@fixture\\.full\\.RepeatableMarker\\s+@fixture\\.full\\.RepeatableMarker\\s+void\\s+m\\(\\)\\s*\\{")
				.matcher(generated).find(),
			"Expected repeated annotation usages on non-field targets");
		assertTrue(generated.contains("@fixture.full.BoundedClassMarker(java.lang.Number.class)"),
			"Expected bounded Class<?> annotation value synthesis");
		assertTrue(Pattern.compile("fixture\\.full\\.BaseApi::getValue")
				.matcher(generated).find(),
			"Expected unbound instance method references");
		assertTrue(generated.contains("outer.super("),
			"Expected non-static inner-class subclass constructors using outer.super(...)");
		assertTrue(generated.contains("ANNOTATION_TYPE target requires class-scope annotation declaration emission"),
			"Expected explicit note for annotation-type target coverage gap");
		assertTrue(Pattern.compile("class GenericTypeProbe\\d+<")
				.matcher(generated).find(),
			"Expected generic type-parameter probes");
		assertTrue(Pattern.compile("class GenericMethodProbe\\d+ \\{")
				.matcher(generated).find(),
			"Expected generic method-parameter probes");
		assertTrue(Pattern.compile("class NestedTypeAccess\\d+ extends fixture\\.full\\.BaseApi")
				.matcher(generated).find(),
			"Expected protected nested-type subclass-access probes");
		assertTrue(Pattern.compile("ProtectedNested\\s+nestedTypeRef\\d+\\s*=\\s*\\(ProtectedNested\\) null;")
				.matcher(generated).find(),
			"Expected first-class protected nested-type references in subclass context");
		assertFalse(generated.contains("// Parameterized use not representable for fixture.full.IntersectionGeneric"),
			"Intersection-bounded generic type should now be representable");
		assertFalse(generated.contains("fixture.full.HiddenPackageType"), "Package-private top-level types should be ignored");
	}

	@Test
	void generated_footprint_preserves_forward_type_parameter_bounds() throws Exception {
		Path tempDir = Files.createTempDirectory("roseau-footprint-forward-bounds");
		try {
			Path sourceTree = tempDir.resolve("src");
			Path apiType = sourceTree.resolve("forward/bounds/A.java");
			Files.createDirectories(apiType.getParent());
			Files.writeString(
				apiType,
				"""
					package forward.bounds;
					public class A<T extends U, U> {}
					"""
			);

			String generated = new FootprintService().generate(sourceTree, "test.footprint", "Footprint");
			assertTrue(
				Pattern.compile("class GenericTypeProbe\\d+<T extends U, U>").matcher(generated).find(),
				"Expected forward bound reference to be preserved in generated formal type parameters"
			);
			assertFalse(
				Pattern.compile("class GenericTypeProbe\\d+<T extends java\\.lang\\.Object, U>").matcher(generated).find(),
				"Forward bound must not degrade to java.lang.Object"
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_incompatible_field_type_parameter_change() throws Exception {
		assertFieldTypeParameterFootprintCompileFailure(
			"""
				package fixture.generic;
				public class A<T, U> {
					public T f;
				}
				""",
			"""
				package fixture.generic;
				public class A<T, U> {
					public U f;
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_subtype_field_type_parameter_change() throws Exception {
		assertFieldTypeParameterFootprintCompileFailure(
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public T f;
				}
				""",
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public U f;
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_supertype_field_type_parameter_change() throws Exception {
		assertFieldTypeParameterFootprintCompileFailure(
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public U f;
				}
				""",
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public T f;
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_incompatible_method_return_type_parameter_change() throws Exception {
		assertMethodReturnTypeParameterFootprintCompileFailure(
			"""
				package fixture.generic;
				public class A<T, U> {
					public T m() { return null; }
				}
				""",
			"""
				package fixture.generic;
				public class A<T, U> {
					public U m() { return null; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_primitive_method_return_type_change() throws Exception {
		assertMethodReturnTypeFootprintCompileFailure(
			"""
				package fixture.returns;
				public class A {
					public int m() { return 0; }
				}
				""",
			"""
				package fixture.returns;
				public class A {
					public long m() { return 0L; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_object_method_return_type_change() throws Exception {
		assertMethodReturnTypeFootprintCompileFailure(
			"""
				package fixture.returns;
				public class A {
					public java.io.InputStream m() { return null; }
				}
				""",
			"""
				package fixture.returns;
				public class A {
					public java.io.File m() { return null; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_generic_method_return_type_change() throws Exception {
		assertMethodReturnTypeFootprintCompileFailure(
			"""
				package fixture.returns;
				public class A {
					public java.util.List<java.lang.Integer> m() { return null; }
				}
				""",
			"""
				package fixture.returns;
				public class A {
					public java.util.List<java.lang.String> m() { return null; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_wildcard_method_return_type_change() throws Exception {
		assertMethodReturnTypeFootprintCompileFailure(
			"""
				package fixture.returns;
				public class A {
					public java.util.List<? extends java.lang.Number> m() { return null; }
				}
				""",
			"""
				package fixture.returns;
				public class A {
					public java.util.List<? super java.lang.Number> m() { return null; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_array_method_return_type_change() throws Exception {
		assertMethodReturnTypeFootprintCompileFailure(
			"""
				package fixture.returns;
				public class A {
					public java.lang.String[] m() { return null; }
				}
				""",
			"""
				package fixture.returns;
				public class A {
					public java.lang.Object[] m() { return null; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_subtype_method_return_type_parameter_change() throws Exception {
		assertMethodReturnTypeParameterFootprintCompileFailure(
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public T m() { return null; }
				}
				""",
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public U m() { return null; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_supertype_method_return_type_parameter_change() throws Exception {
		assertMethodReturnTypeParameterFootprintCompileFailure(
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public U m() { return null; }
				}
				""",
			"""
				package fixture.generic;
				public class A<T, U extends T> {
					public T m() { return null; }
				}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_inherited_static_method_return_type_change() throws Exception {
		assertInheritedStaticMethodReturnTypeFootprintCompileFailure(
			"""
				package fixture.inherited;
				class A {
					public static int m() { return 0; }
				}
				""",
			"""
				package fixture.inherited;
				class A {
					public static String m() { return null; }
				}
				""",
			"""
				package fixture.inherited;
				public class B extends A {}
				"""
		);
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_source_breaking_v2() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-source");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/Service.java"),
				"default Number identity(Number value) {",
				"default Number identity(Number value) throws CheckedProblem {"
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail on source-breaking v2");
			assertTrue(
				compilation.diagnostics().contains("unreported exception") ||
					compilation.diagnostics().contains("incompatible thrown types"),
				"Expected checked-exception compilation failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_when_supertype_is_removed() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-supertype");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/DerivedApi.java"),
				"public class DerivedApi extends BaseApi implements Service, ThrowingOps, AutoCloseable {",
				"public class DerivedApi implements Service, ThrowingOps, AutoCloseable {"
			);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/DerivedApi.java"),
				"super();",
				""
			);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/DerivedApi.java"),
				"super(seed);",
				""
			);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/DerivedApi.java"),
				"super(name);",
				""
			);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/DerivedApi.java"),
				"@Override\n\tprotected void protectedMethod() throws CheckedProblem {\n\t\tsuper.protectedMethod();\n\t}\n\n",
				""
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when a public supertype is removed");
			assertTrue(
				compilation.diagnostics().contains("incompatible types"),
				"Expected supertype-compatibility compilation failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_compiled_footprint_fails_to_link_on_binary_breaking_v2() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-binary");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v1ApiBin = tempDir.resolve("v1-api-bin");
			compileSources(allJavaSources(v1SourceTree), v1ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			compileSources(generatedClientSources(generatedSource), clientBin, List.of("-classpath", v1ApiBin.toString()));

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/BaseApi.java"),
				"public static int STATIC_COUNTER = 1;",
				"public static int STATIC_COUNTER_V2 = 1;"
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			RunResult run = runMain(
				"test.footprint.Footprint",
				clientBin + System.getProperty("path.separator") + v2ApiBin
			);

			assertNotEquals(0, run.exitCode(), "Expected binary linkage failure against v2");
			assertTrue(
				run.output().contains("NoSuchFieldError") || run.output().contains("LinkageError"),
				"Expected linkage-related error output, got:\n" + run.output()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_when_unchecked_exception_becomes_checked() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-checked-ex");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/UncheckedProblem.java"),
				"extends RuntimeException",
				"extends Exception"
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when an unchecked exception becomes checked");
			assertTrue(
				compilation.diagnostics().contains("unreported exception") ||
					compilation.diagnostics().contains("incompatible types"),
				"Expected unreported checked exception failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_annotation_method_default_removal() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-ann-default");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/FullMarker.java"),
				"String value() default \"marker\";",
				"String value();"
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when an annotation method loses its default");
			assertTrue(
				compilation.diagnostics().contains("missing a default value for the element") ||
					compilation.diagnostics().contains("missing element value"),
				"Expected annotation default-related compilation failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_new_annotation_method_without_default() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-ann-new-required");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/FullMarker.java"),
				"int[] codes() default {1, 2, 3};",
				"int[] codes() default {1, 2, 3};\n\n\tString required();"
			);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/BaseApi.java"),
				"@FullMarker(\"base\")\n",
				""
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when a required annotation method is added");
			assertTrue(
				compilation.diagnostics().contains("missing element value") ||
					compilation.diagnostics().contains("missing a default value for the element"),
				"Expected missing annotation element compilation failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_annotation_target_reduction() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-ann-target");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/FullMarker.java"),
				"ElementType.TYPE,\n\tElementType.METHOD,\n\tElementType.FIELD,\n\tElementType.PARAMETER,\n\tElementType.CONSTRUCTOR",
				"ElementType.FIELD"
			);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/BaseApi.java"),
				"@FullMarker(\"base\")\n",
				""
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when annotation targets are reduced");
			assertTrue(
				compilation.diagnostics().contains("not applicable to this kind of declaration"),
				"Expected annotation target applicability failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_package_annotation_target_reduction() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-ann-package-target");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/PackageOnlyMarker.java"),
				"@java.lang.annotation.Target(java.lang.annotation.ElementType.PACKAGE)",
				"@java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)"
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when package annotation targets are reduced");
			assertTrue(
				compilation.diagnostics().contains("package annotations should be in file package-info.java") ||
					compilation.diagnostics().contains("not applicable to this kind of declaration"),
				"Expected package annotation target applicability failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_generated_footprint_fails_to_compile_when_annotation_is_no_longer_repeatable() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-ann-repeatable");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/RepeatableMarker.java"),
				"@java.lang.annotation.Repeatable(RepeatableMarker.Container.class)\n",
				""
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when repeatable annotation support is removed");
			assertTrue(
				compilation.diagnostics().contains("not a repeatable annotation type") ||
					compilation.diagnostics().contains("not a repeatable annotation interface"),
				"Expected repeatable-annotation compilation failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	private static Path fixtureSourceTree() throws URISyntaxException {
		return Path.of(
			Objects.requireNonNull(
				FootprintGeneratorIT.class.getResource("/full-api/v1"),
				"Missing full-api fixture"
			).toURI()
		);
	}

	private static void assertFieldTypeParameterFootprintCompileFailure(String v1Source, String v2Source) throws Exception {
		Path tempDir = Files.createTempDirectory("roseau-footprint-type-params");
		try {
			Path v1SourceTree = tempDir.resolve("v1-source");
			writeJavaSource(v1SourceTree, "fixture/generic/A.java", v1Source);

			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			writeJavaSource(v2SourceTree, "fixture/generic/A.java", v2Source);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when generic field type changes");
			assertTrue(
				compilation.diagnostics().contains("incompatible types"),
				"Expected generic field type-parameter incompatibility, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	private static void assertMethodReturnTypeParameterFootprintCompileFailure(String v1Source, String v2Source)
		throws Exception {
		Path tempDir = Files.createTempDirectory("roseau-footprint-method-type-params");
		try {
			Path v1SourceTree = tempDir.resolve("v1-source");
			writeJavaSource(v1SourceTree, "fixture/generic/A.java", v1Source);

			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			writeJavaSource(v2SourceTree, "fixture/generic/A.java", v2Source);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when generic method return type changes");
			assertTrue(
				compilation.diagnostics().contains("incompatible types") ||
					compilation.diagnostics().contains("does not override") ||
					compilation.diagnostics().contains("return type"),
				"Expected generic method return-type incompatibility, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	private static void assertMethodReturnTypeFootprintCompileFailure(String v1Source, String v2Source)
		throws Exception {
		Path tempDir = Files.createTempDirectory("roseau-footprint-return-types");
		try {
			Path v1SourceTree = tempDir.resolve("v1-source");
			writeJavaSource(v1SourceTree, "fixture/returns/A.java", v1Source);

			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			writeJavaSource(v2SourceTree, "fixture/returns/A.java", v2Source);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail when method return type changes");
			assertTrue(
				compilation.diagnostics().contains("incompatible types"),
				"Expected return-type incompatibility, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	private static void assertInheritedStaticMethodReturnTypeFootprintCompileFailure(String v1ASource, String v2ASource,
	                                                                                String bSource)
		throws Exception {
		Path tempDir = Files.createTempDirectory("roseau-footprint-inherited-static");
		try {
			Path v1SourceTree = tempDir.resolve("v1-source");
			writeJavaSource(v1SourceTree, "fixture/inherited/A.java", v1ASource);
			writeJavaSource(v1SourceTree, "fixture/inherited/B.java", bSource);

			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			writeJavaSource(v2SourceTree, "fixture/inherited/A.java", v2ASource);
			writeJavaSource(v2SourceTree, "fixture/inherited/B.java", bSource);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				generatedClientSources(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(),
				"Compilation should fail when inherited static method return type changes");
			assertTrue(
				compilation.diagnostics().contains("incompatible types"),
				"Expected inherited static method return-type incompatibility, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	private static void writeJavaSource(Path root, String relativePath, String content) throws IOException {
		Path source = root.resolve(relativePath);
		Files.createDirectories(source.getParent());
		Files.writeString(source, content);
	}

	private static List<Path> allJavaSources(Path sourceTree) throws IOException {
		try (Stream<Path> stream = Files.walk(sourceTree)) {
			return stream
				.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
				.sorted()
				.toList();
		}
	}

	private static List<Path> generatedClientSources(Path generatedSource) throws IOException {
		Path parent = generatedSource.getParent();
		String mainName = generatedSource.getFileName().toString();
		try (Stream<Path> stream = Files.list(parent)) {
			return stream
				.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
				.filter(path -> {
					String name = path.getFileName().toString();
					return name.equals(mainName) || name.equals("package-info.java") || name.equals("module-info.java");
				})
				.sorted()
				.toList();
		}
	}

	private static void compileSources(List<Path> sources, Path outputDir, List<String> extraOptions) throws IOException {
		CompilationResult result = compileSourcesWithDiagnostics(sources, outputDir, extraOptions);
		if (!result.success()) {
			fail("Compilation failed:\n" + result.diagnostics());
		}
	}

	private static CompilationResult compileSourcesWithDiagnostics(List<Path> sources, Path outputDir,
	                                                               List<String> extraOptions) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull(compiler, "No system Java compiler available");

		Files.createDirectories(outputDir);
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		List<String> options = new ArrayList<>();
		options.add("--release");
		options.add("25");
		options.add("-d");
		options.add(outputDir.toString());
		options.addAll(extraOptions);

		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sources);
			boolean success = Boolean.TRUE.equals(
				compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call()
			);

			String message = diagnostics.getDiagnostics().stream()
				.map(FootprintGeneratorIT::formatDiagnostic)
				.collect(Collectors.joining(System.lineSeparator()));
			return new CompilationResult(success, message);
		}
	}

	private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
		String source = diagnostic.getSource() == null ? "<no-source>" : diagnostic.getSource().toUri().toString();
		return "%s:%d:%d %s".formatted(source, diagnostic.getLineNumber(), diagnostic.getColumnNumber(),
			diagnostic.getMessage(null));
	}

	private static RunResult runMain(String mainClass, String classpath) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder("java", "-cp", classpath, mainClass);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
		process.getInputStream().transferTo(output);
		int exitCode = process.waitFor();
		return new RunResult(exitCode, output.toString());
	}

	private static void copyRecursively(Path sourceRoot, Path targetRoot) throws IOException {
		try (Stream<Path> stream = Files.walk(sourceRoot)) {
			for (Path source : stream.toList()) {
				Path relative = sourceRoot.relativize(source);
				Path target = targetRoot.resolve(relative);
				if (Files.isDirectory(source)) {
					Files.createDirectories(target);
				} else {
					Files.createDirectories(target.getParent());
					Files.copy(source, target);
				}
			}
		}
	}

	private static void replaceInFile(Path file, String from, String to) throws IOException {
		String content = Files.readString(file);
		if (!content.contains(from)) {
			fail("Could not find expected text to replace in " + file + ":\n" + from);
		}
		Files.writeString(file, content.replace(from, to));
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (root == null || Files.notExists(root)) {
			return;
		}

		try (Stream<Path> walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private record RunResult(int exitCode, String output) {
	}

	private record CompilationResult(boolean success, String diagnostics) {
	}
}
