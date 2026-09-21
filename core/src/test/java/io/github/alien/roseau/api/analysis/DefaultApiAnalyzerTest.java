package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers what the analyzer resolves once, when it is built: that it only answers from it for the declarations it was
 * built from, that a type it does not hold gets the same answer anyway, and that what it hands out cannot be altered.
 */
class DefaultApiAnalyzerTest {
	private static final String HIERARCHY = """
		public interface Box<T> { T get(); }
		public abstract class Base<T> implements Box<T> { public void common() {} }
		public abstract class A extends Base<String> { public void own() {} }""";

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_type_of_the_same_name_from_another_snapshot_is_not_answered_from_the_index(ApiBuilder builder) {
		var v1 = builder.build("""
			public class Base {}
			public class A extends Base { public void m() {} }""");
		var v2 = builder.build("""
			public interface I {}
			public class A implements I { public void n() {} }""");

		var a1 = assertClass(v1, "A");
		var a2 = assertClass(v2, "A");

		// Both snapshots hold an "A", and the differ hands one version's type to the other version's analyzer all the
		// time. Answering a2 from v1's index would describe a type that does not exist — neither its members nor its
		// place in the hierarchy.
		assertThat(v1.analyzer().getExportedMethodsByErasure(a2)).containsKey("n()").doesNotContainKey("m()");
		assertThat(v1.analyzer().getAllSuperTypes(a2))
			.extracting(TypeReference::getQualifiedName).contains("I").doesNotContain("Base");

		// ...while its own declaration is still answered from what it resolved
		assertThat(v1.analyzer().getExportedMethodsByErasure(a1)).containsKey("m()").doesNotContainKey("n()");
		assertThat(v1.analyzer().getAllSuperTypes(a1))
			.extracting(TypeReference::getQualifiedName).contains("Base").doesNotContain("I");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_type_the_index_does_not_hold_gets_the_same_answer(ApiBuilder builder) {
		var indexed = builder.build(HIERARCHY);
		var foreign = builder.build(HIERARCHY);

		var indexedA = assertClass(indexed, "A");
		var foreignA = assertClass(foreign, "A");

		// foreignA is an identical declaration from another snapshot, so it is resolved on demand rather than read
		// from the index: the two paths have to agree, down to the instantiated members
		assertThat(indexed.analyzer().getAllSuperTypes(foreignA))
			.isEqualTo(indexed.analyzer().getAllSuperTypes(indexedA));
		assertThat(indexed.analyzer().getExportedMethodsByErasure(foreignA))
			.isEqualTo(indexed.analyzer().getExportedMethodsByErasure(indexedA))
			.containsKey("get()")
			.containsKey("common()");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void resolved_members_cannot_be_altered_by_callers(ApiBuilder builder) {
		var api = builder.build(HIERARCHY);
		var a = assertClass(api, "A");

		// Every later query answers from what is resolved here, so handing out something mutable would let one caller
		// change what all the others see
		assertThatThrownBy(() -> api.analyzer().getExportedMethodsByErasure(a).clear())
			.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> api.analyzer().getExportedFieldsByName(a).clear())
			.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> api.analyzer().getAllMethodsByErasure(a).clear())
			.isInstanceOf(UnsupportedOperationException.class);
		assertThat(api.analyzer().getExportedMethodsByErasure(a)).containsKey("own()");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_member_type_is_exported_through_what_encloses_it(ApiBuilder builder) {
		var api = builder.build("""
			public final class Outer {
				public static class Visible { public void m() {} }
				protected static class OnlyForSubclasses { public void m() {} }
			}
			class Hidden { public static class Unreachable {} }""");

		// Outer is final, so no client can ever subclass it: what is protected inside it is out of reach, what is
		// public is not. Only the protected case depends on the enclosing type being subtypable.
		assertThat(api.findExportedType("Outer$Visible")).isPresent();
		assertThat(api.findExportedType("Outer$OnlyForSubclasses")).isEmpty();
		// And nothing nested inside a type clients cannot see is exported either
		assertThat(api.findExportedType("Hidden$Unreachable")).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_protected_member_type_is_exported_when_the_enclosing_type_can_be_subclassed(ApiBuilder builder) {
		var api = builder.build("""
			public class Outer {
				public Outer() {}
				protected static class ForSubclasses { public void m() {} }
			}""");

		assertThat(api.findExportedType("Outer$ForSubclasses")).isPresent();
	}
}
