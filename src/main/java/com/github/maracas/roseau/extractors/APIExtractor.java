package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.API;

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
	API extractAPI(Path sources);
}
