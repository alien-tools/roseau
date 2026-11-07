package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The JDK types available in a given JDK release, read from a JDK's {@code lib/ct.sym}, the same way
 * {@code javac --release} does.
 * <p>
 * Reading JDK types from ct.sym (and not the running JVM's modules) makes resolution independent of the modules
 * the host runtime (e.g., a jlink image) contains. As with {@code javac --release}, only the exported JDK APIs are
 * visible: most JDK internal types are not.
 */
public final class CtSymPlatform {
	private final ZipFile zip;
	// Class file entry name (e.g., java/util/List.class) -> ct.sym entry name
	private final Map<String, String> entries;

	private static final Path HOST_CT_SYM = Path.of(System.getProperty("java.home"), "lib", "ct.sym");
	// One platform per release, opened once and shared for the lifetime of the JVM
	private static final ConcurrentMap<Integer, Optional<CtSymPlatform>> HOST_PLATFORMS = new ConcurrentHashMap<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(CtSymPlatform.class);

	private CtSymPlatform(ZipFile zip, Map<String, String> entries) {
		this.zip = zip;
		this.entries = entries;
	}

	/**
	 * Opens the platform of the given release from the given ct.sym file.
	 *
	 * @param ctSym   the path to a JDK's {@code lib/ct.sym}
	 * @param release the Java release (e.g., 8, 21)
	 * @return the platform, or empty if ct.sym cannot be read or does not provide that release
	 */
	static Optional<CtSymPlatform> open(Path ctSym, int release) {
		Preconditions.checkNotNull(ctSym);
		if (release < 0 || release >= Character.MAX_RADIX) {
			LOGGER.debug("{} cannot describe Java release {}", ctSym, release);
			return Optional.empty();
		}
		if (!Files.isRegularFile(ctSym)) {
			LOGGER.debug("{} does not exist", ctSym);
			return Optional.empty();
		}

		ZipFile zip = null;
		try {
			zip = new ZipFile(ctSym.toFile());
			Map<String, String> entries = index(zip, release);
			if (entries.isEmpty()) {
				LOGGER.debug("{} does not provide Java release {}", ctSym, release);
				zip.close();
				return Optional.empty();
			}

			LOGGER.debug("Indexed {} JDK types of Java release {} from {}", entries.size(), release, ctSym);
			return Optional.of(new CtSymPlatform(zip, entries));
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Failed to read {}: {}", ctSym, e.getMessage());
			closeQuietly(zip);
			return Optional.empty();
		}
	}

	/**
	 * Returns the platform of the given release from the running JDK's {@code lib/ct.sym}, opening it on first use.
	 *
	 * @param release the Java feature release (e.g., 8, 21)
	 * @return the platform, or empty if the running JDK has no usable ct.sym (e.g., a JRE) or it lacks that release
	 */
	static Optional<CtSymPlatform> forRelease(int release) {
		return HOST_PLATFORMS.computeIfAbsent(release, r -> open(HOST_CT_SYM, r));
	}

	/**
	 * Opens the class file of a JDK type.
	 *
	 * @param classEntry the type's class file name (e.g., {@code java/util/Map$Entry.class})
	 * @return a stream of the class file, which the caller must close, or {@code null} if the release has no such type
	 * @throws IOException if the class file cannot be read
	 */
	InputStream openClass(String classEntry) throws IOException {
		String sig = entries.get(classEntry);
		if (sig == null) {
			return null;
		}

		ZipEntry entry = zip.getEntry(sig);
		return entry != null ? zip.getInputStream(entry) : null;
	}

	private static Map<String, String> index(ZipFile zip, int release) {
		// ct.sym encodes releases as base-36 characters, e.g. Java 21 is 'L'.
		char key = Character.toUpperCase(Character.forDigit(release, Character.MAX_RADIX));
		ImmutableMap.Builder<String, String> entries = ImmutableMap.builder();
		zip.stream()
			.map(ZipEntry::getName)
			.filter(name -> name.endsWith(".sig") && !name.endsWith("/module-info.sig"))
			.filter(name -> name.indexOf('/') > 0 && name.substring(0, name.indexOf('/')).indexOf(key) >= 0)
			.forEach(name -> entries.put(toClassEntry(name), name));
		return entries.buildOrThrow();
	}

	// LMNOP/java.base/java/util/List.sig -> java/util/List.class
	private static String toClassEntry(String sig) {
		int afterModule = sig.indexOf('/', sig.indexOf('/') + 1) + 1;
		return sig.substring(afterModule, sig.length() - ".sig".length()) + ".class";
	}

	private static void closeQuietly(ZipFile zip) {
		if (zip != null) {
			try {
				zip.close();
			} catch (IOException _) {
				// Preserve the original read failure.
			}
		}
	}
}
