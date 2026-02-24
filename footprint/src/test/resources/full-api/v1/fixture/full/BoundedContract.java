package fixture.full;

public interface BoundedContract<T extends Number> {
	T merge(T left, T right) throws CheckedProblem;

	default T identity(T value) {
		return value;
	}
}
