/**
 * A GPT4-generated file showcasing Java 17 API-related constructs
 */
package io.github.alien.roseau;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class APIShowcase {
	public @interface MyAnnotation {
		String value() default "default";

		int[] counters() default {};
	}

	// Use of sealed classes to restrict subclasses (part of JEP 409 in Java 17)
	public sealed interface Shape permits Circle, Rectangle, Square {
		double area();
	}

	final class Circle implements Shape {
		private final double radius;

		Circle(double radius) {
			this.radius = radius;
		}

		public double area() {
			return Math.PI * radius * radius;
		}
	}

	non-sealed class Rectangle implements Shape {
		private final double length, width;

		Rectangle(double length, double width) {
			this.length = length;
			this.width = width;
		}

		public double area() {
			return length * width;
		}
	}

	// Record class (compact constructor syntax, pattern matching for instanceof)
	public record Point(int x, int y) {
	}

	// Enum with methods and fields
	enum Color {
		RED, GREEN, BLUE;

		void print() {
			System.out.println(this.name());
		}
	}

	// Generic class with bounded type parameter
	class Box<T extends Number> {
		private T t;

		public void set(T t) {
			this.t = t;
		}

		public T get() {
			return t;
		}
	}

	// Interface demonstrating generics, default, and static methods
	interface MyGenericInterface<T, U> {
		T compute(U u);

		default void printU(U u) {
			System.out.println(u);
		}

		static <R> R staticMethod(R r) {
			return r;
		}
	}

	// Abstract class showcasing various modifiers and member types
	abstract class AbstractShowcase<T> implements Serializable {
		public static int staticVar = 10;
		protected volatile boolean flag;
		private T value;

		abstract T getValue();

		abstract void setValue(T value);

		class InnerClass {
			public void display() {
				System.out.println("InnerClass Display");
			}
		}

		static class StaticNestedClass {
			public static void staticNestedMethod() {
				System.out.println("StaticNested Method");
			}
		}
	}

	// Demonstrating a functional interface (useful for lambda expressions)
	@FunctionalInterface
	interface SimpleFunction<T> {
		T apply(T t);
	}

	// Example class encapsulating usage of various constructs
	public class Java17ApiShowcase {

		// Field examples with different modifiers
		private int privateInt;
		protected String protectedString;
		public static final String CONSTANT = "CONSTANT";

		// Constructor
		Java17ApiShowcase(int privateInt, String protectedString) {
			this.privateInt = privateInt;
			this.protectedString = protectedString;
		}

		// Method showcasing varargs, generics, and wildcard types
		public <T> void genericMethod(T t, List<? extends T> list, SimpleFunction<T>... functions) {
		}

		// Static method example
		public static void staticMethod(String[] args) {
		}

		// Example of a method using an enum and record
		public void useEnumAndRecord(Color color, Point point) {
		}

		// Private method (note: private methods in interfaces were introduced in Java 9)
		private void privateMethod() {
		}

		// Main method to avoid instantiation error
		public static void main(String[] args) {
			System.out.println("Java 17 API Showcase");
		}
	}

	public class AdditionalJava17ApiShowcase {

		// Demonstrate a class with multiple interface implementations
		public static class MultiInterfaceImpl implements Serializable, Cloneable {
			private transient String sensitiveData; // transient field example

			// synchronized method example
			public synchronized void threadSafeAction() {
			}

			// Clone method override from Cloneable interface
			@Override
			protected Object clone() throws CloneNotSupportedException {
				return super.clone();
			}
		}

		// Use of strictfp to ensure IEEE 754 compliance for floating-point operations
		public strictfp class StrictFpMath {
			public double sum(double a, double b) {
				return a + b; // The sum will be strictly compliant with IEEE 754
			}
		}

		// Demonstrating a generic method with varargs and a bounded wildcard
		public <T extends Comparable<T>> void sortVarargsCollections(Collection<? extends T>... collections) {
		}

		// Interface with a generic method that has multiple type parameters and extends Comparable
		interface ComplexGenericMethod {
			<T, U extends Collection<T> & Serializable> T findMax(U collection, Comparator<? super T> comparator);
		}

		// Native method demonstration (requires JNI to implement)
		public native void performNativeOperation();

		// Demonstrating the use of instanceof with pattern matching (introduced in Java 16)
		public static <T> boolean isInstanceOfPatternMatching(Object obj) {
			return obj instanceof String s && s.contains("example");
		}

		// Main method to demonstrate the execution
		public static void main(String[] args) {
			System.out.println("Showcasing additional Java 17 constructs for API definitions.");
		}
	}

	public final class Square implements Shape {
		private final double radius;

		Square(double radius) {
			this.radius = radius;
		}

		public double area() {
			return Math.PI * radius * radius;
		}
	}
}
