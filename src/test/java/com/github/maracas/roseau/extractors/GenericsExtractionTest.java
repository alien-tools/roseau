package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;
import com.github.maracas.roseau.utils.ApiBuilder;
import com.github.maracas.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertField;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

class GenericsExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void single_type_parameter(ApiBuilder builder) {
		var api = builder.build("class A<T> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), is(equalTo(List.of(TypeReference.OBJECT))));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_class_bound(ApiBuilder builder) {
		var api = builder.build("class A<T extends String> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(1));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_interface_bound(ApiBuilder builder) {
		var api = builder.build("class A<T extends Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(1));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_several_bounds(ApiBuilder builder) {
		var api = builder.build("class A<T extends String & Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(2));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
		assertThat(t.bounds().get(1).getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_dependent_parameter_bound(ApiBuilder builder) {
		var api = builder.build("class A<T, U extends T> {}");

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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_dependent_class_bound(ApiBuilder builder) {
		var api = builder.build("""
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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_bounded_references(ApiBuilder builder) {
		var api = builder.build("""
			public class C {
				public java.util.List<?> m1() { return null; }
				public java.util.List<? extends Number> m2() { return null; }
				public java.util.List<? super Number> m3() { return null; }
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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void throwable_generic(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public <X extends Throwable> void m() throws X {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(a, "m()");

		assertThat(m.getThrownExceptions().getFirst(), is(instanceOf(TypeParameterReference.class)));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_type_parameter_resolution(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends String> {
				public class B<U extends Number> {
					public <V> void m(T t, U v, V u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(b, "m(java.lang.String,java.lang.Number,java.lang.Object)");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_type_parameter_resolution_hiding(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends String> {
				public class B<T extends Number> {
					public <T> void m(T t, T v, T u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(b, "m(java.lang.Object,java.lang.Object,java.lang.Object)");
	}

	// Had to have some fun with o3 ;)
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_type_parameter_resolution(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T, U extends String, V extends Number> extends java.util.ArrayList<U>
				implements java.util.function.Supplier<V> {
				public V get() { return null; }
			}""");

		var a = assertClass(api, "A");
		var u = a.getFormalTypeParameters().get(1);
		var v = a.getFormalTypeParameters().get(2);
		var uRef = a.getSuperClass().getTypeArguments().getFirst();
		var vRef = a.getImplementedInterfaces().getFirst().getTypeArguments().getFirst();
		var get = assertMethod(a, "get()");
		var mvRef = get.getType();

		if (uRef instanceof TypeParameterReference tpr) {
			assertThat(a.resolveTypeParameter(tpr).get(), is(equalTo(u)));
			assertThat(a.resolveTypeParameterBound(tpr).get().getQualifiedName(), is(equalTo("java.lang.String")));
		} else fail();

		if (vRef instanceof TypeParameterReference tpr) {
			assertThat(a.resolveTypeParameter(tpr).get(), is(equalTo(v)));
			assertThat(a.resolveTypeParameterBound(tpr).get().getQualifiedName(), is(equalTo("java.lang.Number")));
		} else fail();

		if (mvRef instanceof TypeParameterReference tpr) {
			assertThat(get.resolveTypeParameter(tpr).get(), is(equalTo(v)));
			assertThat(a.resolveTypeParameterBound(tpr).get().getQualifiedName(), is(equalTo("java.lang.Number")));
		} else fail();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void llm_generated_generics(ApiBuilder builder) {
		var api = builder.build("""
			public class ComplexGenerics<
				// Primary type parameters with complex bounds
				T extends Comparable<T> & java.io.Serializable,                        // Intersection type
				U extends java.util.ArrayList<Number> & Cloneable,                       // Class & interface bound
				V extends java.util.Map<String, ? super java.util.List<? extends T>>,              // Nested wildcards
				W extends Number & Comparable<? super W>                       // Class + interface with super
				> {
			
				// Fields with complex generic types
				public java.util.Map<? super java.util.List<? extends T>, ? extends java.util.Set<?>> nestedWildcards;
				public java.util.List<java.util.Map.Entry<U, ? extends java.util.Map<T, ?>>> nestedTypeArgs;
				public V complexDependentType;
				public U boundedField;
			
				// Generic constructor with independent type parameters
				public <S extends java.util.Map<? extends T, ? super U> & Cloneable> ComplexGenerics(S param) {}
			
				// Method with nested wildcards and type parameter bounds
				public <A extends java.util.List<? extends T>,
					B extends java.util.Map<A, ? super java.util.Set<? extends W>>>
				java.util.Map<A, B> complexMethod(A a, B b) {
					return null;
				}
			
				// Intersection type in method type parameter
				public <S extends Number & Comparable<? super S>>
				void intersectionTypeMethod(java.util.List<? extends S> numbers) {}
			
				// Recursive bound with nested type arguments
				public <X extends Comparable<X>>
				java.util.Map<X, java.util.List<? extends X>> recursiveBoundMethod(X input) {
					return null;
				}
			
				// Wildcard combinations with super/extends
				public void wildcardStorm(
					java.util.List<? super java.util.ArrayList<? extends T>> contravariantList,
					java.util.Set<? extends java.util.HashMap<? super W, ? extends U>> covariantSet) {}
			
				// Generic method with dependent type parameters
				public <K extends V, L extends java.util.Map<K, ? extends U>>
				L dependentTypesMethod(K key) {
					return null;
				}
			
				// Complex return type with multiple nesting
				public java.util.List<java.util.Map<? super java.util.Map.Entry<U, V>,
					? extends java.util.Set<java.util.List<? extends W>>>>
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
				class InnerClass<Q extends java.util.Map<T, W>> {
					// Field using enclosing class's type parameters
					public Q nestedTypeField;
			
					// Method with nested type parameters
					public <R extends java.util.List<Q> & java.io.Serializable>
					java.util.Map<R, ? extends Q> innerMethod(R param) {
						return null;
					}
				}
			
				// Generic interface implementation
				interface GenericInterface<S extends Comparable<? super S>> {
					S method();
				}
			
				// Implementation with complex type argument
				public GenericInterface<? extends W> genericInterfaceImpl =
					() -> null;
			
				// Method with capture-generating wildcard combination
				public void captureHelper(java.util.List<?> wildcardList) {
					helper(wildcardList);
				}
			
				// Helper method to generate capture error
				public <C> void helper(java.util.List<C> typedList) {}
			
				// Generic field with nested wildcards
				public U nestedList;
				public V complexMap;
				public java.util.List<? super T> superList;
				public java.util.List<? extends T> extendsList;
			
				// Generic method with multiple bounds and nested wildcards
				public <X extends Comparable<X> & java.io.Serializable> void methodWithMultipleBounds(X value) {
					// Implementation omitted
				}
			
				// Method returning a wildcard type
				public java.util.List<? extends Number> getWildcardNumberList() {
					return new java.util.ArrayList<>();
				}
			
				// Generic method with dependent types
				public <A extends T, B extends java.util.List<A>> B dependentTypeMethod(A element) {
					return null; // Placeholder
				}
			
				// Intersection type in method parameter
				public void processIntersectionType(java.util.List<? extends Number> list, java.util.Map<String, ? super Integer> map) {
					// Implementation omitted
				}
			
				// Recursive generic bound: T must be comparable to its own type
				public <R extends Comparable<R>> R recursiveGenericMethod(R value) {
					return value;
				}
			
				// Method using multiple generics in parameters and return types
				public <K, W extends java.util.List<K>> W genericMethod(K key, W list) {
					return list;
				}
			
				// Nested type arguments with wildcards
				public java.util.Map<java.util.List<? extends T>, java.util.Set<? super U>> getNestedGenericTypes() {
					return new java.util.HashMap<>();
				}
			
				// Generic array workaround: Using List instead of direct generic arrays
				public <E> java.util.List<E>[] createGenericArray(int size) {
					return new java.util.List[size]; // Warning: Generic array creation
				}
			
				// A method with a wildcard capture scenario
				public void captureWildcard(java.util.List<?> unknownList) {
					helperMethod(unknownList);
				}
				public <E> void helperMethod(java.util.List<E> list) {
					// Capture the wildcard
				}
			
				// Factory method using generics
				public static <Z> ComplexGenerics<?, ?, ?, ?> createComplexInstance() {
					return null; // Placeholder
				}
			}""");

		var cg = assertClass(api, "ComplexGenerics");
		assertThat(cg.getFormalTypeParameters(), hasSize(4));

		// T: T extends Comparable<T> & Serializable
		var tParam = cg.getFormalTypeParameters().get(0);
		assertThat(tParam.name(), is(equalTo("T")));
		assertThat(tParam.bounds(), hasSize(2));
		var compT = tParam.bounds().get(0);
		assertThat(compT.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) compT).getTypeArguments(), hasSize(1));
		var compTArg = ((TypeReference<?>) compT).getTypeArguments().get(0);
		assertThat(compTArg, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) compTArg).getQualifiedName(), is(equalTo("T")));
		var serT = tParam.bounds().get(1);
		assertThat(serT.getQualifiedName(), is(equalTo("java.io.Serializable")));
		assertThat(((TypeReference<?>) serT).getTypeArguments(), hasSize(0));

		// U: U extends ArrayList<Number> & Cloneable
		var uParam = cg.getFormalTypeParameters().get(1);
		assertThat(uParam.name(), is(equalTo("U")));
		assertThat(uParam.bounds(), hasSize(2));
		var arrayListBound = uParam.bounds().get(0);
		assertThat(arrayListBound.getQualifiedName(), is(equalTo("java.util.ArrayList")));
		assertThat(((TypeReference<?>) arrayListBound).getTypeArguments(), hasSize(1));
		var numberArg = ((TypeReference<?>) arrayListBound).getTypeArguments().get(0);
		assertThat(numberArg.getQualifiedName(), is(equalTo("java.lang.Number")));
		var cloneableBound = uParam.bounds().get(1);
		assertThat(cloneableBound.getQualifiedName(), is(equalTo("java.lang.Cloneable")));
		assertThat(((TypeReference<?>) cloneableBound).getTypeArguments(), hasSize(0));

		// V: V extends Map<String, ? super List<? extends T>>
		var vParam = cg.getFormalTypeParameters().get(2);
		assertThat(vParam.name(), is(equalTo("V")));
		assertThat(vParam.bounds(), hasSize(1));
		var mapBoundV = vParam.bounds().get(0);
		assertThat(mapBoundV.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) mapBoundV).getTypeArguments(), hasSize(2));
		var strArg = ((TypeReference<?>) mapBoundV).getTypeArguments().get(0);
		assertThat(strArg.getQualifiedName(), is(equalTo("java.lang.String")));
		var wildcardV = ((TypeReference<?>) mapBoundV).getTypeArguments().get(1);
		assertThat(wildcardV, instanceOf(WildcardTypeReference.class));
		var wcV = (WildcardTypeReference) wildcardV;
		assertThat(wcV.upper(), is(false));
		assertThat(wcV.bounds(), hasSize(1));
		var listBoundV = wcV.bounds().get(0);
		assertThat(listBoundV.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) listBoundV).getTypeArguments(), hasSize(1));
		var innerWildcardV = ((TypeReference<?>) listBoundV).getTypeArguments().get(0);
		assertThat(innerWildcardV, instanceOf(WildcardTypeReference.class));
		var wcInnerV = (WildcardTypeReference) innerWildcardV;
		assertThat(wcInnerV.upper(), is(true));
		assertThat(wcInnerV.bounds(), hasSize(1));
		var tInV = wcInnerV.bounds().get(0);
		assertThat(tInV, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) tInV).getQualifiedName(), is(equalTo("T")));

		// W: W extends Number & Comparable<? super W>
		var wParam = cg.getFormalTypeParameters().get(3);
		assertThat(wParam.name(), is(equalTo("W")));
		assertThat(wParam.bounds(), hasSize(2));
		var numberBoundW = wParam.bounds().get(0);
		assertThat(numberBoundW.getQualifiedName(), is(equalTo("java.lang.Number")));
		assertThat(((TypeReference<?>) numberBoundW).getTypeArguments(), hasSize(0));
		var compW = wParam.bounds().get(1);
		assertThat(compW.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) compW).getTypeArguments(), hasSize(1));
		var wildcardW = ((TypeReference<?>) compW).getTypeArguments().get(0);
		assertThat(wildcardW, instanceOf(WildcardTypeReference.class));
		var wcW = (WildcardTypeReference) wildcardW;
		assertThat(wcW.upper(), is(false));
		assertThat(wcW.bounds(), hasSize(1));
		var wBoundInW = wcW.bounds().get(0);
		assertThat(wBoundInW, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) wBoundInW).getQualifiedName(), is(equalTo("W")));

		// Check superclass and interfaces
		assertThat(cg.getSuperClass().getQualifiedName(), is(equalTo("java.lang.Object")));
		assertThat(cg.getImplementedInterfaces(), hasSize(0));

		// Field: private Map<? super List<? extends T>, ? extends Set<?>> nestedWildcards;
		var nestedWildcards = assertField(cg, "nestedWildcards");
		var nwType = nestedWildcards.getType();
		assertThat(nwType.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) nwType).getTypeArguments(), hasSize(2));
		// First type argument: wildcard with lower bound List<? extends T>
		var nwArg1 = ((TypeReference<?>) nwType).getTypeArguments().get(0);
		assertThat(nwArg1, instanceOf(WildcardTypeReference.class));
		var nwWc1 = (WildcardTypeReference) nwArg1;
		assertThat(nwWc1.upper(), is(false));
		assertThat(nwWc1.bounds(), hasSize(1));
		var listTypeNW = nwWc1.bounds().get(0);
		assertThat(listTypeNW.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) listTypeNW).getTypeArguments(), hasSize(1));
		var listArgNW = ((TypeReference<?>) listTypeNW).getTypeArguments().get(0);
		assertThat(listArgNW, instanceOf(WildcardTypeReference.class));
		var listWcNW = (WildcardTypeReference) listArgNW;
		assertThat(listWcNW.upper(), is(true));
		assertThat(listWcNW.bounds(), hasSize(1));
		var tInNW = listWcNW.bounds().get(0);
		assertThat(tInNW, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) tInNW).getQualifiedName(), is(equalTo("T")));
		// Second type argument: wildcard with upper bound Set<?>
		var nwArg2 = ((TypeReference<?>) nwType).getTypeArguments().get(1);
		assertThat(nwArg2, instanceOf(WildcardTypeReference.class));
		var nwWc2 = (WildcardTypeReference) nwArg2;
		assertThat(nwWc2.upper(), is(true));
		assertThat(nwWc2.bounds(), hasSize(1));
		var setTypeNW = nwWc2.bounds().get(0);
		assertThat(setTypeNW.getQualifiedName(), is(equalTo("java.util.Set")));
		assertThat(((TypeReference<?>) setTypeNW).getTypeArguments(), hasSize(1));
		var setArgNW = ((TypeReference<?>) setTypeNW).getTypeArguments().get(0);
		assertThat(setArgNW, instanceOf(WildcardTypeReference.class));
		var setWcNW = (WildcardTypeReference) setArgNW;
		assertThat(setWcNW.upper(), is(true));
		assertThat(setWcNW.bounds(), hasSize(1));
		assertThat(setWcNW.bounds().get(0), is(equalTo(TypeReference.OBJECT)));

		// Field: private List<Map.Entry<U, ? extends Map<T, ?>>> nestedTypeArgs;
		var nestedTypeArgs = assertField(cg, "nestedTypeArgs");
		var ntaType = nestedTypeArgs.getType();
		assertThat(ntaType.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) ntaType).getTypeArguments(), hasSize(1));
		var entryType = ((TypeReference<?>) ntaType).getTypeArguments().get(0);
		assertThat(entryType.getQualifiedName(), is(equalTo("java.util.Map$Entry")));
		assertThat(((TypeReference<?>) entryType).getTypeArguments(), hasSize(2));
		// First argument: U
		var entryArg1 = ((TypeReference<?>) entryType).getTypeArguments().get(0);
		assertThat(entryArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) entryArg1).getQualifiedName(), is(equalTo("U")));
		// Second argument: wildcard with upper bound Map<T, ?>
		var entryArg2 = ((TypeReference<?>) entryType).getTypeArguments().get(1);
		assertThat(entryArg2, instanceOf(WildcardTypeReference.class));
		var entryWc = (WildcardTypeReference) entryArg2;
		assertThat(entryWc.upper(), is(true));
		assertThat(entryWc.bounds(), hasSize(1));
		var mapInner = entryWc.bounds().get(0);
		assertThat(mapInner.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) mapInner).getTypeArguments(), hasSize(2));
		var mapInnerArg1 = ((TypeReference<?>) mapInner).getTypeArguments().get(0);
		assertThat(mapInnerArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) mapInnerArg1).getQualifiedName(), is(equalTo("T")));
		var mapInnerArg2 = ((TypeReference<?>) mapInner).getTypeArguments().get(1);
		assertThat(mapInnerArg2, instanceOf(WildcardTypeReference.class));
		var innerMapWc = (WildcardTypeReference) mapInnerArg2;
		assertThat(innerMapWc.upper(), is(true));
		assertThat(innerMapWc.bounds(), hasSize(1));
		assertThat(innerMapWc.bounds().get(0), is(equalTo(TypeReference.OBJECT)));

		// Field: private V complexDependentType;
		var complexDependentType = assertField(cg, "complexDependentType");
		var cdtType = complexDependentType.getType();
		assertThat(cdtType, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) cdtType).getQualifiedName(), is(equalTo("V")));

		// Field: private U boundedField;
		var boundedField = assertField(cg, "boundedField");
		var bfType = boundedField.getType();
		assertThat(bfType, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) bfType).getQualifiedName(), is(equalTo("U")));

		// Field: private GenericInterface<? extends W> genericInterfaceImpl;
		var genericInterfaceImpl = assertField(cg, "genericInterfaceImpl");
		var giType = genericInterfaceImpl.getType();
		assertThat(giType.getQualifiedName(), is(equalTo("ComplexGenerics$GenericInterface")));
		assertThat(((TypeReference<?>) giType).getTypeArguments(), hasSize(1));
		var giArg = ((TypeReference<?>) giType).getTypeArguments().get(0);
		assertThat(giArg, instanceOf(WildcardTypeReference.class));
		var giWc = (WildcardTypeReference) giArg;
		assertThat(giWc.upper(), is(true));
		assertThat(giWc.bounds(), hasSize(1));
		assertThat(giWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) giWc.bounds().get(0)).getQualifiedName(), is(equalTo("W")));

		// Field: private U nestedList;
		var nestedListField = assertField(cg, "nestedList");
		var nlType = nestedListField.getType();
		assertThat(nlType, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) nlType).getQualifiedName(), is(equalTo("U")));

		// Field: private V complexMap;
		var complexMapField = assertField(cg, "complexMap");
		var cmType = complexMapField.getType();
		assertThat(cmType, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) cmType).getQualifiedName(), is(equalTo("V")));

		// Field: private List<? super T> superList;
		var superListField = assertField(cg, "superList");
		var slType = superListField.getType();
		assertThat(slType.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) slType).getTypeArguments(), hasSize(1));
		var slArg = ((TypeReference<?>) slType).getTypeArguments().get(0);
		assertThat(slArg, instanceOf(WildcardTypeReference.class));
		var slWc = (WildcardTypeReference) slArg;
		assertThat(slWc.upper(), is(false));
		assertThat(slWc.bounds(), hasSize(1));
		assertThat(slWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) slWc.bounds().get(0)).getQualifiedName(), is(equalTo("T")));

		// Field: private List<? extends T> extendsList;
		var extendsListField = assertField(cg, "extendsList");
		var elType = extendsListField.getType();
		assertThat(elType.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) elType).getTypeArguments(), hasSize(1));
		var elArg = ((TypeReference<?>) elType).getTypeArguments().get(0);
		assertThat(elArg, instanceOf(WildcardTypeReference.class));
		var elWc = (WildcardTypeReference) elArg;
		assertThat(elWc.upper(), is(true));
		assertThat(elWc.bounds(), hasSize(1));
		assertThat(elWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) elWc.bounds().get(0)).getQualifiedName(), is(equalTo("T")));

		// Constructor: public <S extends Map<? extends T, ? super U> & Cloneable> ComplexGenerics(S param)
		var ctors = cg.getConstructors();
		assertThat(ctors, hasSize(1));
		var ctor = ctors.get(0);
		assertThat(ctor.getFormalTypeParameters(), hasSize(1));
		var sParamCtor = ctor.getFormalTypeParameters().get(0);
		assertThat(sParamCtor.name(), is(equalTo("S")));
		assertThat(sParamCtor.bounds(), hasSize(2));
		var mapBoundS = sParamCtor.bounds().get(0);
		assertThat(mapBoundS.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) mapBoundS).getTypeArguments(), hasSize(2));
		var sArg1 = ((TypeReference<?>) mapBoundS).getTypeArguments().get(0);
		assertThat(sArg1, instanceOf(WildcardTypeReference.class));
		var sWc1 = (WildcardTypeReference) sArg1;
		assertThat(sWc1.upper(), is(true));
		assertThat(sWc1.bounds(), hasSize(1));
		assertThat(sWc1.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) sWc1.bounds().get(0)).getQualifiedName(), is(equalTo("T")));
		var sArg2 = ((TypeReference<?>) mapBoundS).getTypeArguments().get(1);
		assertThat(sArg2, instanceOf(WildcardTypeReference.class));
		var sWc2 = (WildcardTypeReference) sArg2;
		assertThat(sWc2.upper(), is(false));
		assertThat(sWc2.bounds(), hasSize(1));
		assertThat(sWc2.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) sWc2.bounds().get(0)).getQualifiedName(), is(equalTo("U")));
		var cloneBoundS = sParamCtor.bounds().get(1);
		assertThat(cloneBoundS.getQualifiedName(), is(equalTo("java.lang.Cloneable")));
		assertThat(((TypeReference<?>) cloneBoundS).getTypeArguments(), hasSize(0));
		assertThat(ctor.getParameters(), hasSize(1));
		var ctorParam = ctor.getParameters().get(0);
		assertThat(ctorParam.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) ctorParam.type()).getQualifiedName(), is(equalTo("S")));

		// Method: public <A extends List<? extends T>, B extends Map<A, ? super Set<? extends W>>> Map<A, B> complexMethod(A a, B b)
		var complexMethod = assertMethod(cg, "complexMethod(java.util.List,java.util.Map)");
		assertThat(complexMethod.getFormalTypeParameters(), hasSize(2));
		var aParam = complexMethod.getFormalTypeParameters().get(0);
		assertThat(aParam.name(), is(equalTo("A")));
		assertThat(aParam.bounds(), hasSize(1));
		var listBoundA = aParam.bounds().get(0);
		assertThat(listBoundA.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) listBoundA).getTypeArguments(), hasSize(1));
		var wcA = ((TypeReference<?>) listBoundA).getTypeArguments().get(0);
		assertThat(wcA, instanceOf(WildcardTypeReference.class));
		var wcARef = (WildcardTypeReference) wcA;
		assertThat(wcARef.upper(), is(true));
		assertThat(wcARef.bounds(), hasSize(1));
		assertThat(wcARef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) wcARef.bounds().get(0)).getQualifiedName(), is(equalTo("T")));
		var bParam = complexMethod.getFormalTypeParameters().get(1);
		assertThat(bParam.name(), is(equalTo("B")));
		assertThat(bParam.bounds(), hasSize(1));
		var mapBoundB = bParam.bounds().get(0);
		assertThat(mapBoundB.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) mapBoundB).getTypeArguments(), hasSize(2));
		var mapBArg1 = ((TypeReference<?>) mapBoundB).getTypeArguments().get(0);
		assertThat(mapBArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) mapBArg1).getQualifiedName(), is(equalTo("A")));
		var mapBArg2 = ((TypeReference<?>) mapBoundB).getTypeArguments().get(1);
		assertThat(mapBArg2, instanceOf(WildcardTypeReference.class));
		var mapBWc = (WildcardTypeReference) mapBArg2;
		assertThat(mapBWc.upper(), is(false));
		assertThat(mapBWc.bounds(), hasSize(1));
		var setBound = mapBWc.bounds().get(0);
		assertThat(setBound.getQualifiedName(), is(equalTo("java.util.Set")));
		assertThat(((TypeReference<?>) setBound).getTypeArguments(), hasSize(1));
		var setArg = ((TypeReference<?>) setBound).getTypeArguments().get(0);
		assertThat(setArg, instanceOf(WildcardTypeReference.class));
		var setWc = (WildcardTypeReference) setArg;
		assertThat(setWc.upper(), is(true));
		assertThat(setWc.bounds(), hasSize(1));
		assertThat(setWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) setWc.bounds().get(0)).getQualifiedName(), is(equalTo("W")));
		// Return type: Map<A, B>
		var cmRet = complexMethod.getType();
		assertThat(cmRet.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) cmRet).getTypeArguments(), hasSize(2));
		var retArg1 = ((TypeReference<?>) cmRet).getTypeArguments().get(0);
		assertThat(retArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) retArg1).getQualifiedName(), is(equalTo("A")));
		var retArg2 = ((TypeReference<?>) cmRet).getTypeArguments().get(1);
		assertThat(retArg2, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) retArg2).getQualifiedName(), is(equalTo("B")));
		// Parameters: A and B
		assertThat(complexMethod.getParameters(), hasSize(2));
		var cmParam1 = complexMethod.getParameters().get(0);
		assertThat(cmParam1.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) cmParam1.type()).getQualifiedName(), is(equalTo("A")));
		var cmParam2 = complexMethod.getParameters().get(1);
		assertThat(cmParam2.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) cmParam2.type()).getQualifiedName(), is(equalTo("B")));

		// Method: public <S extends Number & Comparable<? super S>> void intersectionTypeMethod(List<? extends S> numbers)
		var intersectionMethod = assertMethod(cg, "intersectionTypeMethod(java.util.List)");
		assertThat(intersectionMethod.getFormalTypeParameters(), hasSize(1));
		var sParamMethod = intersectionMethod.getFormalTypeParameters().get(0);
		assertThat(sParamMethod.name(), is(equalTo("S")));
		assertThat(sParamMethod.bounds(), hasSize(2));
		var numBoundS = sParamMethod.bounds().get(0);
		assertThat(numBoundS.getQualifiedName(), is(equalTo("java.lang.Number")));
		var compBoundS = sParamMethod.bounds().get(1);
		assertThat(compBoundS.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) compBoundS).getTypeArguments(), hasSize(1));
		var sWildcard = ((TypeReference<?>) compBoundS).getTypeArguments().get(0);
		assertThat(sWildcard, instanceOf(WildcardTypeReference.class));
		var sWc = (WildcardTypeReference) sWildcard;
		assertThat(sWc.upper(), is(false));
		assertThat(sWc.bounds(), hasSize(1));
		assertThat(sWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) sWc.bounds().get(0)).getQualifiedName(), is(equalTo("S")));
		var intMethodParam = intersectionMethod.getParameters().get(0);
		assertThat(intMethodParam.type().getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) intMethodParam.type()).getTypeArguments(), hasSize(1));
		var intWc = ((TypeReference<?>) intMethodParam.type()).getTypeArguments().get(0);
		assertThat(intWc, instanceOf(WildcardTypeReference.class));
		var intWcRef = (WildcardTypeReference) intWc;
		assertThat(intWcRef.upper(), is(true));
		assertThat(intWcRef.bounds(), hasSize(1));
		assertThat(intWcRef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) intWcRef.bounds().get(0)).getQualifiedName(), is(equalTo("S")));

		// Method: public <X extends Comparable<X>> Map<X, List<? extends X>> recursiveBoundMethod(X input)
		var recursiveMethod = assertMethod(cg, "recursiveBoundMethod(java.lang.Comparable)");
		assertThat(recursiveMethod.getFormalTypeParameters(), hasSize(1));
		var xParam = recursiveMethod.getFormalTypeParameters().get(0);
		assertThat(xParam.name(), is(equalTo("X")));
		assertThat(xParam.bounds(), hasSize(1));
		var compX = xParam.bounds().get(0);
		assertThat(compX.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) compX).getTypeArguments(), hasSize(1));
		var xArg = ((TypeReference<?>) compX).getTypeArguments().get(0);
		assertThat(xArg, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) xArg).getQualifiedName(), is(equalTo("X")));
		var recParam = recursiveMethod.getParameters().get(0);
		assertThat(recParam.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) recParam.type()).getQualifiedName(), is(equalTo("X")));
		var recRet = recursiveMethod.getType();
		assertThat(recRet.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) recRet).getTypeArguments(), hasSize(2));
		var recRetArg1 = ((TypeReference<?>) recRet).getTypeArguments().get(0);
		assertThat(recRetArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) recRetArg1).getQualifiedName(), is(equalTo("X")));
		var recRetArg2 = ((TypeReference<?>) recRet).getTypeArguments().get(1);
		assertThat(recRetArg2.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) recRetArg2).getTypeArguments(), hasSize(1));
		var recListWc = ((TypeReference<?>) recRetArg2).getTypeArguments().get(0);
		assertThat(recListWc, instanceOf(WildcardTypeReference.class));
		var recListWcRef = (WildcardTypeReference) recListWc;
		assertThat(recListWcRef.upper(), is(true));
		assertThat(recListWcRef.bounds(), hasSize(1));
		assertThat(recListWcRef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) recListWcRef.bounds().get(0)).getQualifiedName(), is(equalTo("X")));

		// Method: public void wildcardStorm(List<? super ArrayList<? extends T>> contravariantList, Set<? extends HashMap<? super W, ? extends U>> covariantSet)
		var wildcardStorm = assertMethod(cg, "wildcardStorm(java.util.List,java.util.Set)");
		assertThat(wildcardStorm.getParameters(), hasSize(2));
		// First parameter
		var wsParam1 = wildcardStorm.getParameters().get(0);
		assertThat(wsParam1.type().getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) wsParam1.type()).getTypeArguments(), hasSize(1));
		var wsArg1 = ((TypeReference<?>) wsParam1.type()).getTypeArguments().get(0);
		assertThat(wsArg1, instanceOf(WildcardTypeReference.class));
		var wsWc1 = (WildcardTypeReference) wsArg1;
		assertThat(wsWc1.upper(), is(false));
		assertThat(wsWc1.bounds(), hasSize(1));
		var arrayListType = wsWc1.bounds().get(0);
		assertThat(arrayListType.getQualifiedName(), is(equalTo("java.util.ArrayList")));
		assertThat(((TypeReference<?>) arrayListType).getTypeArguments(), hasSize(1));
		var alWc = ((TypeReference<?>) arrayListType).getTypeArguments().get(0);
		assertThat(alWc, instanceOf(WildcardTypeReference.class));
		var alWcRef = (WildcardTypeReference) alWc;
		assertThat(alWcRef.upper(), is(true));
		assertThat(alWcRef.bounds(), hasSize(1));
		assertThat(alWcRef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) alWcRef.bounds().get(0)).getQualifiedName(), is(equalTo("T")));
		// Second parameter
		var wsParam2 = wildcardStorm.getParameters().get(1);
		assertThat(wsParam2.type().getQualifiedName(), is(equalTo("java.util.Set")));
		assertThat(((TypeReference<?>) wsParam2.type()).getTypeArguments(), hasSize(1));
		var wsArg2 = ((TypeReference<?>) wsParam2.type()).getTypeArguments().get(0);
		assertThat(wsArg2, instanceOf(WildcardTypeReference.class));
		var wsWc2 = (WildcardTypeReference) wsArg2;
		assertThat(wsWc2.upper(), is(true));
		assertThat(wsWc2.bounds(), hasSize(1));
		var hashMapType = wsWc2.bounds().get(0);
		assertThat(hashMapType.getQualifiedName(), is(equalTo("java.util.HashMap")));
		assertThat(((TypeReference<?>) hashMapType).getTypeArguments(), hasSize(2));
		var hmArg1 = ((TypeReference<?>) hashMapType).getTypeArguments().get(0);
		assertThat(hmArg1, instanceOf(WildcardTypeReference.class));
		var hmWc1 = (WildcardTypeReference) hmArg1;
		assertThat(hmWc1.upper(), is(false));
		assertThat(hmWc1.bounds(), hasSize(1));
		assertThat(hmWc1.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) hmWc1.bounds().get(0)).getQualifiedName(), is(equalTo("W")));
		var hmArg2 = ((TypeReference<?>) hashMapType).getTypeArguments().get(1);
		assertThat(hmArg2, instanceOf(WildcardTypeReference.class));
		var hmWc2 = (WildcardTypeReference) hmArg2;
		assertThat(hmWc2.upper(), is(true));
		assertThat(hmWc2.bounds(), hasSize(1));
		assertThat(hmWc2.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) hmWc2.bounds().get(0)).getQualifiedName(), is(equalTo("U")));

		// Method: public <K extends V, L extends Map<K, ? extends U>> L dependentTypesMethod(K key)
		var dependentTypesMethod = assertMethod(cg, "dependentTypesMethod(java.util.Map)");
		assertThat(dependentTypesMethod.getFormalTypeParameters(), hasSize(2));
		var kParam = dependentTypesMethod.getFormalTypeParameters().get(0);
		assertThat(kParam.name(), is(equalTo("K")));
		assertThat(kParam.bounds(), hasSize(1));
		var kBound = kParam.bounds().get(0);
		assertThat(kBound, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) kBound).getQualifiedName(), is(equalTo("V")));
		var lParam = dependentTypesMethod.getFormalTypeParameters().get(1);
		assertThat(lParam.name(), is(equalTo("L")));
		assertThat(lParam.bounds(), hasSize(1));
		var lBound = lParam.bounds().get(0);
		assertThat(lBound.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) lBound).getTypeArguments(), hasSize(2));
		var lBoundArg1 = ((TypeReference<?>) lBound).getTypeArguments().get(0);
		assertThat(lBoundArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) lBoundArg1).getQualifiedName(), is(equalTo("K")));
		var lBoundArg2 = ((TypeReference<?>) lBound).getTypeArguments().get(1);
		assertThat(lBoundArg2, instanceOf(WildcardTypeReference.class));
		var lBoundWc = (WildcardTypeReference) lBoundArg2;
		assertThat(lBoundWc.upper(), is(true));
		assertThat(lBoundWc.bounds(), hasSize(1));
		assertThat(lBoundWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) lBoundWc.bounds().get(0)).getQualifiedName(), is(equalTo("U")));
		assertThat(dependentTypesMethod.getParameters(), hasSize(1));
		var depParam = dependentTypesMethod.getParameters().get(0);
		assertThat(depParam.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) depParam.type()).getQualifiedName(), is(equalTo("K")));
		assertThat(dependentTypesMethod.getType(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) dependentTypesMethod.getType()).getQualifiedName(), is(equalTo("L")));

		// Method: public List<Map<? super Map.Entry<U, V>, ? extends Set<List<? extends W>>>> ultimateReturnType()
		var ultimateReturnType = assertMethod(cg, "ultimateReturnType()");
		var urtType = ultimateReturnType.getType();
		assertThat(urtType.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) urtType).getTypeArguments(), hasSize(1));
		var mapInUrt = ((TypeReference<?>) urtType).getTypeArguments().get(0);
		assertThat(mapInUrt.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) mapInUrt).getTypeArguments(), hasSize(2));
		var urtArg1 = ((TypeReference<?>) mapInUrt).getTypeArguments().get(0);
		assertThat(urtArg1, instanceOf(WildcardTypeReference.class));
		var urtWc1 = (WildcardTypeReference) urtArg1;
		assertThat(urtWc1.upper(), is(false));
		assertThat(urtWc1.bounds(), hasSize(1));
		var entryBoundUrt = urtWc1.bounds().get(0);
		assertThat(entryBoundUrt.getQualifiedName(), is(equalTo("java.util.Map$Entry")));
		assertThat(((TypeReference<?>) entryBoundUrt).getTypeArguments(), hasSize(2));
		var entryBoundArg1 = ((TypeReference<?>) entryBoundUrt).getTypeArguments().get(0);
		assertThat(entryBoundArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) entryBoundArg1).getQualifiedName(), is(equalTo("U")));
		var entryBoundArg2 = ((TypeReference<?>) entryBoundUrt).getTypeArguments().get(1);
		assertThat(entryBoundArg2, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) entryBoundArg2).getQualifiedName(), is(equalTo("V")));
		var urtArg2 = ((TypeReference<?>) mapInUrt).getTypeArguments().get(1);
		assertThat(urtArg2, instanceOf(WildcardTypeReference.class));
		var urtWc2 = (WildcardTypeReference) urtArg2;
		assertThat(urtWc2.upper(), is(true));
		assertThat(urtWc2.bounds(), hasSize(1));
		var setListType = urtWc2.bounds().get(0);
		assertThat(setListType.getQualifiedName(), is(equalTo("java.util.Set")));
		assertThat(((TypeReference<?>) setListType).getTypeArguments(), hasSize(1));
		var listTypeInSet = ((TypeReference<?>) setListType).getTypeArguments().get(0);
		assertThat(listTypeInSet.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) listTypeInSet).getTypeArguments(), hasSize(1));
		var listWcInSet = ((TypeReference<?>) listTypeInSet).getTypeArguments().get(0);
		assertThat(listWcInSet, instanceOf(WildcardTypeReference.class));
		var listWcInSetRef = (WildcardTypeReference) listWcInSet;
		assertThat(listWcInSetRef.upper(), is(true));
		assertThat(listWcInSetRef.bounds(), hasSize(1));
		assertThat(listWcInSetRef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) listWcInSetRef.bounds().get(0)).getQualifiedName(), is(equalTo("W")));

		// Method: public static <N extends Object & Comparable<? super N>> N staticGenericMethod(N param)
		var staticGenericMethod = assertMethod(cg, "staticGenericMethod(java.lang.Object)");
		assertThat(staticGenericMethod.isStatic(), is(true));
		assertThat(staticGenericMethod.getFormalTypeParameters(), hasSize(1));
		var nParam = staticGenericMethod.getFormalTypeParameters().get(0);
		assertThat(nParam.name(), is(equalTo("N")));
		assertThat(nParam.bounds(), hasSize(2));
		var nBound0 = nParam.bounds().get(0);
		assertThat(nBound0.getQualifiedName(), is(equalTo("java.lang.Object")));
		assertThat(((TypeReference<?>) nBound0).getTypeArguments(), hasSize(0));
		var nBound1 = nParam.bounds().get(1);
		assertThat(nBound1.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) nBound1).getTypeArguments(), hasSize(1));
		var nWc = ((TypeReference<?>) nBound1).getTypeArguments().get(0);
		assertThat(nWc, instanceOf(WildcardTypeReference.class));
		var nWcRef = (WildcardTypeReference) nWc;
		assertThat(nWcRef.upper(), is(false));
		assertThat(nWcRef.bounds(), hasSize(1));
		assertThat(nWcRef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) nWcRef.bounds().get(0)).getQualifiedName(), is(equalTo("N")));
		assertThat(staticGenericMethod.getParameters(), hasSize(1));
		var staticParam = staticGenericMethod.getParameters().get(0);
		assertThat(staticParam.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) staticParam.type()).getQualifiedName(), is(equalTo("N")));
		assertThat(staticGenericMethod.getType(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) staticGenericMethod.getType()).getQualifiedName(), is(equalTo("N")));

		// Method: public <P extends Exception> void genericThrows() throws P
		var genericThrows = assertMethod(cg, "genericThrows()");
		assertThat(genericThrows.getFormalTypeParameters(), hasSize(1));
		var pParam = genericThrows.getFormalTypeParameters().get(0);
		assertThat(pParam.name(), is(equalTo("P")));
		assertThat(pParam.bounds(), hasSize(1));
		var pBound = pParam.bounds().get(0);
		assertThat(pBound.getQualifiedName(), is(equalTo("java.lang.Exception")));
		assertThat(genericThrows.getThrownExceptions(), hasSize(1));
		var thrownEx = genericThrows.getThrownExceptions().get(0);
		assertThat(thrownEx, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) thrownEx).getQualifiedName(), is(equalTo("P")));

		// Method: public void captureHelper(List<?> wildcardList)
		var captureHelper = assertMethod(cg, "captureHelper(java.util.List)");
		assertThat(captureHelper.getParameters(), hasSize(1));
		var chParam = captureHelper.getParameters().get(0);
		assertThat(chParam.type().getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) chParam.type()).getTypeArguments(), hasSize(1));
		var chWc = ((TypeReference<?>) chParam.type()).getTypeArguments().get(0);
		assertThat(chWc, instanceOf(WildcardTypeReference.class));
		var chWcRef = (WildcardTypeReference) chWc;
		assertThat(chWcRef.upper(), is(true));
		assertThat(chWcRef.bounds(), hasSize(1));
		assertThat(chWcRef.bounds().get(0), is(equalTo(TypeReference.OBJECT)));

		// Method: public <X extends Comparable<X> & Serializable> void methodWithMultipleBounds(X value)
		var methodWithMultipleBounds = assertMethod(cg, "methodWithMultipleBounds(java.lang.Comparable)");
		assertThat(methodWithMultipleBounds.getFormalTypeParameters(), hasSize(1));
		var xMb = methodWithMultipleBounds.getFormalTypeParameters().get(0);
		assertThat(xMb.name(), is(equalTo("X")));
		assertThat(xMb.bounds(), hasSize(2));
		var compXMb = xMb.bounds().get(0);
		assertThat(compXMb.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) compXMb).getTypeArguments(), hasSize(1));
		var xMbArg = ((TypeReference<?>) compXMb).getTypeArguments().get(0);
		assertThat(xMbArg, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) xMbArg).getQualifiedName(), is(equalTo("X")));
		var serXMb = xMb.bounds().get(1);
		assertThat(serXMb.getQualifiedName(), is(equalTo("java.io.Serializable")));

		// Method: public List<? extends Number> getWildcardNumberList()
		var getWildcardNumberList = assertMethod(cg, "getWildcardNumberList()");
		var gwnType = getWildcardNumberList.getType();
		assertThat(gwnType.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) gwnType).getTypeArguments(), hasSize(1));
		var gwnArg = ((TypeReference<?>) gwnType).getTypeArguments().get(0);
		assertThat(gwnArg, instanceOf(WildcardTypeReference.class));
		var gwnWc = (WildcardTypeReference) gwnArg;
		assertThat(gwnWc.upper(), is(true));
		assertThat(gwnWc.bounds(), hasSize(1));
		assertThat(gwnWc.bounds().get(0).getQualifiedName(), is(equalTo("java.lang.Number")));

		// Method: public <A extends T, B extends List<A>> B dependentTypeMethod(A element)
		var dependentTypeMethod = assertMethod(cg, "dependentTypeMethod(java.lang.Comparable)");
		assertThat(dependentTypeMethod.getFormalTypeParameters(), hasSize(2));
		var aDtm = dependentTypeMethod.getFormalTypeParameters().get(0);
		assertThat(aDtm.name(), is(equalTo("A")));
		assertThat(aDtm.bounds(), hasSize(1));
		var aDtmBound = aDtm.bounds().get(0);
		assertThat(aDtmBound, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) aDtmBound).getQualifiedName(), is(equalTo("T")));
		var bDtm = dependentTypeMethod.getFormalTypeParameters().get(1);
		assertThat(bDtm.name(), is(equalTo("B")));
		assertThat(bDtm.bounds(), hasSize(1));
		var bDtmBound = bDtm.bounds().get(0);
		assertThat(bDtmBound.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) bDtmBound).getTypeArguments(), hasSize(1));
		var bDtmArg = ((TypeReference<?>) bDtmBound).getTypeArguments().get(0);
		assertThat(bDtmArg, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) bDtmArg).getQualifiedName(), is(equalTo("A")));
		assertThat(dependentTypeMethod.getParameters(), hasSize(1));
		var dtmParam = dependentTypeMethod.getParameters().get(0);
		assertThat(dtmParam.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) dtmParam.type()).getQualifiedName(), is(equalTo("A")));
		assertThat(dependentTypeMethod.getType(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) dependentTypeMethod.getType()).getQualifiedName(), is(equalTo("B")));

		// Method: public void processIntersectionType(List<? extends Number> list, Map<String, ? super Integer> map)
		var processIntersectionType = assertMethod(cg, "processIntersectionType(java.util.List,java.util.Map)");
		assertThat(processIntersectionType.getParameters(), hasSize(2));
		var pitParam1 = processIntersectionType.getParameters().get(0);
		assertThat(pitParam1.type().getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) pitParam1.type()).getTypeArguments(), hasSize(1));
		var pitWc1 = ((TypeReference<?>) pitParam1.type()).getTypeArguments().get(0);
		assertThat(pitWc1, instanceOf(WildcardTypeReference.class));
		var pitWc1Ref = (WildcardTypeReference) pitWc1;
		assertThat(pitWc1Ref.upper(), is(true));
		assertThat(pitWc1Ref.bounds(), hasSize(1));
		assertThat(pitWc1Ref.bounds().get(0).getQualifiedName(), is(equalTo("java.lang.Number")));
		var pitParam2 = processIntersectionType.getParameters().get(1);
		assertThat(pitParam2.type().getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) pitParam2.type()).getTypeArguments(), hasSize(2));
		var pitArg1 = ((TypeReference<?>) pitParam2.type()).getTypeArguments().get(0);
		assertThat(pitArg1.getQualifiedName(), is(equalTo("java.lang.String")));
		var pitArg2 = ((TypeReference<?>) pitParam2.type()).getTypeArguments().get(1);
		assertThat(pitArg2, instanceOf(WildcardTypeReference.class));
		var pitWc2 = (WildcardTypeReference) pitArg2;
		assertThat(pitWc2.upper(), is(false));
		assertThat(pitWc2.bounds(), hasSize(1));
		assertThat(pitWc2.bounds().get(0).getQualifiedName(), is(equalTo("java.lang.Integer")));

		// Method: public <R extends Comparable<R>> R recursiveGenericMethod(R value)
		var recursiveGenericMethod = assertMethod(cg, "recursiveGenericMethod(java.lang.Comparable)");
		assertThat(recursiveGenericMethod.getFormalTypeParameters(), hasSize(1));
		var rParam = recursiveGenericMethod.getFormalTypeParameters().get(0);
		assertThat(rParam.name(), is(equalTo("R")));
		assertThat(rParam.bounds(), hasSize(1));
		var compR = rParam.bounds().get(0);
		assertThat(compR.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) compR).getTypeArguments(), hasSize(1));
		var rArg = ((TypeReference<?>) compR).getTypeArguments().get(0);
		assertThat(rArg, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) rArg).getQualifiedName(), is(equalTo("R")));
		var recGenParam = recursiveGenericMethod.getParameters().get(0);
		assertThat(recGenParam.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) recGenParam.type()).getQualifiedName(), is(equalTo("R")));
		assertThat(recursiveGenericMethod.getType(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) recursiveGenericMethod.getType()).getQualifiedName(), is(equalTo("R")));

		// Method: public <K, W extends List<K>> W genericMethod(K key, W list)
		var genericMethod = assertMethod(cg, "genericMethod(java.lang.Object,java.util.List)");
		assertThat(genericMethod.getFormalTypeParameters(), hasSize(2));
		var kGm = genericMethod.getFormalTypeParameters().get(0);
		assertThat(kGm.name(), is(equalTo("K")));
		assertThat(kGm.bounds(), hasSize(1));
		assertThat(kGm.bounds().getFirst(), is(equalTo(TypeReference.OBJECT)));
		var wGm = genericMethod.getFormalTypeParameters().get(1);
		assertThat(wGm.name(), is(equalTo("W")));
		assertThat(wGm.bounds(), hasSize(1));
		var gmBound = wGm.bounds().get(0);
		assertThat(gmBound.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) gmBound).getTypeArguments(), hasSize(1));
		var gmBoundArg = ((TypeReference<?>) gmBound).getTypeArguments().get(0);
		assertThat(gmBoundArg, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) gmBoundArg).getQualifiedName(), is(equalTo("K")));
		assertThat(genericMethod.getParameters(), hasSize(2));
		var gmParam1 = genericMethod.getParameters().get(0);
		assertThat(gmParam1.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) gmParam1.type()).getQualifiedName(), is(equalTo("K")));
		var gmParam2 = genericMethod.getParameters().get(1);
		assertThat(gmParam2.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) gmParam2.type()).getQualifiedName(), is(equalTo("W")));

		// Method: public Map<List<? extends T>, Set<? super U>> getNestedGenericTypes()
		var getNestedGenericTypes = assertMethod(cg, "getNestedGenericTypes()");
		var ngtType = getNestedGenericTypes.getType();
		assertThat(ngtType.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) ngtType).getTypeArguments(), hasSize(2));
		var ngtArg1 = ((TypeReference<?>) ngtType).getTypeArguments().get(0);
		assertThat(ngtArg1.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) ngtArg1).getTypeArguments(), hasSize(1));
		var ngtArg1Wc = ((TypeReference<?>) ngtArg1).getTypeArguments().get(0);
		assertThat(ngtArg1Wc, instanceOf(WildcardTypeReference.class));
		var ngtArg1WcRef = (WildcardTypeReference) ngtArg1Wc;
		assertThat(ngtArg1WcRef.upper(), is(true));
		assertThat(ngtArg1WcRef.bounds(), hasSize(1));
		assertThat(ngtArg1WcRef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) ngtArg1WcRef.bounds().get(0)).getQualifiedName(), is(equalTo("T")));
		var ngtArg2 = ((TypeReference<?>) ngtType).getTypeArguments().get(1);
		assertThat(ngtArg2.getQualifiedName(), is(equalTo("java.util.Set")));
		assertThat(((TypeReference<?>) ngtArg2).getTypeArguments(), hasSize(1));
		var ngtArg2Wc = ((TypeReference<?>) ngtArg2).getTypeArguments().get(0);
		assertThat(ngtArg2Wc, instanceOf(WildcardTypeReference.class));
		var ngtArg2WcRef = (WildcardTypeReference) ngtArg2Wc;
		assertThat(ngtArg2WcRef.upper(), is(false));
		assertThat(ngtArg2WcRef.bounds(), hasSize(1));
		assertThat(ngtArg2WcRef.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) ngtArg2WcRef.bounds().get(0)).getQualifiedName(), is(equalTo("U")));

		// Method: public <E> List<E>[] createGenericArray(int size)
		var createGenericArray = assertMethod(cg, "createGenericArray(int)");
		assertThat(createGenericArray.getFormalTypeParameters(), hasSize(1));
		var eParam = createGenericArray.getFormalTypeParameters().get(0);
		assertThat(eParam.name(), is(equalTo("E")));
		assertThat(eParam.bounds(), hasSize(1));
		assertThat(eParam.bounds().getFirst(), is(equalTo(TypeReference.OBJECT)));
		assertThat(createGenericArray.getParameters(), hasSize(1));
		assertThat(createGenericArray.getParameters().get(0).type().getQualifiedName(), is(equalTo("int")));
		if (createGenericArray.getType() instanceof ArrayTypeReference atr) {
			var componentType = atr.componentType();
			assertThat(componentType.getQualifiedName(), is(equalTo("java.util.List")));
			assertThat(((TypeReference<?>) componentType).getTypeArguments(), hasSize(1));
			var cgaArg = ((TypeReference<?>) componentType).getTypeArguments().get(0);
			assertThat(cgaArg, instanceOf(TypeParameterReference.class));
			assertThat(((TypeParameterReference) cgaArg).getQualifiedName(), is(equalTo("E")));
		}

		// Method: public void captureWildcard(List<?> unknownList)
		var captureWildcard = assertMethod(cg, "captureWildcard(java.util.List)");
		assertThat(captureWildcard.getParameters(), hasSize(1));
		var cwParam = captureWildcard.getParameters().get(0);
		assertThat(cwParam.type().getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) cwParam.type()).getTypeArguments(), hasSize(1));
		var cwArg = ((TypeReference<?>) cwParam.type()).getTypeArguments().get(0);
		assertThat(cwArg, instanceOf(WildcardTypeReference.class));
		var cwArgRef = (WildcardTypeReference) cwArg;
		assertThat(cwArgRef.upper(), is(true));
		assertThat(cwArgRef.bounds(), hasSize(1));
		assertThat(cwArgRef.bounds().get(0), is(equalTo(TypeReference.OBJECT)));

		// Method: public static <Z> ComplexGenerics<?, ?, ?, ?> createComplexInstance()
		var createComplexInstance = assertMethod(cg, "createComplexInstance()");
		assertThat(createComplexInstance.isStatic(), is(true));
		assertThat(createComplexInstance.getFormalTypeParameters(), hasSize(1));
		var zParam = createComplexInstance.getFormalTypeParameters().get(0);
		assertThat(zParam.name(), is(equalTo("Z")));
		assertThat(zParam.bounds(), hasSize(1));
		assertThat(zParam.bounds().getFirst(), is(equalTo(TypeReference.OBJECT)));
		var cciType = createComplexInstance.getType();
		assertThat(cciType.getQualifiedName(), is(equalTo("ComplexGenerics")));
		assertThat(((TypeReference<?>) cciType).getTypeArguments(), hasSize(4));
		for (int i = 0; i < 4; i++) {
			var arg = ((TypeReference<?>) cciType).getTypeArguments().get(i);
			assertThat(arg, instanceOf(WildcardTypeReference.class));
			var wc = (WildcardTypeReference) arg;
			assertThat(wc.upper(), is(true));
			assertThat(wc.bounds(), hasSize(1));
			assertThat(wc.bounds().get(0), is(equalTo(TypeReference.OBJECT)));
		}

		// Nested Class: InnerClass<Q extends Map<T, W>>
		var innerClass = assertClass(api, "ComplexGenerics$InnerClass");
		assertThat(innerClass.getFormalTypeParameters(), hasSize(1));
		var qParam = innerClass.getFormalTypeParameters().get(0);
		assertThat(qParam.name(), is(equalTo("Q")));
		assertThat(qParam.bounds(), hasSize(1));
		var qBound = qParam.bounds().get(0);
		assertThat(qBound.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) qBound).getTypeArguments(), hasSize(2));
		var qBoundArg1 = ((TypeReference<?>) qBound).getTypeArguments().get(0);
		assertThat(qBoundArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) qBoundArg1).getQualifiedName(), is(equalTo("T")));
		var qBoundArg2 = ((TypeReference<?>) qBound).getTypeArguments().get(1);
		assertThat(qBoundArg2, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) qBoundArg2).getQualifiedName(), is(equalTo("W")));
		// Field: private Q nestedTypeField;
		var nestedTypeField = assertField(innerClass, "nestedTypeField");
		assertThat(nestedTypeField.getType(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) nestedTypeField.getType()).getQualifiedName(), is(equalTo("Q")));
		// Method: public <R extends List<Q> & Serializable> Map<R, ? extends Q> innerMethod(R param)
		var innerMethod = assertMethod(innerClass, "innerMethod(java.util.List)");
		assertThat(innerMethod.getFormalTypeParameters(), hasSize(1));
		var rParamInner = innerMethod.getFormalTypeParameters().get(0);
		assertThat(rParamInner.name(), is(equalTo("R")));
		assertThat(rParamInner.bounds(), hasSize(2));
		var listQBound = rParamInner.bounds().get(0);
		assertThat(listQBound.getQualifiedName(), is(equalTo("java.util.List")));
		assertThat(((TypeReference<?>) listQBound).getTypeArguments(), hasSize(1));
		var listQArg = ((TypeReference<?>) listQBound).getTypeArguments().get(0);
		assertThat(listQArg, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) listQArg).getQualifiedName(), is(equalTo("Q")));
		var serBoundInner = rParamInner.bounds().get(1);
		assertThat(serBoundInner.getQualifiedName(), is(equalTo("java.io.Serializable")));
		var imRet = innerMethod.getType();
		assertThat(imRet.getQualifiedName(), is(equalTo("java.util.Map")));
		assertThat(((TypeReference<?>) imRet).getTypeArguments(), hasSize(2));
		var imArg1 = ((TypeReference<?>) imRet).getTypeArguments().get(0);
		assertThat(imArg1, instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) imArg1).getQualifiedName(), is(equalTo("R")));
		var imArg2 = ((TypeReference<?>) imRet).getTypeArguments().get(1);
		assertThat(imArg2, instanceOf(WildcardTypeReference.class));
		var imWc = (WildcardTypeReference) imArg2;
		assertThat(imWc.upper(), is(true));
		assertThat(imWc.bounds(), hasSize(1));
		assertThat(imWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) imWc.bounds().get(0)).getQualifiedName(), is(equalTo("Q")));
		assertThat(innerMethod.getParameters(), hasSize(1));
		var imParam = innerMethod.getParameters().get(0);
		assertThat(imParam.type(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) imParam.type()).getQualifiedName(), is(equalTo("R")));

		// Nested Interface: GenericInterface<S extends Comparable<? super S>>
		var genericInterface = assertInterface(api, "ComplexGenerics$GenericInterface");
		assertThat(genericInterface.isInterface(), is(true));
		assertThat(genericInterface.getFormalTypeParameters(), hasSize(1));
		var sGi = genericInterface.getFormalTypeParameters().get(0);
		assertThat(sGi.name(), is(equalTo("S")));
		assertThat(sGi.bounds(), hasSize(1));
		var sGiBound = sGi.bounds().get(0);
		assertThat(sGiBound.getQualifiedName(), is(equalTo("java.lang.Comparable")));
		assertThat(((TypeReference<?>) sGiBound).getTypeArguments(), hasSize(1));
		var sGiBoundArg = ((TypeReference<?>) sGiBound).getTypeArguments().get(0);
		assertThat(sGiBoundArg, instanceOf(WildcardTypeReference.class));
		var sGiWc = (WildcardTypeReference) sGiBoundArg;
		assertThat(sGiWc.upper(), is(false));
		assertThat(sGiWc.bounds(), hasSize(1));
		assertThat(sGiWc.bounds().get(0), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) sGiWc.bounds().get(0)).getQualifiedName(), is(equalTo("S")));
		// Method: S method();
		var giMethod = assertMethod(genericInterface, "method()");
		assertThat(giMethod.getType(), instanceOf(TypeParameterReference.class));
		assertThat(((TypeParameterReference) giMethod.getType()).getQualifiedName(), is(equalTo("S")));
	}
}
