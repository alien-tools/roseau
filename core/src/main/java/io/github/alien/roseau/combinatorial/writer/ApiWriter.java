package io.github.alien.roseau.combinatorial.writer;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.visit.APIPrettyPrinter;

import java.nio.file.Files;
import java.nio.file.Path;

public class ApiWriter extends AbstractWriter {
	public ApiWriter(Path outputDir) {
		super(outputDir);
	}

	public void write(API api) {
		var prettyPrinter = new APIPrettyPrinter();

		api.getAllTypes().forEach(t -> {
			try {
				var code = prettyPrinter.$(t).print();

				var filePath = outputDir.resolve(t.getQualifiedName().replace('.', '/') + ".java");
				var file = filePath.toFile();
				file.getParentFile().mkdirs();

				Files.writeString(filePath, code);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
