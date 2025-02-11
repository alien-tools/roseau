package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static com.github.maracas.roseau.utils.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

class GenericsExtractionTest {
	@Test
	void single_type_parameter() {
		var api = buildAPI("class A<T> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), is(equalTo(List.of(TypeReference.OBJECT))));
	}

	@Test
	void type_parameter_with_class_bound() {
		var api = buildAPI("class A<T extends String> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(1));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
	}

	@Test
	void type_parameter_with_interface_bound() {
		var api = buildAPI("class A<T extends Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(1));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@Test
	void type_parameter_with_several_bounds() {
		var api = buildAPI("class A<T extends String & Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(2));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
		assertThat(t.bounds().get(1).getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@Test
	void type_parameter_with_dependent_parameter_bound() {
		var api = buildAPI("class A<T, U extends T> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(2));

		var t = a.getFormalTypeParameters().getFirst();
		var u = a.getFormalTypeParameters().get(1);
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), is(equalTo(List.of(TypeReference.OBJECT))));
		assertThat(u.name(), is(equalTo("U")));
		assertThat(u.bounds(), hasSize(1));
		assertThat(u.bounds().getFirst().getQualifiedName(), is(equalTo("T")));
	}

	@Test
	void type_parameter_with_dependent_class_bound() {
		var api = buildAPI("""
      class X {}
      class A<T, U extends X> {}""");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(2));

		var t = a.getFormalTypeParameters().getFirst();
		var u = a.getFormalTypeParameters().get(1);
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), is(equalTo(List.of(TypeReference.OBJECT))));
		assertThat(u.name(), is(equalTo("U")));
		assertThat(u.bounds(), hasSize(1));
		assertThat(u.bounds().getFirst().getQualifiedName(), is(equalTo("X")));
	}

	@Test
	void type_parameter_bounded_references() {
		var api = buildAPI("""
			public class C {
				public List<?> m1() { return null; }
				public List<? extends Number> m2() { return null; }
				public List<? super Number> m3() { return null; }
				public void m4(java.util.List<?> p) {}
				public void m5(java.util.List<? extends Number> p) {}
				public void m6(java.util.List<? super Number> p) {}
			}""");

		var c = assertClass(api, "C");
		var m1 = assertMethod(c, "m1()");
		var m2 = assertMethod(c, "m2()");
		var m3 = assertMethod(c, "m3()");
		var m4 = assertMethod(c, "m4(java.util.List)");
		var m5 = assertMethod(c, "m5(java.util.List)");
		var m6 = assertMethod(c, "m6(java.util.List)");

		assertThat(m1.getType(), instanceOf(TypeReference.class));
		if (m1.getType() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst(), is(equalTo(TypeReference.OBJECT)));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m2.getType(), instanceOf(TypeReference.class));
		if (m2.getType() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m3.getType(), instanceOf(TypeReference.class));
		if (m3.getType() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(false));
			}
		}

		assertThat(m4.getParameters().getFirst().type(), instanceOf(TypeReference.class));
		if (m4.getParameters().getFirst().type() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst(), is(equalTo(TypeReference.OBJECT)));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m5.getParameters().getFirst().type(), instanceOf(TypeReference.class));
		if (m5.getParameters().getFirst().type() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m6.getParameters().getFirst().type(), instanceOf(TypeReference.class));
		if (m6.getParameters().getFirst().type() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(false));
			}
		}
	}

	@Test
	void throwable_generic() {
		var api = buildAPI("""
			public class A {
				public <X extends Throwable> void m() throws X {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(a, "m()");

		assertThat(m.getThrownExceptions().getFirst(), is(instanceOf(TypeParameterReference.class)));
	}

	@Test
	void method_type_parameter_resolution() {
		var api = buildAPI("""
			public class A<T extends String> {
				public class B<U extends Number> {
					public <V> void m(T t, U v, V u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(b, "m(java.lang.String,java.lang.Number,java.lang.Object)");
	}

	@Test
	void method_type_parameter_resolution_hiding() {
		var api = buildAPI("""
			public class A<T extends String> {
				public class B<T extends Number> {
					public <T> void m(T t, T v, T u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(b, "m(java.lang.Object,java.lang.Object,java.lang.Object)");
	}

	@Test
	void class_type_parameter_resolution() {
		var api = buildAPI("""
			public class A<T, U extends String, V extends Number> extends ArrayList<U> implements Supplier<V> {
				public V get() { return null; }
			}""");

		var a = assertClass(api, "A");
		var u = a.getFormalTypeParameters().get(1);
		var v = a.getFormalTypeParameters().get(2);
		var uRef = a.getSuperClass().getTypeArguments().getFirst();
		var vRef = a.getImplementedInterfaces().getFirst().getTypeArguments().getFirst();
		var get = assertMethod(a, "get()");
		var mvRef = get.getType();

		if (uRef instanceof TypeParameterReference tpr)
			assertThat(a.resolveTypeParameter(tpr).get(), is(equalTo(u)));
		else fail();

		if (vRef instanceof TypeParameterReference tpr)
			assertThat(a.resolveTypeParameter(tpr).get(), is(equalTo(v)));
		else fail();

		if (mvRef instanceof TypeParameterReference tpr)
			assertThat(get.resolveTypeParameter(tpr).get(), is(equalTo(v)));
		else fail();
	}

	@Test
	void llm_generated_generics() {
		var api = buildAPI("""
			public class ComplexGenerics<
				// Primary type parameters with complex bounds
				T extends Comparable<T> & Serializable,                        // Intersection type
				U extends ArrayList<Number> & Cloneable,                       // Class & interface bound
				V extends Map<String, ? super List<? extends T>>,              // Nested wildcards
				W extends Number & Comparable<? super W>                       // Class + interface with super
				> {
			
				// Fields with complex generic types
				private Map<? super List<? extends T>, ? extends Set<?>> nestedWildcards;
				private List<Map.Entry<U, ? extends Map<T, ?>>> nestedTypeArgs;
				private V complexDependentType;
				private U boundedField;
			
				// Generic constructor with independent type parameters
				public <S extends Map<? extends T, ? super U> & Cloneable> ComplexGenerics(S param) {}
			
				// Method with nested wildcards and type parameter bounds
				public <A extends List<? extends T>,
					B extends Map<A, ? super Set<? extends W>>>
				Map<A, B> complexMethod(A a, B b) {
					return null;
				}
			
				// Intersection type in method type parameter
				public <S extends Number & Comparable<? super S>>
				void intersectionTypeMethod(List<? extends S> numbers) {}
			
				// Recursive bound with nested type arguments
				public <X extends Comparable<X>>
				Map<X, List<? extends X>> recursiveBoundMethod(X input) {
					return null;
				}
			
				// Wildcard combinations with super/extends
				public void wildcardStorm(
					List<? super ArrayList<? extends T>> contravariantList,
					Set<? extends HashMap<? super W, ? extends U>> covariantSet) {}
			
				// Generic method with dependent type parameters
				public <K extends V, L extends Map<K, ? extends U>>
				L dependentTypesMethod(K key) {
					return null;
				}
			
				// Complex return type with multiple nesting
				public List<Map<? super Map.Entry<U, V>,
					? extends Set<List<? extends W>>>>
				ultimateReturnType() {
					return null;
				}
			
				// Static generic method with intersection type
				public static <N extends Object & Comparable<? super N>>
				N staticGenericMethod(N param) {
					return param;
				}
			
				// Method with type parameter in throws clause
				public <P extends Exception> void genericThrows() throws P {}
			
				// Nested generic class
				class InnerClass<Q extends Map<T, W>> {
					// Field using enclosing class's type parameters
					private Q nestedTypeField;
			
					// Method with nested type parameters
					public <R extends List<Q> & Serializable>
					Map<R, ? extends Q> innerMethod(R param) {
						return null;
					}
				}
			
				// Generic interface implementation
				interface GenericInterface<S extends Comparable<? super S>> {
					S method();
				}
			
				// Implementation with complex type argument
				private GenericInterface<? extends W> genericInterfaceImpl =
					() -> null;
			
				// Method with capture-generating wildcard combination
				public void captureHelper(List<?> wildcardList) {
					helper(wildcardList);
				}
			
				// Helper method to generate capture error
				private <C> void helper(List<C> typedList) {}
			
				// Generic field with nested wildcards
				private U nestedList;
				private V complexMap;
				private List<? super T> superList;
				private List<? extends T> extendsList;
			
				// Generic method with multiple bounds and nested wildcards
				public <X extends Comparable<X> & Serializable> void methodWithMultipleBounds(X value) {
					// Implementation omitted
				}
			
				// Method returning a wildcard type
				public List<? extends Number> getWildcardNumberList() {
					return new ArrayList<>();
				}
			
				// Generic method with dependent types
				public <A extends T, B extends List<A>> B dependentTypeMethod(A element) {
					return null; // Placeholder
				}
			
				// Intersection type in method parameter
				public void processIntersectionType(List<? extends Number> list, Map<String, ? super Integer> map) {
					// Implementation omitted
				}
			
				// Recursive generic bound: T must be comparable to its own type
				public <R extends Comparable<R>> R recursiveGenericMethod(R value) {
					return value;
				}
			
				// Method using multiple generics in parameters and return types
				public <K, W extends List<K>> W genericMethod(K key, W list) {
					return list;
				}
			
				// Nested type arguments with wildcards
				public Map<List<? extends T>, Set<? super U>> getNestedGenericTypes() {
					return new HashMap<>();
				}
			
				// Generic array workaround: Using List instead of direct generic arrays
				public <E> List<E>[] createGenericArray(int size) {
					return new List[size]; // Warning: Generic array creation
				}
			
				// A method with a wildcard capture scenario
				public void captureWildcard(List<?> unknownList) {
					helperMethod(unknownList);
				}
				private <E> void helperMethod(List<E> list) {
					// Capture the wildcard
				}
			
				// Factory method using generics
				public static <Z> GPTGenerics<?, ?, ?> createComplexInstance() {
					return null; // Placeholder
				}
			}""");

		// FIXME
	}
}
