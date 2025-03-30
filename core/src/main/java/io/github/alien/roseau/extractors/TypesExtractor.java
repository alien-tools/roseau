package io.github.alien.roseau.extractors;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;

import java.nio.file.Path;

/**
 * A {@link TypesExtractor} is responsible for extracting {@link LibraryTypes} from a given {@link Library}. Types
 * extractors must keep track of <strong>all</strong> types in a library, including non-exported/accessible ones. This
 * is necessary for type resolution later and to handle potentially-leaked internal types.
 */
public interface TypesExtractor {
	/**
	 * Extracts a new {@link LibraryTypes} from the given {@link Library}.
	 *
	 * @param library the library to extract types from
	 * @return the extracted {@link LibraryTypes}
	 * @throws RoseauException if anything went wrong
	 */
	LibraryTypes extractTypes(Library library);

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
