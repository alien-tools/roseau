package fixture.full;

public class GenericThrower {
	public GenericThrower() {
		// no-op
	}

	public <X extends CheckedProblem> void fail() throws X {
		// no-op
	}
}
