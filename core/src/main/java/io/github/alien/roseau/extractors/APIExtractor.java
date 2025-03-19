package io.github.alien.roseau.extractors;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;

import java.nio.file.Path;
import java.util.List;

/**
 * An {@link APIExtractor} is responsible for extracting an {@link API} from a supplied source (e.g., source code or
 * bytecode of a library).
 * <p>
 * API extractors must keep track of <strong>all</strong> types in an API, including non-exported/accessible ones. This
 * is necessary for type resolution later and to handle potentially-leaked internal types.
 */
public interface APIExtractor {
	/**
	 * Extracts a new {@link API} from the source located at {@code sources} using the supplied {@code classpath}.
	 *
	 * @param sources   the file or directory to analyze
	 * @param classpath a classpath to resolve types with
	 * @return the extracted {@link API}
	 * @throws RoseauException if anything went wrong
	 */
	API extractAPI(Path sources, List<Path> classpath);

	/**
	 * Extracts a new {@link API} from the source located at {@code sources}.
	 *
	 * @param sources the file or directory to analyze
	 * @return the extracted {@link API}
	 * @throws RoseauException if anything went wrong
	 */
	default API extractAPI(Path sources) {
		return extractAPI(sources, List.of());
	}

	/**
	 * Checks whether this extractor can handle the given {@code sources}.
	 *
	 * @param sources The file or directory to check
	 * @return true if this extractor handles the given {@code sources}
	 */
	boolean canExtract(Path sources);
}
