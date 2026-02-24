package fixture.full;

public class UncheckedProblem extends RuntimeException {
	public UncheckedProblem() {
		super();
	}

	public UncheckedProblem(String message) {
		super(message);
	}
}
