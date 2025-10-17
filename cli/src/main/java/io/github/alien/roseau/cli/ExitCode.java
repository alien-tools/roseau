package io.github.alien.roseau.cli;

enum ExitCode {
	SUCCESS(0),
	BREAKING(1),
	ERROR(2);

	private final int code;

	ExitCode(int code) {
		this.code = code;
	}

	int getCode() {
		return code;
	}
}
