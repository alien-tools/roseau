package io.github.alien.roseau.api.model;

import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EqualityTest {
	@Test
	void api_order_doesnt_matter() {
		var sources1 = """
			package p1;
			public @interface Marker {}
			public @interface Ann {
				int value() default 1;
				String[] names() default {};
				Class<?> type() default Object.class;
			}
			public record R<T>(int id, String name, T data) implements java.io.Serializable {
				public static final int CONST = 42;
				public R { if (id < 0) id = -id; }
				public R(int id, String name) { this(id, name, null); }
				public void m() {}
				public static class Nested {}
				public interface NI {}
				public enum NE { X, Y }
				public @interface NA { int x() default 0; }
			}
			public enum E {
				A(1), B(2);
				private final int code;
				E(int c) { this.code = c; }
				public int getCode() { return code; }
				public class Inner {}
				public static class S {}
			}
			@Deprecated @Ann(2)
			public class C<U> {
				public U field;
				public int[] arr;
				public java.util.List<? extends Number> nums;
				public C() {}
				@Ann(3) public C(@Marker String s, @Ann(4) U u) throws java.io.IOException {}
				public <X extends Number & java.io.Serializable> X method(@Ann int a, @Marker String... rest)
					throws java.lang.Exception { return null; }
				public static <Y> Y smethod(Y y) { return y; }
				public void m2() {}
				public void m1(int i) {}
				public class Inner { public void im() {} }
				public static class SInner { public void sm() {} }
				public interface IF { default void d() {} static void s() {} }
				public record Rec(int a, @Ann String b) {}
				public @interface IA { String k() default "v"; }
			}
			package p2;
			@FunctionalInterface
			@Deprecated
			public interface I {
				void m(String s);
				default void d() {}
				static int util() { return 0; }
				class Nested {}
				enum NE2 { C, D }
				@interface A2 { int v(); }
			}
			public class D {
				public D() {}
				public D(int x){}
			}""";
		var sources2 = """
			package p2;
			public class D {
				public D(int x) {}
				public D() {}
			}
			@Deprecated
			@FunctionalInterface
			public interface I {
				static int util() { return 0; }
				default void d() {}
				void m(String s);
				@interface A2 { int v(); }
				enum NE2 { D, C }
				class Nested {}
			}
			package p1;
			public @interface Ann {
				Class<?> type() default Object.class;
				String[] names() default {};
				int value() default 1;
			}
			public @interface Marker {}
			public enum E {
				B(2), A(1);
				public static class S {}
				public class Inner {}
				private final int code;
				E(int c) { this.code = c; }
				public int getCode() { return code; }
			}
			public record R<T>(int id, String name, T data) implements java.io.Serializable {
				public @interface NA { int x() default 0; }
				public enum NE { Y, X }
				public interface NI {}
				public static class Nested {}
				public void m() {}
				public R(int id, String name) { this(id, name, null); }
				public static final int CONST = 42;
				public R { if (id < 0) id = -id; }
			}
			@Ann(2) @Deprecated
			public class C<U> {
				public java.util.List<? extends Number> nums;
				public int[] arr;
				public U field;
				public static <Y> Y smethod(Y y) { return y; }
				public <X extends Number & java.io.Serializable> X method(@Ann int a, @Marker String... rest)
					throws java.lang.Exception { return null; }
				@Ann(3) public C(@Marker String s, @Ann(4) U u) throws java.io.IOException {}
				public C() {}
				public void m1(int i) {}
				public void m2() {}
				public static class SInner { public void sm() {} }
				public class Inner { public void im() {} }
				public record Rec(int a, @Ann String b) {}
				public interface IF { static void s() {} default void d() {} }
				public @interface IA { String k() default "v"; }
			}""";

		var apis = Arrays.stream(ApiBuilderType.values())
			.flatMap(t -> Stream.of(sources1, sources2).map(t::build))
			.toList();

		var baseline = apis.getFirst();
		apis.forEach(api -> {
			var apiTypes = api.getLibraryTypes().getAllTypes();
			var baselineTypes = baseline.getLibraryTypes().getAllTypes();
			assertThat(apiTypes).isEqualTo(baselineTypes);
		});
	}
}
