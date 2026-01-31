package io.github.alien.roseau.cli;

import java.io.PrintWriter;

class Console {
	private PrintWriter out;
	private PrintWriter err;
	private Verbosity verbosity;

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

	void printlnVerbose(String message) {
		if (verbosity.level >= Verbosity.VERBOSE.level) {
			println(message);
		}
	}

	void printlnDebug(String message) {
		if (verbosity.level >= Verbosity.DEBUG.level) {
			println(message);
		}
	}

	void printlnErr(String message) {
		err.println(message);
	}

	void print(String message) {
		out.print(message);
		out.flush();
	}

	void printVerbose(String message) {
		if (verbosity.level >= Verbosity.VERBOSE.level) {
			print(message);
		}
	}

	void printStackTrace(Throwable t) {
		t.printStackTrace(err);
	}
}
