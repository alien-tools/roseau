package com.github.maracas.roseau;

public class RoseauException extends RuntimeException {
	public RoseauException(String message) {
		super(message);
	}

	public RoseauException(String message, Throwable cause) {
		super(message, cause);
	}
}
