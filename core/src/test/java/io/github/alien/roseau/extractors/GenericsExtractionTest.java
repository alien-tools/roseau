package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class GenericsExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void single_type_parameter(ApiBuilder builder) {
		var api = builder.build("class A<T> {}");
		var a = assertClass(api, "A");

		assertThat(a.getFormalTypeParameters())
			.singleElement()
			.satisfies(ftp -> {
				assertThat(ftp.name()).isEqualTo("T");
				assertThat(ftp.bounds()).singleElement().isEqualTo(TypeReference.OBJECT);
			});
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_class_bound(ApiBuilder builder) {
		var api = builder.build("class A<T extends CharSequence> {}");
		var a = assertClass(api, "A");

		assertThat(a.getFormalTypeParameters())
			.singleElement()
			.satisfies(ftp -> {
				assertThat(ftp.name()).isEqualTo("T");
				assertThat(ftp.bounds()).singleElement().isEqualTo(new TypeReference<>("java.lang.CharSequence"));
			});
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_interface_bound(ApiBuilder builder) {
		var api = builder.build("class A<T extends Runnable> {}");
		var a = assertClass(api, "A");

		assertThat(a.getFormalTypeParameters())
			.singleElement()
			.satisfies(ftp -> {
				assertThat(ftp.name()).isEqualTo("T");
				assertThat(ftp.bounds()).singleElement().isEqualTo(new TypeReference<>("java.lang.Runnable"));
			});
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_several_bounds(ApiBuilder builder) {
		var api = builder.build("class A<T extends CharSequence & Runnable> {}");
		var a = assertClass(api, "A");

		assertThat(a.getFormalTypeParameters())
			.singleElement()
			.satisfies(ftp -> {
				assertThat(ftp.name()).isEqualTo("T");
				assertThat(ftp.bounds())
					.containsOnly(new TypeReference<>("java.lang.CharSequence"), new TypeReference<>("java.lang.Runnable"));
			});
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_dependent_parameter_bound(ApiBuilder builder) {
		var api = builder.build("class A<T, U extends T> {}");
		var a = assertClass(api, "A");

		assertThat(a.getFormalTypeParameters())
			.hasSize(2)
			.satisfiesExactly(t -> {
				assertThat(t.name()).isEqualTo("T");
				assertThat(t.bounds()).singleElement().isEqualTo(TypeReference.OBJECT);
			}, u -> {
				assertThat(u.name()).isEqualTo("U");
				assertThat(u.bounds())
					.singleElement()
					.isEqualTo(new TypeParameterReference("T"));
			});
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_parameter_with_dependent_class_bound(ApiBuilder builder) {
		var api = builder.build("""
			class X {}
			class A<T, U extends X> {}""");
		var a = assertClass(api, "A");

		assertThat(a.getFormalTypeParameters())
			.hasSize(2)
			.satisfiesExactly(t -> {
				assertThat(t.name()).isEqualTo("T");
				assertThat(t.bounds()).singleElement().isEqualTo(TypeReference.OBJECT);
			}, u -> {
				assertThat(u.name()).isEqualTo("U");
				assertThat(u.bounds())
					.singleElement()
					.isEqualTo(new TypeReference<>("X"));
			});
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
		var m1 = assertMethod(api, c, "m1()");
		var m2 = assertMethod(api, c, "m2()");
		var m3 = assertMethod(api, c, "m3()");
		var m4 = assertMethod(api, c, "m4(java.util.List)");
		var m5 = assertMethod(api, c, "m5(java.util.List)");
		var m6 = assertMethod(api, c, "m6(java.util.List)");

		assertThat(m1.getType()).isEqualTo(new TypeReference<>("java.util.List",
			List.of(new WildcardTypeReference(List.of(TypeReference.OBJECT), true))));
		assertThat(m2.getType()).isEqualTo(new TypeReference<>("java.util.List",
			List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Number")), true))));
		assertThat(m3.getType()).isEqualTo(new TypeReference<>("java.util.List",
			List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Number")), false))));
		assertThat(m4.getParameters())
			.singleElement()
			.extracting(ParameterDecl::type)
			.isEqualTo(new TypeReference<>("java.util.List",
				List.of(new WildcardTypeReference(List.of(TypeReference.OBJECT), true))));
		assertThat(m5.getParameters())
			.singleElement()
			.extracting(ParameterDecl::type)
			.isEqualTo(new TypeReference<>("java.util.List",
				List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Number")), true))));
		assertThat(m6.getParameters())
			.singleElement()
			.extracting(ParameterDecl::type)
			.isEqualTo(new TypeReference<>("java.util.List",
				List.of(new WildcardTypeReference(List.of(new TypeReference<>("java.lang.Number")), false))));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void throwable_generic(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public <X extends Throwable> void m() throws X {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		assertThat(m.getThrownExceptions())
			.singleElement()
			.isEqualTo(new TypeParameterReference("X"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_arguments_type_parameter_resolution(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends CharSequence> {
				public class B<U extends Number> {
					public <V> void m(T t, U v, V u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(api, b, "m(java.lang.CharSequence,java.lang.Number,java.lang.Object)");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_arguments_type_parameter_resolution_hiding(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends CharSequence> {
				public class B<T extends Number> {
					public <T> void m(T t, T v, T u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(api, b, "m(java.lang.Object,java.lang.Object,java.lang.Object)");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_type_parameter_resolution(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T, U extends CharSequence, V extends Number> extends java.util.ArrayList<U>
				implements java.util.function.Supplier<V> {
				public V get() { return null; }
			}""");

		var a = assertClass(api, "A");
		var v = a.getFormalTypeParameters().get(2);
		var get = assertMethod(api, a, "get()");
		var mvRef = get.getType();

		if (mvRef instanceof TypeParameterReference tpr) {
			assertThat(api.resolveTypeParameter(get, tpr)).hasValue(v);
			assertThat(api.resolveTypeParameterBound(get, tpr)).isEqualTo(new TypeReference<>("java.lang.Number"));
		} else fail();
	}

	// Had to have some fun with o3 ;)
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
		assertThat(cg.getFormalTypeParameters()).hasSize(4);

		// T: T extends Comparable<T> & Serializable
		var tParam = cg.getFormalTypeParameters().getFirst();
		assertThat(tParam.name()).isEqualTo("T");
		assertThat(tParam.bounds()).hasSize(2);
		var compT = tParam.bounds().getFirst();
		assertThat(compT.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) compT).typeArguments()).hasSize(1);
		var compTArg = ((TypeReference<?>) compT).typeArguments().getFirst();
		assertThat(compTArg).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) compTArg).getQualifiedName()).isEqualTo("T");
		var serT = tParam.bounds().get(1);
		assertThat(serT.getQualifiedName()).isEqualTo("java.io.Serializable");
		assertThat(((TypeReference<?>) serT).typeArguments()).isEmpty();

		// U: U extends ArrayList<Number> & Cloneable
		var uParam = cg.getFormalTypeParameters().get(1);
		assertThat(uParam.name()).isEqualTo("U");
		assertThat(uParam.bounds()).hasSize(2);
		var arrayListBound = uParam.bounds().getFirst();
		assertThat(arrayListBound.getQualifiedName()).isEqualTo("java.util.ArrayList");
		assertThat(((TypeReference<?>) arrayListBound).typeArguments()).hasSize(1);
		var numberArg = ((TypeReference<?>) arrayListBound).typeArguments().getFirst();
		assertThat(numberArg.getQualifiedName()).isEqualTo("java.lang.Number");
		var cloneableBound = uParam.bounds().get(1);
		assertThat(cloneableBound.getQualifiedName()).isEqualTo("java.lang.Cloneable");
		assertThat(((TypeReference<?>) cloneableBound).typeArguments()).isEmpty();

		// V: V extends Map<String, ? super List<? extends T>>
		var vParam = cg.getFormalTypeParameters().get(2);
		assertThat(vParam.name()).isEqualTo("V");
		assertThat(vParam.bounds()).hasSize(1);
		var mapBoundV = vParam.bounds().getFirst();
		assertThat(mapBoundV.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) mapBoundV).typeArguments()).hasSize(2);
		var strArg = ((TypeReference<?>) mapBoundV).typeArguments().getFirst();
		assertThat(strArg.getQualifiedName()).isEqualTo("java.lang.String");
		var wildcardV = ((TypeReference<?>) mapBoundV).typeArguments().get(1);
		assertThat(wildcardV).isInstanceOf(WildcardTypeReference.class);
		var wcV = (WildcardTypeReference) wildcardV;
		assertThat(wcV.upper()).isFalse();
		assertThat(wcV.bounds()).hasSize(1);
		var listBoundV = wcV.bounds().getFirst();
		assertThat(listBoundV.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) listBoundV).typeArguments()).hasSize(1);
		var innerWildcardV = ((TypeReference<?>) listBoundV).typeArguments().getFirst();
		assertThat(innerWildcardV).isInstanceOf(WildcardTypeReference.class);
		var wcInnerV = (WildcardTypeReference) innerWildcardV;
		assertThat(wcInnerV.upper()).isTrue();
		assertThat(wcInnerV.bounds()).hasSize(1);
		var tInV = wcInnerV.bounds().getFirst();
		assertThat(tInV).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) tInV).getQualifiedName()).isEqualTo("T");

		// W: W extends Number & Comparable<? super W>
		var wParam = cg.getFormalTypeParameters().get(3);
		assertThat(wParam.name()).isEqualTo("W");
		assertThat(wParam.bounds()).hasSize(2);
		var numberBoundW = wParam.bounds().getFirst();
		assertThat(numberBoundW.getQualifiedName()).isEqualTo("java.lang.Number");
		assertThat(((TypeReference<?>) numberBoundW).typeArguments()).isEmpty();
		var compW = wParam.bounds().get(1);
		assertThat(compW.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) compW).typeArguments()).hasSize(1);
		var wildcardW = ((TypeReference<?>) compW).typeArguments().getFirst();
		assertThat(wildcardW).isInstanceOf(WildcardTypeReference.class);
		var wcW = (WildcardTypeReference) wildcardW;
		assertThat(wcW.upper()).isFalse();
		assertThat(wcW.bounds()).hasSize(1);
		var wBoundInW = wcW.bounds().getFirst();
		assertThat(wBoundInW).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) wBoundInW).getQualifiedName()).isEqualTo("W");

		// Check superclass and interfaces
		assertThat(cg.getSuperClass().getQualifiedName()).isEqualTo("java.lang.Object");
		assertThat(cg.getImplementedInterfaces()).isEmpty();

		// Field: private Map<? super List<? extends T>, ? extends Set<?>> nestedWildcards;
		var nestedWildcards = assertField(api, cg, "nestedWildcards");
		var nwType = nestedWildcards.getType();
		assertThat(nwType.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) nwType).typeArguments()).hasSize(2);
		// First type argument: wildcard with lower bound List<? extends T>
		var nwArg1 = ((TypeReference<?>) nwType).typeArguments().getFirst();
		assertThat(nwArg1).isInstanceOf(WildcardTypeReference.class);
		var nwWc1 = (WildcardTypeReference) nwArg1;
		assertThat(nwWc1.upper()).isFalse();
		assertThat(nwWc1.bounds()).hasSize(1);
		var listTypeNW = nwWc1.bounds().getFirst();
		assertThat(listTypeNW.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) listTypeNW).typeArguments()).hasSize(1);
		var listArgNW = ((TypeReference<?>) listTypeNW).typeArguments().getFirst();
		assertThat(listArgNW).isInstanceOf(WildcardTypeReference.class);
		var listWcNW = (WildcardTypeReference) listArgNW;
		assertThat(listWcNW.upper()).isTrue();
		assertThat(listWcNW.bounds()).hasSize(1);
		var tInNW = listWcNW.bounds().getFirst();
		assertThat(tInNW).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) tInNW).getQualifiedName()).isEqualTo("T");
		// Second type argument: wildcard with upper bound Set<?>
		var nwArg2 = ((TypeReference<?>) nwType).typeArguments().get(1);
		assertThat(nwArg2).isInstanceOf(WildcardTypeReference.class);
		var nwWc2 = (WildcardTypeReference) nwArg2;
		assertThat(nwWc2.upper()).isTrue();
		assertThat(nwWc2.bounds()).hasSize(1);
		var setTypeNW = nwWc2.bounds().getFirst();
		assertThat(setTypeNW.getQualifiedName()).isEqualTo("java.util.Set");
		assertThat(((TypeReference<?>) setTypeNW).typeArguments()).hasSize(1);
		var setArgNW = ((TypeReference<?>) setTypeNW).typeArguments().getFirst();
		assertThat(setArgNW).isInstanceOf(WildcardTypeReference.class);
		var setWcNW = (WildcardTypeReference) setArgNW;
		assertThat(setWcNW.upper()).isTrue();
		assertThat(setWcNW.bounds()).hasSize(1);
		assertThat(setWcNW.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);

		// Field: private List<Map.Entry<U, ? extends Map<T, ?>>> nestedTypeArgs;
		var nestedTypeArgs = assertField(api, cg, "nestedTypeArgs");
		var ntaType = nestedTypeArgs.getType();
		assertThat(ntaType.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) ntaType).typeArguments()).hasSize(1);
		var entryType = ((TypeReference<?>) ntaType).typeArguments().getFirst();
		assertThat(entryType.getQualifiedName()).isEqualTo("java.util.Map$Entry");
		assertThat(((TypeReference<?>) entryType).typeArguments()).hasSize(2);
		// First argument: U
		var entryArg1 = ((TypeReference<?>) entryType).typeArguments().getFirst();
		assertThat(entryArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) entryArg1).getQualifiedName()).isEqualTo("U");
		// Second argument: wildcard with upper bound Map<T, ?>
		var entryArg2 = ((TypeReference<?>) entryType).typeArguments().get(1);
		assertThat(entryArg2).isInstanceOf(WildcardTypeReference.class);
		var entryWc = (WildcardTypeReference) entryArg2;
		assertThat(entryWc.upper()).isTrue();
		assertThat(entryWc.bounds()).hasSize(1);
		var mapInner = entryWc.bounds().getFirst();
		assertThat(mapInner.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) mapInner).typeArguments()).hasSize(2);
		var mapInnerArg1 = ((TypeReference<?>) mapInner).typeArguments().getFirst();
		assertThat(mapInnerArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) mapInnerArg1).getQualifiedName()).isEqualTo("T");
		var mapInnerArg2 = ((TypeReference<?>) mapInner).typeArguments().get(1);
		assertThat(mapInnerArg2).isInstanceOf(WildcardTypeReference.class);
		var innerMapWc = (WildcardTypeReference) mapInnerArg2;
		assertThat(innerMapWc.upper()).isTrue();
		assertThat(innerMapWc.bounds()).hasSize(1);
		assertThat(innerMapWc.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);

		// Field: private V complexDependentType;
		var complexDependentType = assertField(api, cg, "complexDependentType");
		var cdtType = complexDependentType.getType();
		assertThat(cdtType).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) cdtType).getQualifiedName()).isEqualTo("V");

		// Field: private U boundedField;
		var boundedField = assertField(api, cg, "boundedField");
		var bfType = boundedField.getType();
		assertThat(bfType).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) bfType).getQualifiedName()).isEqualTo("U");

		// Field: private GenericInterface<? extends W> genericInterfaceImpl;
		var genericInterfaceImpl = assertField(api, cg, "genericInterfaceImpl");
		var giType = genericInterfaceImpl.getType();
		assertThat(giType.getQualifiedName()).isEqualTo("ComplexGenerics$GenericInterface");
		assertThat(((TypeReference<?>) giType).typeArguments()).hasSize(1);
		var giArg = ((TypeReference<?>) giType).typeArguments().getFirst();
		assertThat(giArg).isInstanceOf(WildcardTypeReference.class);
		var giWc = (WildcardTypeReference) giArg;
		assertThat(giWc.upper()).isTrue();
		assertThat(giWc.bounds()).hasSize(1);
		assertThat(giWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) giWc.bounds().getFirst()).getQualifiedName()).isEqualTo("W");

		// Field: private U nestedList;
		var nestedListField = assertField(api, cg, "nestedList");
		var nlType = nestedListField.getType();
		assertThat(nlType).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) nlType).getQualifiedName()).isEqualTo("U");

		// Field: private V complexMap;
		var complexMapField = assertField(api, cg, "complexMap");
		var cmType = complexMapField.getType();
		assertThat(cmType).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) cmType).getQualifiedName()).isEqualTo("V");

		// Field: private List<? super T> superList;
		var superListField = assertField(api, cg, "superList");
		var slType = superListField.getType();
		assertThat(slType.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) slType).typeArguments()).hasSize(1);
		var slArg = ((TypeReference<?>) slType).typeArguments().getFirst();
		assertThat(slArg).isInstanceOf(WildcardTypeReference.class);
		var slWc = (WildcardTypeReference) slArg;
		assertThat(slWc.upper()).isFalse();
		assertThat(slWc.bounds()).hasSize(1);
		assertThat(slWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) slWc.bounds().getFirst()).getQualifiedName()).isEqualTo("T");

		// Field: private List<? extends T> extendsList;
		var extendsListField = assertField(api, cg, "extendsList");
		var elType = extendsListField.getType();
		assertThat(elType.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) elType).typeArguments()).hasSize(1);
		var elArg = ((TypeReference<?>) elType).typeArguments().getFirst();
		assertThat(elArg).isInstanceOf(WildcardTypeReference.class);
		var elWc = (WildcardTypeReference) elArg;
		assertThat(elWc.upper()).isTrue();
		assertThat(elWc.bounds()).hasSize(1);
		assertThat(elWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) elWc.bounds().getFirst()).getQualifiedName()).isEqualTo("T");

		// Constructor: public <S extends Map<? extends T, ? super U> & Cloneable> ComplexGenerics(S param)
		var ctors = cg.getDeclaredConstructors();
		assertThat(ctors).hasSize(1);
		var ctor = ctors.getFirst();
		assertThat(ctor.getFormalTypeParameters()).hasSize(1);
		var sParamCtor = ctor.getFormalTypeParameters().getFirst();
		assertThat(sParamCtor.name()).isEqualTo("S");
		assertThat(sParamCtor.bounds()).hasSize(2);
		var mapBoundS = sParamCtor.bounds().getFirst();
		assertThat(mapBoundS.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) mapBoundS).typeArguments()).hasSize(2);
		var sArg1 = ((TypeReference<?>) mapBoundS).typeArguments().getFirst();
		assertThat(sArg1).isInstanceOf(WildcardTypeReference.class);
		var sWc1 = (WildcardTypeReference) sArg1;
		assertThat(sWc1.upper()).isTrue();
		assertThat(sWc1.bounds()).hasSize(1);
		assertThat(sWc1.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) sWc1.bounds().getFirst()).getQualifiedName()).isEqualTo("T");
		var sArg2 = ((TypeReference<?>) mapBoundS).typeArguments().get(1);
		assertThat(sArg2).isInstanceOf(WildcardTypeReference.class);
		var sWc2 = (WildcardTypeReference) sArg2;
		assertThat(sWc2.upper()).isFalse();
		assertThat(sWc2.bounds()).hasSize(1);
		assertThat(sWc2.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) sWc2.bounds().getFirst()).getQualifiedName()).isEqualTo("U");
		var cloneBoundS = sParamCtor.bounds().get(1);
		assertThat(cloneBoundS.getQualifiedName()).isEqualTo("java.lang.Cloneable");
		assertThat(((TypeReference<?>) cloneBoundS).typeArguments()).isEmpty();
		assertThat(ctor.getParameters()).hasSize(1);
		var ctorParam = ctor.getParameters().getFirst();
		assertThat(ctorParam.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) ctorParam.type()).getQualifiedName()).isEqualTo("S");

		// Method: public <A extends List<? extends T>, B extends Map<A, ? super Set<? extends W>>> Map<A, B> complexMethod(A a, B b)
		var complexMethod = assertMethod(api, cg, "complexMethod(java.util.List,java.util.Map)");
		assertThat(complexMethod.getFormalTypeParameters()).hasSize(2);
		var aParam = complexMethod.getFormalTypeParameters().getFirst();
		assertThat(aParam.name()).isEqualTo("A");
		assertThat(aParam.bounds()).hasSize(1);
		var listBoundA = aParam.bounds().getFirst();
		assertThat(listBoundA.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) listBoundA).typeArguments()).hasSize(1);
		var wcA = ((TypeReference<?>) listBoundA).typeArguments().getFirst();
		assertThat(wcA).isInstanceOf(WildcardTypeReference.class);
		var wcARef = (WildcardTypeReference) wcA;
		assertThat(wcARef.upper()).isTrue();
		assertThat(wcARef.bounds()).hasSize(1);
		assertThat(wcARef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) wcARef.bounds().getFirst()).getQualifiedName()).isEqualTo("T");
		var bParam = complexMethod.getFormalTypeParameters().get(1);
		assertThat(bParam.name()).isEqualTo("B");
		assertThat(bParam.bounds()).hasSize(1);
		var mapBoundB = bParam.bounds().getFirst();
		assertThat(mapBoundB.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) mapBoundB).typeArguments()).hasSize(2);
		var mapBArg1 = ((TypeReference<?>) mapBoundB).typeArguments().getFirst();
		assertThat(mapBArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) mapBArg1).getQualifiedName()).isEqualTo("A");
		var mapBArg2 = ((TypeReference<?>) mapBoundB).typeArguments().get(1);
		assertThat(mapBArg2).isInstanceOf(WildcardTypeReference.class);
		var mapBWc = (WildcardTypeReference) mapBArg2;
		assertThat(mapBWc.upper()).isFalse();
		assertThat(mapBWc.bounds()).hasSize(1);
		var setBound = mapBWc.bounds().getFirst();
		assertThat(setBound.getQualifiedName()).isEqualTo("java.util.Set");
		assertThat(((TypeReference<?>) setBound).typeArguments()).hasSize(1);
		var setArg = ((TypeReference<?>) setBound).typeArguments().getFirst();
		assertThat(setArg).isInstanceOf(WildcardTypeReference.class);
		var setWc = (WildcardTypeReference) setArg;
		assertThat(setWc.upper()).isTrue();
		assertThat(setWc.bounds()).hasSize(1);
		assertThat(setWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) setWc.bounds().getFirst()).getQualifiedName()).isEqualTo("W");
		// Return type: Map<A, B>
		var cmRet = complexMethod.getType();
		assertThat(cmRet.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) cmRet).typeArguments()).hasSize(2);
		var retArg1 = ((TypeReference<?>) cmRet).typeArguments().getFirst();
		assertThat(retArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) retArg1).getQualifiedName()).isEqualTo("A");
		var retArg2 = ((TypeReference<?>) cmRet).typeArguments().get(1);
		assertThat(retArg2).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) retArg2).getQualifiedName()).isEqualTo("B");
		// Parameters: A and B
		assertThat(complexMethod.getParameters()).hasSize(2);
		var cmParam1 = complexMethod.getParameters().getFirst();
		assertThat(cmParam1.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) cmParam1.type()).getQualifiedName()).isEqualTo("A");
		var cmParam2 = complexMethod.getParameters().get(1);
		assertThat(cmParam2.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) cmParam2.type()).getQualifiedName()).isEqualTo("B");

		// Method: public <S extends Number & Comparable<? super S>> void intersectionTypeMethod(List<? extends S> numbers)
		var intersectionMethod = assertMethod(api, cg, "intersectionTypeMethod(java.util.List)");
		assertThat(intersectionMethod.getFormalTypeParameters()).hasSize(1);
		var sParamMethod = intersectionMethod.getFormalTypeParameters().getFirst();
		assertThat(sParamMethod.name()).isEqualTo("S");
		assertThat(sParamMethod.bounds()).hasSize(2);
		var numBoundS = sParamMethod.bounds().getFirst();
		assertThat(numBoundS.getQualifiedName()).isEqualTo("java.lang.Number");
		var compBoundS = sParamMethod.bounds().get(1);
		assertThat(compBoundS.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) compBoundS).typeArguments()).hasSize(1);
		var sWildcard = ((TypeReference<?>) compBoundS).typeArguments().getFirst();
		assertThat(sWildcard).isInstanceOf(WildcardTypeReference.class);
		var sWc = (WildcardTypeReference) sWildcard;
		assertThat(sWc.upper()).isFalse();
		assertThat(sWc.bounds()).hasSize(1);
		assertThat(sWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) sWc.bounds().getFirst()).getQualifiedName()).isEqualTo("S");
		var intMethodParam = intersectionMethod.getParameters().getFirst();
		assertThat(intMethodParam.type().getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) intMethodParam.type()).typeArguments()).hasSize(1);
		var intWc = ((TypeReference<?>) intMethodParam.type()).typeArguments().getFirst();
		assertThat(intWc).isInstanceOf(WildcardTypeReference.class);
		var intWcRef = (WildcardTypeReference) intWc;
		assertThat(intWcRef.upper()).isTrue();
		assertThat(intWcRef.bounds()).hasSize(1);
		assertThat(intWcRef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) intWcRef.bounds().getFirst()).getQualifiedName()).isEqualTo("S");

		// Method: public <X extends Comparable<X>> Map<X, List<? extends X>> recursiveBoundMethod(X input)
		var recursiveMethod = assertMethod(api, cg, "recursiveBoundMethod(java.lang.Comparable)");
		assertThat(recursiveMethod.getFormalTypeParameters()).hasSize(1);
		var xParam = recursiveMethod.getFormalTypeParameters().getFirst();
		assertThat(xParam.name()).isEqualTo("X");
		assertThat(xParam.bounds()).hasSize(1);
		var compX = xParam.bounds().getFirst();
		assertThat(compX.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) compX).typeArguments()).hasSize(1);
		var xArg = ((TypeReference<?>) compX).typeArguments().getFirst();
		assertThat(xArg).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) xArg).getQualifiedName()).isEqualTo("X");
		var recParam = recursiveMethod.getParameters().getFirst();
		assertThat(recParam.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) recParam.type()).getQualifiedName()).isEqualTo("X");
		var recRet = recursiveMethod.getType();
		assertThat(recRet.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) recRet).typeArguments()).hasSize(2);
		var recRetArg1 = ((TypeReference<?>) recRet).typeArguments().getFirst();
		assertThat(recRetArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) recRetArg1).getQualifiedName()).isEqualTo("X");
		var recRetArg2 = ((TypeReference<?>) recRet).typeArguments().get(1);
		assertThat(recRetArg2.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) recRetArg2).typeArguments()).hasSize(1);
		var recListWc = ((TypeReference<?>) recRetArg2).typeArguments().getFirst();
		assertThat(recListWc).isInstanceOf(WildcardTypeReference.class);
		var recListWcRef = (WildcardTypeReference) recListWc;
		assertThat(recListWcRef.upper()).isTrue();
		assertThat(recListWcRef.bounds()).hasSize(1);
		assertThat(recListWcRef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) recListWcRef.bounds().getFirst()).getQualifiedName()).isEqualTo("X");

		// Method: public void wildcardStorm(List<? super ArrayList<? extends T>> contravariantList, Set<? extends HashMap<? super W, ? extends U>> covariantSet)
		var wildcardStorm = assertMethod(api, cg, "wildcardStorm(java.util.List,java.util.Set)");
		assertThat(wildcardStorm.getParameters()).hasSize(2);
		// First parameter
		var wsParam1 = wildcardStorm.getParameters().getFirst();
		assertThat(wsParam1.type().getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) wsParam1.type()).typeArguments()).hasSize(1);
		var wsArg1 = ((TypeReference<?>) wsParam1.type()).typeArguments().getFirst();
		assertThat(wsArg1).isInstanceOf(WildcardTypeReference.class);
		var wsWc1 = (WildcardTypeReference) wsArg1;
		assertThat(wsWc1.upper()).isFalse();
		assertThat(wsWc1.bounds()).hasSize(1);
		var arrayListType = wsWc1.bounds().getFirst();
		assertThat(arrayListType.getQualifiedName()).isEqualTo("java.util.ArrayList");
		assertThat(((TypeReference<?>) arrayListType).typeArguments()).hasSize(1);
		var alWc = ((TypeReference<?>) arrayListType).typeArguments().getFirst();
		assertThat(alWc).isInstanceOf(WildcardTypeReference.class);
		var alWcRef = (WildcardTypeReference) alWc;
		assertThat(alWcRef.upper()).isTrue();
		assertThat(alWcRef.bounds()).hasSize(1);
		assertThat(alWcRef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) alWcRef.bounds().getFirst()).getQualifiedName()).isEqualTo("T");
		// Second parameter
		var wsParam2 = wildcardStorm.getParameters().get(1);
		assertThat(wsParam2.type().getQualifiedName()).isEqualTo("java.util.Set");
		assertThat(((TypeReference<?>) wsParam2.type()).typeArguments()).hasSize(1);
		var wsArg2 = ((TypeReference<?>) wsParam2.type()).typeArguments().getFirst();
		assertThat(wsArg2).isInstanceOf(WildcardTypeReference.class);
		var wsWc2 = (WildcardTypeReference) wsArg2;
		assertThat(wsWc2.upper()).isTrue();
		assertThat(wsWc2.bounds()).hasSize(1);
		var hashMapType = wsWc2.bounds().getFirst();
		assertThat(hashMapType.getQualifiedName()).isEqualTo("java.util.HashMap");
		assertThat(((TypeReference<?>) hashMapType).typeArguments()).hasSize(2);
		var hmArg1 = ((TypeReference<?>) hashMapType).typeArguments().getFirst();
		assertThat(hmArg1).isInstanceOf(WildcardTypeReference.class);
		var hmWc1 = (WildcardTypeReference) hmArg1;
		assertThat(hmWc1.upper()).isFalse();
		assertThat(hmWc1.bounds()).hasSize(1);
		assertThat(hmWc1.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) hmWc1.bounds().getFirst()).getQualifiedName()).isEqualTo("W");
		var hmArg2 = ((TypeReference<?>) hashMapType).typeArguments().get(1);
		assertThat(hmArg2).isInstanceOf(WildcardTypeReference.class);
		var hmWc2 = (WildcardTypeReference) hmArg2;
		assertThat(hmWc2.upper()).isTrue();
		assertThat(hmWc2.bounds()).hasSize(1);
		assertThat(hmWc2.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) hmWc2.bounds().getFirst()).getQualifiedName()).isEqualTo("U");

		// Method: public <K extends V, L extends Map<K, ? extends U>> L dependentTypesMethod(K key)
		var dependentTypesMethod = assertMethod(api, cg, "dependentTypesMethod(java.util.Map)");
		assertThat(dependentTypesMethod.getFormalTypeParameters()).hasSize(2);
		var kParam = dependentTypesMethod.getFormalTypeParameters().getFirst();
		assertThat(kParam.name()).isEqualTo("K");
		assertThat(kParam.bounds()).hasSize(1);
		var kBound = kParam.bounds().getFirst();
		assertThat(kBound).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) kBound).getQualifiedName()).isEqualTo("V");
		var lParam = dependentTypesMethod.getFormalTypeParameters().get(1);
		assertThat(lParam.name()).isEqualTo("L");
		assertThat(lParam.bounds()).hasSize(1);
		var lBound = lParam.bounds().getFirst();
		assertThat(lBound.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) lBound).typeArguments()).hasSize(2);
		var lBoundArg1 = ((TypeReference<?>) lBound).typeArguments().getFirst();
		assertThat(lBoundArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) lBoundArg1).getQualifiedName()).isEqualTo("K");
		var lBoundArg2 = ((TypeReference<?>) lBound).typeArguments().get(1);
		assertThat(lBoundArg2).isInstanceOf(WildcardTypeReference.class);
		var lBoundWc = (WildcardTypeReference) lBoundArg2;
		assertThat(lBoundWc.upper()).isTrue();
		assertThat(lBoundWc.bounds()).hasSize(1);
		assertThat(lBoundWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) lBoundWc.bounds().getFirst()).getQualifiedName()).isEqualTo("U");
		assertThat(dependentTypesMethod.getParameters()).hasSize(1);
		var depParam = dependentTypesMethod.getParameters().getFirst();
		assertThat(depParam.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) depParam.type()).getQualifiedName()).isEqualTo("K");
		assertThat(dependentTypesMethod.getType()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) dependentTypesMethod.getType()).getQualifiedName()).isEqualTo("L");

		// Method: public List<Map<? super Map.Entry<U, V>, ? extends Set<List<? extends W>>>> ultimateReturnType()
		var ultimateReturnType = assertMethod(api, cg, "ultimateReturnType()");
		var urtType = ultimateReturnType.getType();
		assertThat(urtType.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) urtType).typeArguments()).hasSize(1);
		var mapInUrt = ((TypeReference<?>) urtType).typeArguments().getFirst();
		assertThat(mapInUrt.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) mapInUrt).typeArguments()).hasSize(2);
		var urtArg1 = ((TypeReference<?>) mapInUrt).typeArguments().getFirst();
		assertThat(urtArg1).isInstanceOf(WildcardTypeReference.class);
		var urtWc1 = (WildcardTypeReference) urtArg1;
		assertThat(urtWc1.upper()).isFalse();
		assertThat(urtWc1.bounds()).hasSize(1);
		var entryBoundUrt = urtWc1.bounds().getFirst();
		assertThat(entryBoundUrt.getQualifiedName()).isEqualTo("java.util.Map$Entry");
		assertThat(((TypeReference<?>) entryBoundUrt).typeArguments()).hasSize(2);
		var entryBoundArg1 = ((TypeReference<?>) entryBoundUrt).typeArguments().getFirst();
		assertThat(entryBoundArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) entryBoundArg1).getQualifiedName()).isEqualTo("U");
		var entryBoundArg2 = ((TypeReference<?>) entryBoundUrt).typeArguments().get(1);
		assertThat(entryBoundArg2).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) entryBoundArg2).getQualifiedName()).isEqualTo("V");
		var urtArg2 = ((TypeReference<?>) mapInUrt).typeArguments().get(1);
		assertThat(urtArg2).isInstanceOf(WildcardTypeReference.class);
		var urtWc2 = (WildcardTypeReference) urtArg2;
		assertThat(urtWc2.upper()).isTrue();
		assertThat(urtWc2.bounds()).hasSize(1);
		var setListType = urtWc2.bounds().getFirst();
		assertThat(setListType.getQualifiedName()).isEqualTo("java.util.Set");
		assertThat(((TypeReference<?>) setListType).typeArguments()).hasSize(1);
		var listTypeInSet = ((TypeReference<?>) setListType).typeArguments().getFirst();
		assertThat(listTypeInSet.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) listTypeInSet).typeArguments()).hasSize(1);
		var listWcInSet = ((TypeReference<?>) listTypeInSet).typeArguments().getFirst();
		assertThat(listWcInSet).isInstanceOf(WildcardTypeReference.class);
		var listWcInSetRef = (WildcardTypeReference) listWcInSet;
		assertThat(listWcInSetRef.upper()).isTrue();
		assertThat(listWcInSetRef.bounds()).hasSize(1);
		assertThat(listWcInSetRef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) listWcInSetRef.bounds().getFirst()).getQualifiedName()).isEqualTo("W");

		// Method: public static <N extends Object & Comparable<? super N>> N staticGenericMethod(N param)
		var staticGenericMethod = assertMethod(api, cg, "staticGenericMethod(java.lang.Object)");
		assertThat(staticGenericMethod.isStatic()).isTrue();
		assertThat(staticGenericMethod.getFormalTypeParameters()).hasSize(1);
		var nParam = staticGenericMethod.getFormalTypeParameters().getFirst();
		assertThat(nParam.name()).isEqualTo("N");
		assertThat(nParam.bounds()).hasSize(2);
		var nBound0 = nParam.bounds().getFirst();
		assertThat(nBound0.getQualifiedName()).isEqualTo("java.lang.Object");
		assertThat(((TypeReference<?>) nBound0).typeArguments()).isEmpty();
		var nBound1 = nParam.bounds().get(1);
		assertThat(nBound1.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) nBound1).typeArguments()).hasSize(1);
		var nWc = ((TypeReference<?>) nBound1).typeArguments().getFirst();
		assertThat(nWc).isInstanceOf(WildcardTypeReference.class);
		var nWcRef = (WildcardTypeReference) nWc;
		assertThat(nWcRef.upper()).isFalse();
		assertThat(nWcRef.bounds()).hasSize(1);
		assertThat(nWcRef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) nWcRef.bounds().getFirst()).getQualifiedName()).isEqualTo("N");
		assertThat(staticGenericMethod.getParameters()).hasSize(1);
		var staticParam = staticGenericMethod.getParameters().getFirst();
		assertThat(staticParam.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) staticParam.type()).getQualifiedName()).isEqualTo("N");
		assertThat(staticGenericMethod.getType()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) staticGenericMethod.getType()).getQualifiedName()).isEqualTo("N");

		// Method: public <P extends Exception> void genericThrows() throws P
		var genericThrows = assertMethod(api, cg, "genericThrows()");
		assertThat(genericThrows.getFormalTypeParameters()).hasSize(1);
		var pParam = genericThrows.getFormalTypeParameters().getFirst();
		assertThat(pParam.name()).isEqualTo("P");
		assertThat(pParam.bounds()).hasSize(1);
		var pBound = pParam.bounds().getFirst();
		assertThat(pBound.getQualifiedName()).isEqualTo("java.lang.Exception");
		assertThat(genericThrows.getThrownExceptions()).hasSize(1);
		var thrownEx = genericThrows.getThrownExceptions().getFirst();
		assertThat(thrownEx).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) thrownEx).getQualifiedName()).isEqualTo("P");

		// Method: public void captureHelper(List<?> wildcardList)
		var captureHelper = assertMethod(api, cg, "captureHelper(java.util.List)");
		assertThat(captureHelper.getParameters()).hasSize(1);
		var chParam = captureHelper.getParameters().getFirst();
		assertThat(chParam.type().getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) chParam.type()).typeArguments()).hasSize(1);
		var chWc = ((TypeReference<?>) chParam.type()).typeArguments().getFirst();
		assertThat(chWc).isInstanceOf(WildcardTypeReference.class);
		var chWcRef = (WildcardTypeReference) chWc;
		assertThat(chWcRef.upper()).isTrue();
		assertThat(chWcRef.bounds()).hasSize(1);
		assertThat(chWcRef.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);

		// Method: public <X extends Comparable<X> & Serializable> void methodWithMultipleBounds(X value)
		var methodWithMultipleBounds = assertMethod(api, cg, "methodWithMultipleBounds(java.lang.Comparable)");
		assertThat(methodWithMultipleBounds.getFormalTypeParameters()).hasSize(1);
		var xMb = methodWithMultipleBounds.getFormalTypeParameters().getFirst();
		assertThat(xMb.name()).isEqualTo("X");
		assertThat(xMb.bounds()).hasSize(2);
		var compXMb = xMb.bounds().getFirst();
		assertThat(compXMb.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) compXMb).typeArguments()).hasSize(1);
		var xMbArg = ((TypeReference<?>) compXMb).typeArguments().getFirst();
		assertThat(xMbArg).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) xMbArg).getQualifiedName()).isEqualTo("X");
		var serXMb = xMb.bounds().get(1);
		assertThat(serXMb.getQualifiedName()).isEqualTo("java.io.Serializable");

		// Method: public List<? extends Number> getWildcardNumberList()
		var getWildcardNumberList = assertMethod(api, cg, "getWildcardNumberList()");
		var gwnType = getWildcardNumberList.getType();
		assertThat(gwnType.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) gwnType).typeArguments()).hasSize(1);
		var gwnArg = ((TypeReference<?>) gwnType).typeArguments().getFirst();
		assertThat(gwnArg).isInstanceOf(WildcardTypeReference.class);
		var gwnWc = (WildcardTypeReference) gwnArg;
		assertThat(gwnWc.upper()).isTrue();
		assertThat(gwnWc.bounds()).hasSize(1);
		assertThat(gwnWc.bounds().getFirst().getQualifiedName()).isEqualTo("java.lang.Number");

		// Method: public <A extends T, B extends List<A>> B dependentTypeMethod(A element)
		var dependentTypeMethod = assertMethod(api, cg, "dependentTypeMethod(java.lang.Comparable)");
		assertThat(dependentTypeMethod.getFormalTypeParameters()).hasSize(2);
		var aDtm = dependentTypeMethod.getFormalTypeParameters().getFirst();
		assertThat(aDtm.name()).isEqualTo("A");
		assertThat(aDtm.bounds()).hasSize(1);
		var aDtmBound = aDtm.bounds().getFirst();
		assertThat(aDtmBound).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) aDtmBound).getQualifiedName()).isEqualTo("T");
		var bDtm = dependentTypeMethod.getFormalTypeParameters().get(1);
		assertThat(bDtm.name()).isEqualTo("B");
		assertThat(bDtm.bounds()).hasSize(1);
		var bDtmBound = bDtm.bounds().getFirst();
		assertThat(bDtmBound.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) bDtmBound).typeArguments()).hasSize(1);
		var bDtmArg = ((TypeReference<?>) bDtmBound).typeArguments().getFirst();
		assertThat(bDtmArg).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) bDtmArg).getQualifiedName()).isEqualTo("A");
		assertThat(dependentTypeMethod.getParameters()).hasSize(1);
		var dtmParam = dependentTypeMethod.getParameters().getFirst();
		assertThat(dtmParam.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) dtmParam.type()).getQualifiedName()).isEqualTo("A");
		assertThat(dependentTypeMethod.getType()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) dependentTypeMethod.getType()).getQualifiedName()).isEqualTo("B");

		// Method: public void processIntersectionType(List<? extends Number> list, Map<String, ? super Integer> map)
		var processIntersectionType = assertMethod(api, cg, "processIntersectionType(java.util.List,java.util.Map)");
		assertThat(processIntersectionType.getParameters()).hasSize(2);
		var pitParam1 = processIntersectionType.getParameters().getFirst();
		assertThat(pitParam1.type().getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) pitParam1.type()).typeArguments()).hasSize(1);
		var pitWc1 = ((TypeReference<?>) pitParam1.type()).typeArguments().getFirst();
		assertThat(pitWc1).isInstanceOf(WildcardTypeReference.class);
		var pitWc1Ref = (WildcardTypeReference) pitWc1;
		assertThat(pitWc1Ref.upper()).isTrue();
		assertThat(pitWc1Ref.bounds()).hasSize(1);
		assertThat(pitWc1Ref.bounds().getFirst().getQualifiedName()).isEqualTo("java.lang.Number");
		var pitParam2 = processIntersectionType.getParameters().get(1);
		assertThat(pitParam2.type().getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) pitParam2.type()).typeArguments()).hasSize(2);
		var pitArg1 = ((TypeReference<?>) pitParam2.type()).typeArguments().getFirst();
		assertThat(pitArg1.getQualifiedName()).isEqualTo("java.lang.String");
		var pitArg2 = ((TypeReference<?>) pitParam2.type()).typeArguments().get(1);
		assertThat(pitArg2).isInstanceOf(WildcardTypeReference.class);
		var pitWc2 = (WildcardTypeReference) pitArg2;
		assertThat(pitWc2.upper()).isFalse();
		assertThat(pitWc2.bounds()).hasSize(1);
		assertThat(pitWc2.bounds().getFirst().getQualifiedName()).isEqualTo("java.lang.Integer");

		// Method: public <R extends Comparable<R>> R recursiveGenericMethod(R value)
		var recursiveGenericMethod = assertMethod(api, cg, "recursiveGenericMethod(java.lang.Comparable)");
		assertThat(recursiveGenericMethod.getFormalTypeParameters()).hasSize(1);
		var rParam = recursiveGenericMethod.getFormalTypeParameters().getFirst();
		assertThat(rParam.name()).isEqualTo("R");
		assertThat(rParam.bounds()).hasSize(1);
		var compR = rParam.bounds().getFirst();
		assertThat(compR.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) compR).typeArguments()).hasSize(1);
		var rArg = ((TypeReference<?>) compR).typeArguments().getFirst();
		assertThat(rArg).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) rArg).getQualifiedName()).isEqualTo("R");
		var recGenParam = recursiveGenericMethod.getParameters().getFirst();
		assertThat(recGenParam.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) recGenParam.type()).getQualifiedName()).isEqualTo("R");
		assertThat(recursiveGenericMethod.getType()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) recursiveGenericMethod.getType()).getQualifiedName()).isEqualTo("R");

		// Method: public <K, W extends List<K>> W genericMethod(K key, W list)
		var genericMethod = assertMethod(api, cg, "genericMethod(java.lang.Object,java.util.List)");
		assertThat(genericMethod.getFormalTypeParameters()).hasSize(2);
		var kGm = genericMethod.getFormalTypeParameters().getFirst();
		assertThat(kGm.name()).isEqualTo("K");
		assertThat(kGm.bounds()).hasSize(1);
		assertThat(kGm.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);
		var wGm = genericMethod.getFormalTypeParameters().get(1);
		assertThat(wGm.name()).isEqualTo("W");
		assertThat(wGm.bounds()).hasSize(1);
		var gmBound = wGm.bounds().getFirst();
		assertThat(gmBound.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) gmBound).typeArguments()).hasSize(1);
		var gmBoundArg = ((TypeReference<?>) gmBound).typeArguments().getFirst();
		assertThat(gmBoundArg).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) gmBoundArg).getQualifiedName()).isEqualTo("K");
		assertThat(genericMethod.getParameters()).hasSize(2);
		var gmParam1 = genericMethod.getParameters().getFirst();
		assertThat(gmParam1.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) gmParam1.type()).getQualifiedName()).isEqualTo("K");
		var gmParam2 = genericMethod.getParameters().get(1);
		assertThat(gmParam2.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) gmParam2.type()).getQualifiedName()).isEqualTo("W");

		// Method: public Map<List<? extends T>, Set<? super U>> getNestedGenericTypes()
		var getNestedGenericTypes = assertMethod(api, cg, "getNestedGenericTypes()");
		var ngtType = getNestedGenericTypes.getType();
		assertThat(ngtType.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) ngtType).typeArguments()).hasSize(2);
		var ngtArg1 = ((TypeReference<?>) ngtType).typeArguments().getFirst();
		assertThat(ngtArg1.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) ngtArg1).typeArguments()).hasSize(1);
		var ngtArg1Wc = ((TypeReference<?>) ngtArg1).typeArguments().getFirst();
		assertThat(ngtArg1Wc).isInstanceOf(WildcardTypeReference.class);
		var ngtArg1WcRef = (WildcardTypeReference) ngtArg1Wc;
		assertThat(ngtArg1WcRef.upper()).isTrue();
		assertThat(ngtArg1WcRef.bounds()).hasSize(1);
		assertThat(ngtArg1WcRef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) ngtArg1WcRef.bounds().getFirst()).getQualifiedName()).isEqualTo("T");
		var ngtArg2 = ((TypeReference<?>) ngtType).typeArguments().get(1);
		assertThat(ngtArg2.getQualifiedName()).isEqualTo("java.util.Set");
		assertThat(((TypeReference<?>) ngtArg2).typeArguments()).hasSize(1);
		var ngtArg2Wc = ((TypeReference<?>) ngtArg2).typeArguments().getFirst();
		assertThat(ngtArg2Wc).isInstanceOf(WildcardTypeReference.class);
		var ngtArg2WcRef = (WildcardTypeReference) ngtArg2Wc;
		assertThat(ngtArg2WcRef.upper()).isFalse();
		assertThat(ngtArg2WcRef.bounds()).hasSize(1);
		assertThat(ngtArg2WcRef.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) ngtArg2WcRef.bounds().getFirst()).getQualifiedName()).isEqualTo("U");

		// Method: public <E> List<E>[] createGenericArray(int size)
		var createGenericArray = assertMethod(api, cg, "createGenericArray(int)");
		assertThat(createGenericArray.getFormalTypeParameters()).hasSize(1);
		var eParam = createGenericArray.getFormalTypeParameters().getFirst();
		assertThat(eParam.name()).isEqualTo("E");
		assertThat(eParam.bounds()).hasSize(1);
		assertThat(eParam.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);
		assertThat(createGenericArray.getParameters()).hasSize(1);
		assertThat(createGenericArray.getParameters().getFirst().type().getQualifiedName()).isEqualTo("int");
		if (createGenericArray.getType() instanceof ArrayTypeReference atr) {
			var componentType = atr.componentType();
			assertThat(componentType.getQualifiedName()).isEqualTo("java.util.List");
			assertThat(((TypeReference<?>) componentType).typeArguments()).hasSize(1);
			var cgaArg = ((TypeReference<?>) componentType).typeArguments().getFirst();
			assertThat(cgaArg).isInstanceOf(TypeParameterReference.class);
			assertThat(((TypeParameterReference) cgaArg).getQualifiedName()).isEqualTo("E");
		}

		// Method: public void captureWildcard(List<?> unknownList)
		var captureWildcard = assertMethod(api, cg, "captureWildcard(java.util.List)");
		assertThat(captureWildcard.getParameters()).hasSize(1);
		var cwParam = captureWildcard.getParameters().getFirst();
		assertThat(cwParam.type().getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) cwParam.type()).typeArguments()).hasSize(1);
		var cwArg = ((TypeReference<?>) cwParam.type()).typeArguments().getFirst();
		assertThat(cwArg).isInstanceOf(WildcardTypeReference.class);
		var cwArgRef = (WildcardTypeReference) cwArg;
		assertThat(cwArgRef.upper()).isTrue();
		assertThat(cwArgRef.bounds()).hasSize(1);
		assertThat(cwArgRef.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);

		// Method: public static <Z> ComplexGenerics<?, ?, ?, ?> createComplexInstance()
		var createComplexInstance = assertMethod(api, cg, "createComplexInstance()");
		assertThat(createComplexInstance.isStatic()).isTrue();
		assertThat(createComplexInstance.getFormalTypeParameters()).hasSize(1);
		var zParam = createComplexInstance.getFormalTypeParameters().getFirst();
		assertThat(zParam.name()).isEqualTo("Z");
		assertThat(zParam.bounds()).hasSize(1);
		assertThat(zParam.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);
		var cciType = createComplexInstance.getType();
		assertThat(cciType.getQualifiedName()).isEqualTo("ComplexGenerics");
		assertThat(((TypeReference<?>) cciType).typeArguments()).hasSize(4);
		for (int i = 0; i < 4; i++) {
			var arg = ((TypeReference<?>) cciType).typeArguments().get(i);
			assertThat(arg).isInstanceOf(WildcardTypeReference.class);
			var wc = (WildcardTypeReference) arg;
			assertThat(wc.upper()).isTrue();
			assertThat(wc.bounds()).hasSize(1);
			assertThat(wc.bounds().getFirst()).isEqualTo(TypeReference.OBJECT);
		}

		// Nested Class: InnerClass<Q extends Map<T, W>>
		var innerClass = assertClass(api, "ComplexGenerics$InnerClass");
		assertThat(innerClass.getFormalTypeParameters()).hasSize(1);
		var qParam = innerClass.getFormalTypeParameters().getFirst();
		assertThat(qParam.name()).isEqualTo("Q");
		assertThat(qParam.bounds()).hasSize(1);
		var qBound = qParam.bounds().getFirst();
		assertThat(qBound.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) qBound).typeArguments()).hasSize(2);
		var qBoundArg1 = ((TypeReference<?>) qBound).typeArguments().getFirst();
		assertThat(qBoundArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) qBoundArg1).getQualifiedName()).isEqualTo("T");
		var qBoundArg2 = ((TypeReference<?>) qBound).typeArguments().get(1);
		assertThat(qBoundArg2).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) qBoundArg2).getQualifiedName()).isEqualTo("W");
		// Field: private Q nestedTypeField;
		var nestedTypeField = assertField(api, innerClass, "nestedTypeField");
		assertThat(nestedTypeField.getType()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) nestedTypeField.getType()).getQualifiedName()).isEqualTo("Q");
		// Method: public <R extends List<Q> & Serializable> Map<R, ? extends Q> innerMethod(R param)
		var innerMethod = assertMethod(api, innerClass, "innerMethod(java.util.List)");
		assertThat(innerMethod.getFormalTypeParameters()).hasSize(1);
		var rParamInner = innerMethod.getFormalTypeParameters().getFirst();
		assertThat(rParamInner.name()).isEqualTo("R");
		assertThat(rParamInner.bounds()).hasSize(2);
		var listQBound = rParamInner.bounds().getFirst();
		assertThat(listQBound.getQualifiedName()).isEqualTo("java.util.List");
		assertThat(((TypeReference<?>) listQBound).typeArguments()).hasSize(1);
		var listQArg = ((TypeReference<?>) listQBound).typeArguments().getFirst();
		assertThat(listQArg).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) listQArg).getQualifiedName()).isEqualTo("Q");
		var serBoundInner = rParamInner.bounds().get(1);
		assertThat(serBoundInner.getQualifiedName()).isEqualTo("java.io.Serializable");
		var imRet = innerMethod.getType();
		assertThat(imRet.getQualifiedName()).isEqualTo("java.util.Map");
		assertThat(((TypeReference<?>) imRet).typeArguments()).hasSize(2);
		var imArg1 = ((TypeReference<?>) imRet).typeArguments().getFirst();
		assertThat(imArg1).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) imArg1).getQualifiedName()).isEqualTo("R");
		var imArg2 = ((TypeReference<?>) imRet).typeArguments().get(1);
		assertThat(imArg2).isInstanceOf(WildcardTypeReference.class);
		var imWc = (WildcardTypeReference) imArg2;
		assertThat(imWc.upper()).isTrue();
		assertThat(imWc.bounds()).hasSize(1);
		assertThat(imWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) imWc.bounds().getFirst()).getQualifiedName()).isEqualTo("Q");
		assertThat(innerMethod.getParameters()).hasSize(1);
		var imParam = innerMethod.getParameters().getFirst();
		assertThat(imParam.type()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) imParam.type()).getQualifiedName()).isEqualTo("R");

		// Nested Interface: GenericInterface<S extends Comparable<? super S>>
		var genericInterface = assertInterface(api, "ComplexGenerics$GenericInterface");
		assertThat(genericInterface.isInterface()).isTrue();
		assertThat(genericInterface.getFormalTypeParameters()).hasSize(1);
		var sGi = genericInterface.getFormalTypeParameters().getFirst();
		assertThat(sGi.name()).isEqualTo("S");
		assertThat(sGi.bounds()).hasSize(1);
		var sGiBound = sGi.bounds().getFirst();
		assertThat(sGiBound.getQualifiedName()).isEqualTo("java.lang.Comparable");
		assertThat(((TypeReference<?>) sGiBound).typeArguments()).hasSize(1);
		var sGiBoundArg = ((TypeReference<?>) sGiBound).typeArguments().getFirst();
		assertThat(sGiBoundArg).isInstanceOf(WildcardTypeReference.class);
		var sGiWc = (WildcardTypeReference) sGiBoundArg;
		assertThat(sGiWc.upper()).isFalse();
		assertThat(sGiWc.bounds()).hasSize(1);
		assertThat(sGiWc.bounds().getFirst()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) sGiWc.bounds().getFirst()).getQualifiedName()).isEqualTo("S");
		// Method: S method();
		var giMethod = assertMethod(api, genericInterface, "method()");
		assertThat(giMethod.getType()).isInstanceOf(TypeParameterReference.class);
		assertThat(((TypeParameterReference) giMethod.getType()).getQualifiedName()).isEqualTo("S");
	}
}
