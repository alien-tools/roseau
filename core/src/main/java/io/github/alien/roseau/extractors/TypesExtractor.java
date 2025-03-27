package io.github.alien.roseau.extractors;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;

import java.nio.file.Path;
import java.util.List;

/**
 * An {@link TypesExtractor} is responsible for extracting an {@link LibraryTypes} from a supplied source (e.g., source code or
 * bytecode of a library).
 * <p>
 * API extractors must keep track of <strong>all</strong> types in an API, including non-exported/accessible ones. This
 * is necessary for type resolution later and to handle potentially-leaked internal types.
 */
public interface TypesExtractor {
	/**
	 * Extracts a new {@link LibraryTypes} from the source located at {@code sources} using the supplied {@code classpath}.
	 *
	 * @param sources   the file or directory to analyze
	 * @param classpath a classpath to resolve types with
	 * @return the extracted {@link LibraryTypes}
	 * @throws RoseauException if anything went wrong
	 */
	LibraryTypes extractTypes(Path sources, List<Path> classpath);

	/**
	 * Extracts a new {@link LibraryTypes} from the source located at {@code sources}.
	 *
	 * @param sources the file or directory to analyze
	 * @return the extracted {@link LibraryTypes}
	 * @throws RoseauException if anything went wrong
	 */
	default LibraryTypes extractTypes(Path sources) {
		return extractTypes(sources, List.of());
	}

	/**
	 * Checks whether this extractor can handle the given {@code sources}.
	 *
	 * @param sources The file or directory to check
	 * @return true if this extractor handles the given {@code sources}
	 */
	boolean canExtract(Path sources);

	/**
	 * Returns a user-friendly name for this extractor
	 *
	 * @return this extractor's name
	 */
	String getName();
}
