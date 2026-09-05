package io.github.alien.roseau.cli;

import java.io.PrintWriter;

class Console {
	private final PrintWriter out;
	private final PrintWriter err;
	private final Verbosity verbosity;

	enum Verbosity {
		NORMAL(0),
		VERBOSE(1),
		DEBUG(2);

		final int level;

		Verbosity(int level) {
			this.level = level;
		}
	}

	Console(PrintWriter out, PrintWriter err, Verbosity verbosity) {
		this.out = out;
		this.err = err;
		this.verbosity = verbosity;
	}

	void println(String message) {
		out.println(message);
	}

	void printlnErr(String message) {
		err.println(message);
	}

	void printlnVerbose(String message) {
		if (verbosity.level >= Verbosity.VERBOSE.level) {
			printlnErr(message);
		}
	}

	void printlnDebug(String message) {
		if (verbosity.level >= Verbosity.DEBUG.level) {
			printlnErr(message);
		}
	}

	void printVerbose(String message) {
		if (verbosity.level >= Verbosity.VERBOSE.level) {
			err.print(message);
			err.flush();
		}
	}

	void printStackTrace(Throwable t) {
		t.printStackTrace(err);
	}
}
