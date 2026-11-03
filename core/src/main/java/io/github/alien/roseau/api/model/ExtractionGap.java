package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;

import java.util.Comparator;

/**
 * A declaration that could not be extracted faithfully from the sources, typically because a type it uses is missing
 * from the classpath. The extracted model lacks the declaration, or holds a distorted version of it (e.g., a supertype
 * or a bound replaced with {@code java.lang.Object}). Gaps are only produced by source extraction.
 *
 * @param declaringType the qualified name of the type whose declaration is lossy, or {@code null} for file-level gaps
 * @param declaration   a human-readable name of the lossy declaration (e.g., {@code pkg.A.f}, {@code pkg.A.m(B)}, or
 *                      {@code pkg.A} for a type header)
 * @param reference     the best-effort qualified name of the type that could not be resolved, or {@code null} when
 *                      there is none
 * @param location      the location of the offending source node
 * @param reason        why the declaration could not be extracted
 */
public record ExtractionGap(
	String declaringType,
	String declaration,
	String reference,
	SourceLocation location,
	Reason reason
) implements Comparable<ExtractionGap> {
	/**
	 * Why a declaration could not be extracted faithfully.
	 */
	public enum Reason {
		/**
		 * The declaration's signature uses a type that could not be resolved
		 */
		UNRESOLVED_TYPE,
		/**
		 * The parser produced no binding for the declaration
		 */
		NO_BINDING,
		/**
		 * The parser never delivered the file
		 */
		UNPARSED_FILE,
		/**
		 * The value of a constant field could not be computed
		 */
		UNKNOWN_CONSTANT
	}

	private static final Comparator<ExtractionGap> ORDER =
		Comparator.comparing((ExtractionGap gap) -> String.valueOf(gap.location().file()))
			.thenComparingInt(gap -> gap.location().line())
			.thenComparing(ExtractionGap::declaration)
			.thenComparing(ExtractionGap::reason)
			.thenComparing(ExtractionGap::reference, Comparator.nullsFirst(Comparator.naturalOrder()));

	public ExtractionGap {
		Preconditions.checkNotNull(declaration);
		Preconditions.checkNotNull(location);
		Preconditions.checkNotNull(reason);
	}

	@Override
	public int compareTo(ExtractionGap other) {
		return ORDER.compare(this, other);
	}

	@Override
	public String toString() {
		String detail = switch (reason) {
			case UNRESOLVED_TYPE -> reference;
			case NO_BINDING -> "no binding";
			case UNPARSED_FILE -> "not parsed";
			case UNKNOWN_CONSTANT -> "unknown constant value";
		};
		return "%s (%s, %s)".formatted(declaration, location, detail);
	}
}
