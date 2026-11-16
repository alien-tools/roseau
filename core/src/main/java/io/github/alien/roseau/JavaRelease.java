package io.github.alien.roseau;

import io.github.alien.roseau.api.resolution.CtSymPlatform;

/**
 * Java feature releases (8, 11, 17, 21, ...) on which Roseau can check compatibility.
 */
public final class JavaRelease {
	/**
	 * The oldest supported release, same as {@code javac --release}
	 */
	public static final int MINIMUM = 8;

	private JavaRelease() {

	}

	/**
	 * @return the feature release of the running JDK
	 */
	public static int host() {
		return Runtime.version().feature();
	}

	/**
	 * @param release the Java feature release
	 * @return the version selecting the entries of multi-release JARs that this release loads
	 */
	public static Runtime.Version version(int release) {
		return Runtime.Version.parse(Integer.toString(release));
	}

	/**
	 * Checks that the JDK running Roseau can check compatibility on the given release.
	 *
	 * @param release the Java feature release
	 * @throws RoseauException if the release is not supported
	 */
	public static void checkSupported(int release) {
		int host = host();
		if (release < MINIMUM || release > host) {
			throw new RoseauException("Unsupported Java release %d; Roseau running on Java %d supports releases %d to %d"
				.formatted(release, host, MINIMUM, host));
		}
		if (release != host && !CtSymPlatform.isAvailable(release)) {
			throw new RoseauException(("Cannot check compatibility on Java %d: %s does not provide it; " +
				"run Roseau on a full JDK").formatted(release, CtSymPlatform.hostCtSym()));
		}
	}
}
