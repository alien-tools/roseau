package fixture.full;

public record DataRecord(Number value, String name) implements Service {
	public DataRecord {
		if (name == null) {
			throw new IllegalArgumentException("name");
		}
	}

	@Override
	public Number compute(Number input) {
		return input;
	}

	public static DataRecord of(int value) {
		return new DataRecord(value, "v" + value);
	}
}
