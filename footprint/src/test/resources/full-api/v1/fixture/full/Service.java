package fixture.full;

public interface Service {
	Number compute(Number input) throws CheckedProblem;

	default Number identity(Number value) {
		return value;
	}

	static <U> U passthrough(U value) {
		return value;
	}
}
