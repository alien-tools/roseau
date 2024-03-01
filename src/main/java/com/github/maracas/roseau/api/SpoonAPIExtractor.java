package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.TypeDecl;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.TypeFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * Extracts an {@link API} model from source code using Spoon as back-end.
 */
public class SpoonAPIExtractor implements APIExtractor {
	/**
	 * Extracts the {@link API} model for the source code located at {@code sources}.
	 *
	 * @param  sources {@link Path} to the source code to be parsed into an {@link API}
	 * @return the extracted {@link API}
	 */
	@Override
	public API extractAPI(Path sources) {
		CtModel model = SpoonUtils.buildModel(sources, Duration.ofSeconds(Long.MAX_VALUE));
		return extractAPI(model);
	}

	public API extractAPI(CtModel model) {
		TypeFactory typeFactory = model.getRootPackage().getFactory().Type();
		SpoonAPIFactory factory = new SpoonAPIFactory(typeFactory);

		List<TypeDecl> allTypes =
			model.getAllPackages().stream()
				.parallel()
				.flatMap(p -> getAllTypes(p).stream()
					.parallel()
					.map(factory::convertCtType))
				.toList();

		return new API(allTypes, factory);
	}

	// Returns all types within a package
	private List<CtType<?>> getAllTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.flatMap(type -> Stream.concat(
				Stream.of(type),
				getNestedTypes(type).stream()
			))
			.toList();
	}

	// Returns (recursively) nested types within a type
	private List<CtType<?>> getNestedTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.flatMap(nestedType -> Stream.concat(
				Stream.of(nestedType),
				getNestedTypes(nestedType).stream()
			))
			.toList();
	}
}
