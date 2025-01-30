package com.github.maracas.roseau.combinatorial;

public class StepExecutionException extends RuntimeException {
	public StepExecutionException(String step, String message) {
		super(step + " - " + message);
	}
}
