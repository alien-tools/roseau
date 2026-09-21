package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers how the supertypes of a type and the members it exposes are resolved: type arguments travelling down a
 * hierarchy, members inherited from types the library does not export or does not even contain, and the invariants the
 * differ relies on when it looks those members up.
 */
class HierarchyProviderTest {
	private static final TypeReference<TypeDecl> STRING = new TypeReference<>("java.lang.String");
	private static final TypeReference<TypeDecl> NUMBER = new TypeReference<>("java.lang.Number");

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_arguments_travel_down_a_renaming_hierarchy(ApiBuilder builder) {
		var api = builder.build("""
			public interface Top<X> { X get(); }
			public interface Middle<Y> extends Top<Y> {}
			public abstract class A implements Middle<String> {}""");

		var a = assertClass(api, "A");

		// Every level renames its variable, so the argument A supplies has to travel through both substitutions
		assertThat(api.analyzer().getAllInstantiatedSuperTypes(new TypeReference<>("A")))
			.contains(new TypeReference<>("Middle", List.of(STRING)), new TypeReference<>("Top", List.of(STRING)));
		assertThat(api.analyzer().getExportedMethodsByErasure(a).get("get()").getType()).isEqualTo(STRING);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void the_supertypes_of_a_generic_type_are_instantiated_per_reference(ApiBuilder builder) {
		var api = builder.build("""
			public interface Box<T> { T get(); }
			public abstract class A<T> implements Box<T> {}""");

		// A's hierarchy is resolved once, in terms of its own type parameter, and instantiated for each reference to it
		assertThat(api.analyzer().getAllInstantiatedSuperTypes(new TypeReference<>("A", List.of(STRING))))
			.contains(new TypeReference<>("Box", List.of(STRING)));
		assertThat(api.analyzer().getAllInstantiatedSuperTypes(new TypeReference<>("A")))
			.contains(new TypeReference<>("Box", List.of(new TypeParameterReference("T"))));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_raw_supertype_supplies_no_type_argument(ApiBuilder builder) {
		var api = builder.build("""
			public interface Box<T extends Number> { T get(); }
			public abstract class A implements Box {}""");

		var a = assertClass(api, "A");

		// Nothing is supplied for T, so the inherited member keeps the variable rather than instantiating it
		assertThat(api.analyzer().getExportedMethodsByErasure(a).get("get()").getType())
			.isEqualTo(new TypeParameterReference("T"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_supertype_reached_through_several_paths_is_reported_once(ApiBuilder builder) {
		var api = builder.build("""
			public interface Top { void m(); }
			public interface Left extends Top {}
			public interface Right extends Top {}
			public abstract class A implements Left, Right {}""");

		var a = assertClass(api, "A");

		assertThat(api.analyzer().getAllSuperTypes(a))
			.extracting(TypeReference::getQualifiedName)
			.containsOnlyOnce("Top")
			.containsExactlyInAnyOrder("java.lang.Object", "Left", "Top", "Right");
		// And the method it declares is inherited once, whichever path it is reached through
		assertThat(api.analyzer().getExportedMethodsByErasure(a).get("m()").getContainingType().getQualifiedName())
			.isEqualTo("Top");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void the_most_concrete_implementation_of_a_method_wins(ApiBuilder builder) {
		var api = builder.build("""
			public interface Top { default String m() { return null; } }
			public abstract class Base implements Top {}
			public class A extends Base { public String m() { return null; } }""");

		var a = assertClass(api, "A");
		var base = assertClass(api, "Base");

		assertThat(api.analyzer().getExportedMethodsByErasure(a).get("m()").getContainingType().getQualifiedName())
			.isEqualTo("A");
		// Base overrides nothing, so it exposes the interface's default
		assertThat(api.analyzer().getExportedMethodsByErasure(base).get("m()").getContainingType().getQualifiedName())
			.isEqualTo("Top");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_shadowed_field_is_hidden_by_the_one_declared_below(ApiBuilder builder) {
		var api = builder.build("""
			public class Base { public String f; }
			public class A extends Base { public int f; }""");

		var a = assertClass(api, "A");

		assertThat(api.analyzer().getExportedFieldsByName(a).get("f").getType()).isEqualTo(PrimitiveTypeReference.INT);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_are_indexed_under_the_erasure_computed_for_their_owner(ApiBuilder builder) {
		var api = builder.build("""
			public interface Box<T extends Number> { T get(); void put(T t); }
			public class A implements Box<Integer> {
				public Integer get() { return null; }
				public void put(Integer i) {}
			}
			public class B<U extends Number> implements Box<U> {
				public U get() { return null; }
				public void put(U u) {}
			}
			public class C extends B<Integer> {}""");

		// The differ matches a member by the erasure the analyzer computes for its owner, and looks it up by the key it
		// is indexed under. The two have to agree, for inherited and instantiated members as much as declared ones.
		for (TypeDecl type : api.getExportedTypes()) {
			api.analyzer().getExportedMethodsByErasure(type).forEach((erasure, method) ->
				assertThat(api.analyzer().getErasure(type, method))
					.as("%s exposes %s", type.getQualifiedName(), erasure)
					.isEqualTo(erasure));
		}

		// And an inherited member is instantiated with the arguments its owner supplies, not left on the bound
		var c = assertClass(api, "C");
		assertThat(api.analyzer().getExportedMethodsByErasure(c)).containsKey("put(java.lang.Integer)");
		assertThat(api.analyzer().getExportedMethodsByErasure(c).get("get()").getType())
			.isEqualTo(new TypeReference<>("java.lang.Integer"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_are_inherited_from_supertypes_the_library_does_not_export(ApiBuilder builder) {
		var api = builder.build("""
			package p;
			class Base<T> { public T get() { return null; } }
			public class A extends Base<String> {}""");

		var a = assertClass(api, "p.A");

		// Base is package-private, so it is no part of the API — but what A inherits from it is
		assertThat(api.findExportedType("p.Base")).isEmpty();
		assertThat(api.analyzer().getExportedMethodsByErasure(a).get("get()").getType()).isEqualTo(STRING);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void hierarchies_of_types_outside_the_snapshot_are_resolved_too(ApiBuilder builder) {
		var api = builder.build("public class A {}");

		// java.util.ArrayList belongs to no snapshot, so nothing about it is indexed: it still has to resolve, and to
		// instantiate, exactly like a type of the library would
		var arrayList = api.analyzer().resolver().resolve(new TypeReference<>("java.util.ArrayList")).orElseThrow();
		assertThat(api.analyzer().getAllSuperTypes(arrayList))
			.extracting(TypeReference::getQualifiedName)
			.contains("java.util.List", "java.util.Collection", "java.lang.Iterable");
		assertThat(api.analyzer().getAllInstantiatedSuperTypes(
				new TypeReference<>("java.util.ArrayList", List.of(STRING))))
			.contains(new TypeReference<>("java.util.List", List.of(STRING)),
				new TypeReference<>("java.lang.Iterable", List.of(STRING)));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void a_bounded_type_variable_is_instantiated_through_the_classpath(ApiBuilder builder) {
		var api = builder.build("""
			public class A extends java.util.AbstractList<Number> {
				public Number get(int i) { return null; }
				public int size() { return 0; }
			}""");

		var a = assertClass(api, "A");

		// The argument A supplies has to reach the members AbstractList itself inherits from List and Collection
		assertThat(api.analyzer().getAllInstantiatedSuperTypes(new TypeReference<>("A")))
			.contains(new TypeReference<>("java.util.List", List.of(NUMBER)));
		assertThat(api.analyzer().getExportedMethodsByErasure(a))
			// add(E) is instantiated before its erasure is computed, so the key follows the argument...
			.containsKey("add(java.lang.Number)")
			.doesNotContainKey("add(java.lang.Object)")
			// ...while a parameter Collection really declares as Object is left alone
			.containsKey("contains(java.lang.Object)");
		assertThat(api.analyzer().getExportedMethodsByErasure(a).get("add(java.lang.Number)").getParameters().getFirst()
			.type()).isEqualTo(NUMBER);
	}
}
