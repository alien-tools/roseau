package io.github.alien.roseau.extractors.spoon;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.extractors.TypesExtractor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Spoon-based {@link TypesExtractor}.
 */
public class SpoonTypesExtractor implements TypesExtractor {
	private final ApiFactory factory;

	public SpoonTypesExtractor(ApiFactory factory) {
		this.factory = Preconditions.checkNotNull(factory);
	}

	@Override
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(canExtract(library));
		CtModel model = SpoonUtils.buildModel(library.getLocation(), library.getClasspath(),
			Duration.ofSeconds(Long.MAX_VALUE));
		return extractTypes(library, model);
	}

	public LibraryTypes extractTypes(Library library, CtModel model) {
		Preconditions.checkArgument(canExtract(library));
		Preconditions.checkNotNull(model);
		SpoonApiFactory spoonFactory = new SpoonApiFactory(library, factory);

		Set<TypeDecl> allTypes = model.getAllPackages().stream().parallel()
			.flatMap(p -> getAllTypes(p).parallel().map(spoonFactory::convertCtType))
			.collect(Collectors.toSet());

		Set<CtModule> modules = model.getAllModules().stream()
			.filter(mod -> !mod.isUnnamedModule())
			.collect(Collectors.toSet());

		return switch (modules.size()) {
			case 0 -> new LibraryTypes(library, allTypes);
			case 1 -> new LibraryTypes(library, spoonFactory.convertCtModule(modules.iterator().next()), allTypes);
			default -> throw new RoseauException("%s contains multiple module declarations: %s".formatted(library, modules));
		};
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

	private static boolean canExtract(Library library) {
		return library != null && library.isSources();
	}
}
