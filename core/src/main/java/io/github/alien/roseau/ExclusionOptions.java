package io.github.alien.roseau;

import io.github.alien.roseau.api.model.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Exclusion options to customize API inference and delimitation.
 *
 * @param excludedSymbols     regex-based list of excluded symbols
 * @param excludedAnnotations list of @Annotations to mark symbols as excluded
 */
public record ExclusionOptions(
	List<Pattern> excludedSymbols,
	List<String> excludedAnnotations
) {
	public ExclusionOptions {
		Objects.requireNonNull(excludedSymbols);
		Objects.requireNonNull(excludedAnnotations);
	}

	/**
	 * Checks whether the supplied {@link Symbol} is excluded.
	 *
	 * @param symbol the symbol to check
	 * @return true if this symbol is excluded
	 */
	public boolean isExcluded(Symbol symbol) {
		if (excludedAnnotations.stream().anyMatch(symbol::isAnnotatedWith)) {
			return true;
		}

		return excludedSymbols.stream()
			.anyMatch(pattern -> pattern.matcher(symbol.getQualifiedName()).matches());
	}

	/**
	 * Returns default options, with no symbol excluded.
	 *
	 * @return default options
	 */
	public static ExclusionOptions defaultOptions() {
		return new ExclusionOptions(List.of(), List.of());
	}

	/**
	 * Returns a new builder of {@link ExclusionOptions}.
	 *
	 * @return the builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A builder for {@link ExclusionOptions}.
	 */
	public static final class Builder {
		private List<Pattern> excludedSymbols = new ArrayList<>();
		private List<String> excludedAnnotations = new ArrayList<>();

		private Builder() {

		}

		/**
		 * Excludes a list of symbols.
		 *
		 * @param excludedSymbols regex-based list of symbols to exclude
		 * @return the builder
		 * @see #excludeSymbol(String)
		 */
		public Builder excludeSymbols(List<String> excludedSymbols) {
			this.excludedSymbols = excludedSymbols.stream()
				.map(Builder::compileSafely)
				.flatMap(Optional::stream)
				.toList();
			return this;
		}

		/**
		 * Excludes a single symbol.
		 *
		 * @param excludedSymbol regex-based symbol to exclude
		 * @return the builder
		 * @see Pattern#compile(String)
		 */
		public Builder excludeSymbol(String excludedSymbol) {
			Optional<Pattern> pattern = compileSafely(excludedSymbol);
			pattern.ifPresent(value -> this.excludedSymbols.add(value));
			return this;
		}

		/**
		 * Excludes symbols marked with the provided annotations.
		 *
		 * @param excludedAnnotations a list of fully qualified annotation names
		 * @return the builder
		 * @see #excludeAnnotation(String)
		 */
		public Builder excludeAnnotations(List<String> excludedAnnotations) {
			this.excludedAnnotations = List.copyOf(excludedAnnotations);
			return this;
		}

		/**
		 * Excludes symbols marked with the provided annotation.
		 *
		 * @param excludedAnnotation a fully qualified annotation name
		 * @return the builder
		 */
		public Builder excludeAnnotation(String excludedAnnotation) {
			this.excludedAnnotations.add(excludedAnnotation);
			return this;
		}

		/**
		 * Builds the resulting {@link ExclusionOptions} object.
		 *
		 * @return the built options
		 */
		public ExclusionOptions build() {
			return new ExclusionOptions(excludedSymbols, excludedAnnotations);
		}

		private static Optional<Pattern> compileSafely(String regex) {
			try {
				return Optional.of(Pattern.compile(regex));
			} catch (PatternSyntaxException e) {
				return Optional.empty();
			}
		}
	}
}
