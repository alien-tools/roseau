package io.github.alien.roseau.combinatorial.writer;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.api.ApiPrettyPrinter;
import io.github.alien.roseau.combinatorial.Constants;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ApiWriter extends AbstractWriter {
	public ApiWriter(Path outputDir) {
		super(outputDir.resolve(Constants.API_FOLDER));
	}

	public void write(API api) {
		var prettyPrinter = new ApiPrettyPrinter(api);

		api.getLibraryTypes().getAllTypes().forEach(t -> {
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
