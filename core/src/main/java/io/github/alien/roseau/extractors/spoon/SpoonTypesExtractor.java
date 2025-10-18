package io.github.alien.roseau.extractors.spoon;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.TypesExtractor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Spoon-based {@link TypesExtractor}.
 */
public class SpoonTypesExtractor implements TypesExtractor {
	@Override
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(canExtract(library));
		CtModel model = SpoonUtils.buildModel(library.getLocation(), library.getClasspath(),
			Duration.ofSeconds(Long.MAX_VALUE));
		return extractTypes(library, model);
	}

	@Override
	public boolean canExtract(Library library) {
		return library != null && library.isSources();
	}

	public LibraryTypes extractTypes(Library library, CtModel model) {
		Preconditions.checkArgument(canExtract(library));
		Preconditions.checkNotNull(model);
		TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();
		SpoonAPIFactory factory = new SpoonAPIFactory(library, typeRefFactory);

		List<TypeDecl> allTypes = model.getAllPackages().stream().parallel()
			.flatMap(p -> getAllTypes(p).parallel().map(factory::convertCtType))
			.toList();

		List<CtModule> modules = model.getAllModules().stream()
			.filter(mod -> !mod.isUnnamedModule())
			.toList();

		if (modules.isEmpty()) {
			return new LibraryTypes(library, allTypes);
		} else if (modules.size() == 1) {
			return new LibraryTypes(library, factory.convertCtModule(modules.getFirst()), allTypes);
		} else {
			throw new RoseauException("%s contains multiple module declarations: %s".formatted(library, modules));
		}
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
