package fixture.full;

public class BoundedOps {
	public <U extends Number> U pick(U left, U right) throws CheckedProblem {
		if (left == null) {
			throw new CheckedProblem("left");
		}
		return left;
	}

	@SafeVarargs
	public final <U extends Number> U[] array(U... values) {
		return values;
	}
}
