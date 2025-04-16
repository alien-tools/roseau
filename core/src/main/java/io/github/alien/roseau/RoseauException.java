package io.github.alien.roseau;

/**
 * A generic runtime exception for Roseau-related exceptions.
 */
public final class RoseauException extends RuntimeException {
	public RoseauException(String message) {
		super(message);
	}

	public RoseauException(String message, Throwable cause) {
		super(message, cause);
	}
}
