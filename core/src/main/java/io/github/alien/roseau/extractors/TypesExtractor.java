package io.github.alien.roseau.extractors;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;

/**
 * A {@link TypesExtractor} is responsible for extracting {@link LibraryTypes} from a given {@link Library}. Types
 * extractors must keep track of <strong>all</strong> types in a library, including non-exported/accessible ones. This
 * is necessary for type resolution later and to handle potentially leaked internal types.
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
}
