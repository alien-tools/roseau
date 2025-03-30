package io.github.alien.roseau.extractors.spoon;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.TypesExtractor;
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
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(library != null && library.isSources());
		CtModel model = SpoonUtils.buildModel(library.getPath(), library.getClasspath(), Duration.ofSeconds(Long.MAX_VALUE));
		return extractTypes(library, model);
	}

	@Override
	public boolean canExtract(Path sources) {
		return Files.isDirectory(sources);
	}

	public LibraryTypes extractTypes(Library library, CtModel model) {
		Preconditions.checkArgument(library != null && library.isSources());
		Preconditions.checkNotNull(model);
		TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();
		SpoonAPIFactory factory = new SpoonAPIFactory(typeRefFactory, library.getClasspath());

		List<TypeDecl> allTypes = model.getAllPackages().stream().parallel()
			.flatMap(p -> getAllTypes(p).parallel().map(factory::convertCtType))
			.toList();

		return new LibraryTypes(library, allTypes);
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
