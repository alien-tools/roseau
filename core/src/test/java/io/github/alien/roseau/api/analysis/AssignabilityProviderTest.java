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

class AssignabilityProviderTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void primitive_and_boxing_assignability(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		assertThat(api.isAssignable(m, PrimitiveTypeReference.INT, PrimitiveTypeReference.LONG)).isTrue();
		assertThat(api.isAssignable(m, PrimitiveTypeReference.LONG, PrimitiveTypeReference.INT)).isFalse();

		assertThat(api.isAssignable(m, PrimitiveTypeReference.INT, new TypeReference<>("java.lang.Integer"))).isTrue();
		assertThat(api.isAssignable(m, new TypeReference<>("java.lang.Integer"), PrimitiveTypeReference.INT)).isTrue();
		assertThat(api.isAssignable(m, new TypeReference<>("java.lang.Integer"), PrimitiveTypeReference.LONG)).isTrue();
		assertThat(api.isAssignable(m, new TypeReference<>("java.lang.Long"), PrimitiveTypeReference.INT)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void array_assignability(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		assertThat(api.isAssignable(m,
			new ArrayTypeReference(TypeReference.STRING, 1),
			new ArrayTypeReference(TypeReference.OBJECT, 1)))
			.isTrue();

		assertThat(api.isAssignable(m,
			new ArrayTypeReference(PrimitiveTypeReference.INT, 1),
			new ArrayTypeReference(TypeReference.OBJECT, 1)))
			.isFalse();

		assertThat(api.isAssignable(m,
			new ArrayTypeReference(PrimitiveTypeReference.INT, 1),
			TypeReference.OBJECT))
			.isTrue();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void generic_and_raw_assignability(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		var listOfString = new TypeReference<>("java.util.List", List.of(TypeReference.STRING));
		var rawList = new TypeReference<>("java.util.List");
		var listOfExtendsCharSequence = new TypeReference<>("java.util.List",
			List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.CharSequence")), true)));
		var listOfSuperInteger = new TypeReference<>("java.util.List",
			List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Integer")), false)));
		var listOfNumber = new TypeReference<>("java.util.List",
			List.of(new TypeReference<>("java.lang.Number")));

		assertThat(api.isAssignable(m, listOfString, rawList)).isTrue();
		assertThat(api.isAssignable(m, rawList, listOfString)).isTrue();
		assertThat(api.isAssignable(m, listOfString, listOfExtendsCharSequence)).isTrue();
		assertThat(api.isAssignable(m, listOfNumber, listOfSuperInteger)).isTrue();
		assertThat(api.isAssignable(m, listOfExtendsCharSequence, listOfString)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_assignability(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends Number> {
				public <U extends T> void m(U u, T t) {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m(java.lang.Number,java.lang.Number)");
		var u = new TypeParameterReference("U");
		var t = new TypeParameterReference("T");
		var number = new TypeReference<>("java.lang.Number");
		var string = TypeReference.STRING;

		assertThat(api.isAssignable(m, u, number)).isTrue();
		assertThat(api.isAssignable(m, string, t)).isFalse();
	}
}
