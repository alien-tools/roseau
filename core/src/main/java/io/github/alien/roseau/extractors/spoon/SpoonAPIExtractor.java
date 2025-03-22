package io.github.alien.roseau.extractors.spoon;

import io.github.alien.roseau.api.model.reference.CachedTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.APIExtractor;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Spoon-based {@link APIExtractor}.
 */
public class SpoonAPIExtractor implements APIExtractor {
	@Override
	public API extractAPI(Path sources, List<Path> classpath) {
		CtModel model = SpoonUtils.buildModel(sources, classpath, Duration.ofSeconds(Long.MAX_VALUE));
		return extractAPI(model);
	}

	@Override
	public boolean canExtract(Path sources) {
		return Files.isDirectory(sources);
	}

	public API extractAPI(CtModel model) {
		TypeReferenceFactory typeRefFactory = new CachedTypeReferenceFactory();
		SpoonAPIFactory factory = new SpoonAPIFactory(typeRefFactory);

		List<TypeDecl> allTypes = model.getAllPackages().stream().parallel()
			.flatMap(p -> getAllTypes(p).parallel().map(factory::convertCtType))
			.toList();

		return new API(allTypes, typeRefFactory);
	}

	@Override
	public String getName() {
		return "Spoon";
	}

	// Returns all types within a package
	private static Stream<CtType<?>> getAllTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.flatMap(type -> Stream.concat(Stream.of(type), getNestedTypes(type)));
	}

	// Returns (recursively) nested types within a type
	private static Stream<CtType<?>> getNestedTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.flatMap(nestedType -> Stream.concat(Stream.of(nestedType), getNestedTypes(nestedType)));
	}
}
