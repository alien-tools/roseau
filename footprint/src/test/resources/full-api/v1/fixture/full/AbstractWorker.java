package fixture.full;

public abstract class AbstractWorker {
	public static final long VERSION = 1L;
	protected String name;

	protected AbstractWorker() {
		this.name = "worker";
	}

	protected AbstractWorker(String name) throws CheckedProblem {
		if (name == null) {
			throw new CheckedProblem("name");
		}
		this.name = name;
	}

	public abstract String work(String input) throws CheckedProblem;

	protected abstract int protectedWork(int input) throws DerivedCheckedProblem;

	public String label() {
		return name;
	}
}
