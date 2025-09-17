package io.github.alien.roseau.extractors.spoon;

import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.TypesExtractor;
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
 * A Spoon-based {@link TypesExtractor}.
 */
public class SpoonTypesExtractor implements TypesExtractor {
	@Override
	public LibraryTypes extractTypes(Path sources, List<Path> classpath) {
		CtModel model = SpoonUtils.buildModel(sources, classpath, Duration.ofSeconds(Long.MAX_VALUE));
		return extractTypes(model, classpath);
	}

	@Override
	public boolean canExtract(Path sources) {
		return Files.isDirectory(sources);
	}

	public LibraryTypes extractTypes(CtModel model, List<Path> classpath) {
		TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();
		SpoonAPIFactory factory = new SpoonAPIFactory(typeRefFactory, classpath);

		List<TypeDecl> allTypes = model.getAllPackages().stream().parallel()
			.flatMap(p -> getAllTypes(p).parallel().map(factory::convertCtType))
			.toList();

		return new LibraryTypes(allTypes);
	}

	public LibraryTypes extractTypes(CtModel model) {
		return extractTypes(model, List.of());
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
