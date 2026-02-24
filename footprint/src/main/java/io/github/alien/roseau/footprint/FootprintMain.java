package io.github.alien.roseau.footprint;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Entrypoint for generating a footprint source file from a source tree.
 */
public final class FootprintMain {
	private FootprintMain() {

	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1 || args.length > 4) {
			System.err.println("Usage: FootprintMain <source-tree> [output-file|output-dir] [package] [pom.xml]");
			System.err.println("Defaults: output=./Footprint.java, package=" + FootprintService.DEFAULT_PACKAGE
				+ ", pom=<none>");
			return;
		}

		Path sourceTree = Path.of(args[0]);
		Path output = args.length >= 2
			? Path.of(args[1])
			: Path.of(FootprintService.DEFAULT_CLASS_NAME + ".java");
		String packageName = args.length >= 3 ? args[2] : FootprintService.DEFAULT_PACKAGE;
		Path pomFile = args.length >= 4 ? Path.of(args[3]) : null;

		FootprintService service = new FootprintService();
		Path generated = service.generateToFile(
			sourceTree,
			output,
			pomFile,
			packageName,
			FootprintService.DEFAULT_CLASS_NAME
		);
		System.out.println("Generated footprint: " + generated);
	}
}
