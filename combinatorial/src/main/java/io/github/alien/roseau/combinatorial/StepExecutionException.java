package io.github.alien.roseau.combinatorial;

public class StepExecutionException extends Exception {
	public StepExecutionException(String step, String message) {
		super(step + " - " + message);
	}
}
