package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static io.github.alien.roseau.utils.TestUtils.buildSourcesAPI;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unresolved types spelled with their qualified name <em>and</em> type arguments, e.g.
 * {@code com.example.absent.Missing<String>} with no classpath. The same type is extracted correctly when it is
 * imported ({@code Missing<String>}) or used without type arguments ({@code com.example.absent.Missing}), see
 * {@link JdtTypesExtractorTest}. With this spelling, members whose signature uses it are silently dropped, and
 * supertypes and bounds silently become {@code java.lang.Object}.
 */
class QualifiedParameterizedUnresolvedTypesTest {
	static final TypeReference<?> MISSING_OF_STRING =
		new TypeReference<>("com.example.absent.Missing", List.of(TypeReference.STRING));

	@Test
	void field_type_is_kept() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String> f;
			}""");

		var a = assertClass(api, "A");
		assertThat(assertField(api, a, "f").getType()).isEqualTo(MISSING_OF_STRING);
	}

	@Test
	void field_array_type_is_kept() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String>[] f;
			}""");

		var a = assertClass(api, "A");
		assertThat(assertField(api, a, "f").getType()).isEqualTo(new ArrayTypeReference(MISSING_OF_STRING, 1));
	}

	@Test
	void field_type_argument_is_kept() {
		var api = buildSourcesAPI("""
			public class A {
				public java.util.List<com.example.absent.Missing<String>> f;
			}""");

		var a = assertClass(api, "A");
		assertThat(assertField(api, a, "f").getType())
			.isEqualTo(new TypeReference<>("java.util.List", List.of(MISSING_OF_STRING)));
	}

	@Test
	void method_return_type_is_kept() {
		var api = buildSourcesAPI("""
			public class A {
				public com.example.absent.Missing<String> m() { return null; }
			}""");

		var a = assertClass(api, "A");
		assertThat(assertMethod(api, a, "m()").getType()).isEqualTo(MISSING_OF_STRING);
	}

	@Test
	void method_parameter_type_is_kept() {
		var api = buildSourcesAPI("""
			public class A {
				public void m(com.example.absent.Missing<String> p) {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m(com.example.absent.Missing)");
		assertThat(m.getParameters().getFirst().type()).isEqualTo(MISSING_OF_STRING);
	}

	@Test
	void constructor_parameter_type_is_kept() {
		var api = buildSourcesAPI("""
			public class A {
				public A(com.example.absent.Missing<String> p) {}
			}""");

		var a = assertClass(api, "A");
		var cons = assertConstructor(api, a, "<init>(com.example.absent.Missing)");
		assertThat(cons.getParameters().getFirst().type()).isEqualTo(MISSING_OF_STRING);
	}

	@Test
	void record_component_accessor_and_canonical_constructor_are_kept() {
		var api = buildSourcesAPI("public record A(com.example.absent.Missing<String> c) {}");

		var a = assertRecord(api, "A");
		assertThat(assertMethod(api, a, "c()").getType()).isEqualTo(MISSING_OF_STRING);
		assertConstructor(api, a, "<init>(com.example.absent.Missing)");
	}

	@Test
	void superclass_is_kept() {
		var api = buildSourcesAPI("public class A extends com.example.absent.Missing<String> {}");

		var a = assertClass(api, "A");
		assertThat(a.getSuperClass()).isEqualTo(MISSING_OF_STRING);
	}

	@Test
	void implemented_interface_is_kept() {
		var api = buildSourcesAPI("public class A implements com.example.absent.Missing<String> {}");

		var a = assertClass(api, "A");
		assertThat(a.getImplementedInterfaces()).singleElement().isEqualTo(MISSING_OF_STRING);
	}

	@Test
	void extended_interface_is_kept() {
		var api = buildSourcesAPI("public interface A extends com.example.absent.Missing<String> {}");

		var a = assertInterface(api, "A");
		assertThat(a.getImplementedInterfaces()).singleElement().isEqualTo(MISSING_OF_STRING);
	}

	@Test
	void type_parameter_bound_is_kept() {
		var api = buildSourcesAPI("public class A<T extends com.example.absent.Missing<String>> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters().getFirst().bounds()).containsExactly(MISSING_OF_STRING);
	}

	@Test
	void method_type_parameter_bound_is_kept() {
		var api = buildSourcesAPI("""
			public class A {
				public <T extends com.example.absent.Missing<String>> void m(T t) {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m(com.example.absent.Missing)");
		assertThat(m.getFormalTypeParameters().getFirst().bounds()).containsExactly(MISSING_OF_STRING);
	}
}
