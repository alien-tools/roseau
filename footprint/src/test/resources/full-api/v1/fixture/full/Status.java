package fixture.full;

public enum Status {
	NEW(0),
	READY(1),
	DONE(2);

	private final int code;

	Status(int code) {
		this.code = code;
	}

	public int code() {
		return code;
	}

	public static Status fromCode(int code) {
		return switch (code) {
			case 0 -> NEW;
			case 1 -> READY;
			default -> DONE;
		};
	}
}
