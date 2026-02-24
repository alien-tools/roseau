package fixture.full;

public class IntersectionGeneric<T extends Number & Comparable<T>> {
	public T value;

	public IntersectionGeneric() {
		// no-op
	}

	public T project(T input) {
		return input;
	}
}
