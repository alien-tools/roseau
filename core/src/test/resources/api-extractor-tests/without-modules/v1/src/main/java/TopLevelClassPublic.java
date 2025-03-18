// Top-level classes are either public or package-private
import java.util.List;


public class TopLevelClassPublic {
	public static String AString;
	// Inner classes
	class InnerClass {}

	public class originalClass {}
	public class InnerClassPublic extends originalClass {
		public int number;
		public List<String> text;

		public InnerClassPublic(int number, List<String> text) {
			this.number = number;
			this.text = text;
		}

		public void m() {
			System.out.println("InnerClassPublic - method m()");
		}

		public default void varargsMethod(int[] number, String... texts) {}

		public static int NumMethod1() {
			return number;
		}

		public void NumMethod2(int number) {
			this.number = number;
		}


	}

	protected class InnerClassProtected {

		public List<String> exceptions;

		public List<T> processList(List<T> list, List<Boolean> strings) {

			return list;
		}

		public void throwExceptions() throws IOException {

			throw new IOException("An I/O exception occurred.");

		}
	}

	private class InnerClassPrivate {}
	public class CustomCheckedException extends Exception {
		// Constructor
		public CustomCheckedException(String message) {
			super(message);
		}
	}
	// Nested classes

	public interface Interface1 {
		void method1();
	}

	public interface Interface2 {
		void method2();
	}

	public class InnerClassWithInterfaces implements Interface1, Interface2 {

	}
	static class NestedClass {}
	public class NestedClassPublic extends InnerClassPublic {}
	public class NestedClassProtected extends InnerClassProtected {}
	private static class NestedClassPrivate {}

	// Nested interfaces are implicitly static (no inner interfaces)
	interface NestedInterface {}
	public interface NestedInterfacePublic {}
	protected interface NestedInterfaceProtected {}
	private interface NestedInterfacePrivate {}

	// Nested records are implicitly static (no inner records)
	record NestedRecord() {}
	public record NestedRecordPublic() {}
	protected record NestedRecordProtected() {}
	private record NestedRecordPrivate() {}

	// Nested enums are implicitly static (no inner enums)
	enum NestedEnum {}
	public enum NestedEnumPublic {}
	protected enum NestedEnumProtected {}
	private enum NestedEnumPrivate {}

	// Annotations are interfaces so nested annotations are implicitly static (no inner annotations)
	@interface NestedAnnotation {}
	public @interface NestedAnnotationPublic {}
	protected @interface NestedAnnotationProtected {}
	private @interface NestedAnnotationPrivate {}
}
