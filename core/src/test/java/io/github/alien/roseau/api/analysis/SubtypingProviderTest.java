package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;

class SubtypingProviderTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void primitive_subtyping_is_identity_only(ApiBuilder builder) {
		var api = builder.build("public class C {}");
		var c = assertClass(api, "C");

		assertThat(api.isSubtypeOf(c, PrimitiveTypeReference.INT, PrimitiveTypeReference.INT)).isTrue();
		assertThat(api.isSubtypeOf(c, PrimitiveTypeReference.INT, PrimitiveTypeReference.LONG)).isFalse();
		assertThat(api.isSubtypeOf(c, PrimitiveTypeReference.LONG, PrimitiveTypeReference.INT)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void array_subtyping(ApiBuilder builder) {
		var api = builder.build("public class C {}");
		var c = assertClass(api, "C");

		assertThat(api.isSubtypeOf(c,
			new ArrayTypeReference(TypeReference.STRING, 1),
			new ArrayTypeReference(TypeReference.OBJECT, 1)))
			.isTrue();

		assertThat(api.isSubtypeOf(c,
			new ArrayTypeReference(PrimitiveTypeReference.INT, 1),
			new ArrayTypeReference(TypeReference.OBJECT, 1)))
			.isFalse();

		assertThat(api.isSubtypeOf(c,
			new ArrayTypeReference(TypeReference.STRING, 1),
			TypeReference.OBJECT))
			.isTrue();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void parameterized_subtyping_uses_instantiated_supertypes(ApiBuilder builder) {
		var api = builder.build("public class C {}");
		var c = assertClass(api, "C");

		var arrayListOfString = new TypeReference<>("java.util.ArrayList", List.of(TypeReference.STRING));
		var listOfString = new TypeReference<>("java.util.List", List.of(TypeReference.STRING));
		var listOfObject = new TypeReference<>("java.util.List", List.of(TypeReference.OBJECT));
		var listOfExtendsCharSequence = new TypeReference<>("java.util.List",
			List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.CharSequence")), true)));
		var listOfSuperInteger = new TypeReference<>("java.util.List",
			List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Integer")), false)));
		var listOfNumber = new TypeReference<>("java.util.List",
			List.of(new TypeReference<>("java.lang.Number")));

		assertThat(api.isSubtypeOf(c, arrayListOfString, listOfString)).isTrue();
		assertThat(api.isSubtypeOf(c, listOfString, listOfObject)).isFalse();
		assertThat(api.isSubtypeOf(c, listOfString, listOfExtendsCharSequence)).isTrue();
		assertThat(api.isSubtypeOf(c, listOfNumber, listOfSuperInteger)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void wildcard_containment(ApiBuilder builder) {
		var api = builder.build("public class C {}");
		var c = assertClass(api, "C");

		var superNumber = new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Number")), false);
		var superInteger = new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Integer")), false);

		assertThat(api.isSubtypeOf(c, superNumber, superInteger)).isTrue();
		assertThat(api.isSubtypeOf(c, superInteger, superNumber)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void scoped_type_parameter_subtyping(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends Number> {
				public <U extends T> void m(U u, T t) {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m(java.lang.Number,java.lang.Number)");
		var u = new TypeParameterReference("U");
		var number = new TypeReference<>("java.lang.Number");

		assertThat(api.isSubtypeOf(m, u, number)).isTrue();
		assertThat(api.isSubtypeOf(a, u, TypeReference.OBJECT)).isFalse();
	}
}
