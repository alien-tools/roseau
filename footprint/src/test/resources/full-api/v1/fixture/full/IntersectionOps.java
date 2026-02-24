package fixture.full;

public class IntersectionOps {
	public <T extends Number & Comparable<T>> T pick(T left, T right) {
		return left;
	}

	public <T extends Runnable & AutoCloseable> void runAndClose(T value) throws Exception {
		if (value != null) {
			value.run();
			value.close();
		}
	}
}
