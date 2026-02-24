package fixture.full;

public interface GenericContract<T> {
	T head(T value) throws CheckedProblem;

	default T tail(T value) {
		return value;
	}

	static <U> U id(U value) {
		return value;
	}
}
