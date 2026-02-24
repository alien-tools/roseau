package fixture.full;

public record PairRecord<T>(T left, T right) {
	public static <U> PairRecord<U> of(U left, U right) {
		return new PairRecord<>(left, right);
	}
}
