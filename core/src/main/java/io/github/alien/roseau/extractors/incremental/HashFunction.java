package io.github.alien.roseau.extractors.incremental;

import net.openhft.hashing.LongHashFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An interface for file hash functions. Implementations of this interface compute hash values for files, enabling the
 * identification of changes between different versions of files.
 */
public interface HashFunction {
	/**
	 * Computes the hash of the specified file. Implementers should return {@code -1L} if the hash cannot be computed.
	 *
	 * @param file the hashed file
	 * @return the computed hash value as a long, or {@code -1L} if an error occurs during hash computation
	 */
	long hash(Path file);

	/**
	 * A default implementation that uses the non-cryptographic xxHash algorithm. Thus, there is a (very unlikely) chance
	 * of collisions.
	 */
	HashFunction XXHASH = file -> {
		try {
			return LongHashFunction.xx().hashBytes(Files.readAllBytes(file));
		} catch (IOException e) {
			return -1L;
		}
	};
}
