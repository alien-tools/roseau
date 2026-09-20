package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiTestFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.github.alien.roseau.utils.ApiTestFactory.newMethod;
import static io.github.alien.roseau.utils.ApiTestFactory.newParameter;
import static io.github.alien.roseau.utils.ApiTestFactory.newVarargsParameter;
import static org.assertj.core.api.Assertions.assertThat;

class ExecutableDeclTest {
	static final TypeReference<TypeDecl> T = new TypeReference<>("pkg.T");

	@Test
	void signature_of_a_method_without_parameters() {
		var m = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID, List.of(), Set.of());

		assertThat(m.getSignature()).isEqualTo("m()");
		assertThat(m.getQualifiedSignature()).isEqualTo("pkg.T.m()");
	}

	@Test
	void signature_uses_qualified_parameter_types_and_ignores_parameter_names() {
		var m = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID,
			List.of(newParameter("s", TypeReference.STRING), newParameter("i", PrimitiveTypeReference.INT)), Set.of());
		var renamed = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID,
			List.of(newParameter("other", TypeReference.STRING), newParameter("j", PrimitiveTypeReference.INT)),
			Set.of());

		assertThat(m.getSignature()).isEqualTo("m(java.lang.String,int)");
		assertThat(renamed.getSignature()).isEqualTo(m.getSignature());
		assertThat(renamed).isEqualTo(m);
	}

	@Test
	void signature_does_not_erase_generics() {
		var listOfString = new TypeReference<>("java.util.List", List.of(TypeReference.STRING));
		var m = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID,
			List.of(newParameter("l", listOfString), newParameter("t", new TypeParameterReference("E"))), Set.of());

		assertThat(m.getSignature()).isEqualTo("m(java.util.List<java.lang.String>,E)");
	}

	@Test
	void varargs_and_array_parameters_share_a_signature_but_are_distinct_symbols() {
		var varargs = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID,
			List.of(newVarargsParameter("is", PrimitiveTypeReference.INT)), Set.of());
		var array = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID,
			List.of(newParameter("is", new ArrayTypeReference(PrimitiveTypeReference.INT, 1))), Set.of());

		assertThat(varargs.getSignature()).isEqualTo("m(int[])");
		assertThat(array.getSignature()).isEqualTo("m(int[])");
		assertThat(varargs.isVarargs()).isTrue();
		assertThat(array.isVarargs()).isFalse();
		assertThat(varargs).isNotEqualTo(array);
	}

	@Test
	void qualified_signature_of_a_member_of_a_nested_type() {
		var nested = new TypeReference<>("pkg.T$Inner");
		var m = newMethod("pkg.T$Inner.m", nested, PrimitiveTypeReference.VOID,
			List.of(newParameter("i", PrimitiveTypeReference.INT)), Set.of());

		assertThat(m.getQualifiedSignature()).isEqualTo("pkg.T$Inner.m(int)");
	}

	@Test
	void constructors_are_identified_by_their_parameters() {
		var c1 = ApiTestFactory.newConstructor("pkg.T.<init>", T, List.of());
		var c2 = ApiTestFactory.newConstructor("pkg.T.<init>", T,
			List.of(newParameter("i", PrimitiveTypeReference.INT)));

		assertThat(c1.getQualifiedSignature()).isEqualTo("pkg.T.<init>()");
		assertThat(c2.getQualifiedSignature()).isEqualTo("pkg.T.<init>(int)");
		assertThat(c1.isConstructor()).isTrue();
		assertThat(c1).isNotEqualTo(c2);
	}

	@Test
	void return_type_is_part_of_the_symbol_but_not_of_the_signature() {
		var returnsInt = newMethod("pkg.T.m", T, PrimitiveTypeReference.INT, List.of(), Set.of());
		var returnsVoid = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID, List.of(), Set.of());

		assertThat(returnsInt.getSignature()).isEqualTo(returnsVoid.getSignature());
		assertThat(returnsInt).isNotEqualTo(returnsVoid);
	}

	@Test
	void thrown_exceptions_are_part_of_the_symbol() {
		var plain = newMethod("pkg.T.m", T, PrimitiveTypeReference.VOID, List.of(), Set.of());
		var throwing = new MethodDecl("pkg.T.m", AccessModifier.PUBLIC, Set.of(), Set.of(),
			SourceLocation.NO_LOCATION, T, PrimitiveTypeReference.VOID, List.of(), List.of(),
			Set.of(TypeReference.IO_EXCEPTION));

		assertThat(throwing.getThrownExceptions()).containsExactly(TypeReference.IO_EXCEPTION);
		assertThat(plain).isNotEqualTo(throwing);
	}

	@Test
	void formal_type_parameters_are_part_of_the_symbol_but_not_of_the_signature() {
		var t = new TypeParameterReference("T");
		var unbounded = new MethodDecl("pkg.T.m", AccessModifier.PUBLIC, Set.of(), Set.of(),
			SourceLocation.NO_LOCATION, T, t, List.of(newParameter("t", t)),
			List.of(new FormalTypeParameter("T", List.of())), Set.of());
		var bounded = new MethodDecl("pkg.T.m", AccessModifier.PUBLIC, Set.of(), Set.of(),
			SourceLocation.NO_LOCATION, T, t, List.of(newParameter("t", t)),
			List.of(new FormalTypeParameter("T", List.of(TypeReference.STRING))), Set.of());

		assertThat(unbounded.getSignature()).isEqualTo("m(T)");
		assertThat(bounded.getSignature()).isEqualTo(unbounded.getSignature());
		assertThat(unbounded).isNotEqualTo(bounded);
	}
}
