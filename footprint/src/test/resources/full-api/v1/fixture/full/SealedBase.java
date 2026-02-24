package fixture.full;

public sealed class SealedBase permits SealedLeaf, SealedMiddle {
	public int id() {
		return 1;
	}
}
