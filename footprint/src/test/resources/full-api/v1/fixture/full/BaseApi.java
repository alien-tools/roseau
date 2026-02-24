package fixture.full;

import java.util.List;

@FullMarker("base")
public class BaseApi {
	public static int STATIC_COUNTER = 1;
	public static final String STATIC_FINAL = "sf";

	public Number value;
	public final String immutable = "immutable";

	protected int protectedMutable = 7;
	protected static String PROTECTED_STATIC = "ps";

	public BaseApi() {
		// no-op
	}

	public BaseApi(Number value) throws CheckedProblem {
		this.value = value;
		if (value == null) {
			throw new CheckedProblem("value");
		}
	}

	protected BaseApi(String text) throws DerivedCheckedProblem {
		if (text == null) {
			throw new DerivedCheckedProblem("text");
		}
	}

	public Number getValue() {
		return value;
	}

	public void setValue(Number value) {
		this.value = value;
	}

	public <X> X echo(X x) throws CheckedProblem {
		if (x == null) {
			throw new CheckedProblem("x");
		}
		return x;
	}

	public void varargs(String label, int... values) {
		// no-op
	}

	public static String staticCall(String input) throws DerivedCheckedProblem {
		if (input == null) {
			throw new DerivedCheckedProblem("input");
		}
		return input.toUpperCase();
	}

	public final void finalMethod() {
		// no-op
	}

	protected void protectedMethod() throws CheckedProblem {
		if (value == null) {
			throw new CheckedProblem("value");
		}
	}

	protected static void protectedStaticMethod() {
		// no-op
	}

	public synchronized int syncMethod(int left, int right) {
		return left + right;
	}

	public static class PublicStaticNested {
		public static long STATIC_NESTED = 11L;
		public Object nestedValue;
		protected int protectedCounter = 0;

		public PublicStaticNested() {
			// no-op
		}

		public PublicStaticNested(Object nestedValue) throws CheckedProblem {
			this.nestedValue = nestedValue;
			if (nestedValue == null) {
				throw new CheckedProblem("nestedValue");
			}
		}

		public Object nestedValue() {
			return nestedValue;
		}

		public <R> R transform(R input) {
			return input;
		}

		protected void protectedNestedMethod() {
			protectedCounter++;
		}

		protected static void protectedStaticNestedMethod() {
			STATIC_NESTED++;
		}

		public static void staticNestedCall() {
			STATIC_NESTED++;
		}
	}

	public class PublicInner {
		public int innerValue;
		protected String protectedInner = "inner";

		public PublicInner(int innerValue) {
			this.innerValue = innerValue;
		}

		public int readOuter() {
			return mutableOrZero() + innerValue;
		}

		protected void protectedInnerMethod() {
			innerValue++;
		}

		private int mutableOrZero() {
			return value == null ? 0 : value.intValue();
		}
	}

	public interface NestedPublicInterface {
		void run() throws CheckedProblem;

		default int size() {
			return 1;
		}

		static NestedPublicInterface noop() {
			return () -> {
				// no-op
			};
		}
	}

	public enum NestedEnum {
		FIRST,
		SECOND;

		public int index() {
			return ordinal();
		}
	}

	public record NestedRecord(int x, String y) {
	}

	public @interface NestedAnn {
		String value() default "nested";

		Class<?> type() default Object.class;
	}

	protected static class ProtectedNested {
		public int value;

		protected ProtectedNested(int value) {
			this.value = value;
		}

		public int value() {
			return value;
		}
	}
}
