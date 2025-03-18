package io.github.alien.roseau.extractors;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;

import java.nio.file.Path;

/**
 * API extractors must keep track of inaccessible types in the resulting API because of API leaks; e.g.:
 *
 * <pre>
 * package pkg1;
 * class A {
 *  public int f;
 *  // Leaked method
 *  public void m() { System.out.println("A.m()"); }
 * }
 * public class B extends A {} // Leaking A's public members
 * package pkg2;
 * class D extends B {
 *  void leak() { m(); } // Leak, can break
 * }
 * class E extends B {
 *  public void m() {
 *    super.f = 0; // Leak, can break
 *    super.m();   // Leak, can break
 *  }
 * }
 * </pre>
 */
public interface APIExtractor {
	/**
	 * Extracts a new {@link API} from the data located at {@code sources}
	 *
	 * @throws RoseauException if anything went wrong
	 */
	API extractAPI(Path sources);

	/**
	 * Checks whether this extractor can handle the given {@code sources}
	 *
	 * @param sources The file or directory to check
	 * @return true if this extractor handles the given {@code sources}
	 */
	boolean canExtract(Path sources);
}
