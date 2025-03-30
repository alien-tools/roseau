package io.github.alien.roseau;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.api.resolution.CachingTypeResolver;
import io.github.alien.roseau.api.resolution.SpoonTypeProvider;
import io.github.alien.roseau.api.resolution.TypeProvider;
import io.github.alien.roseau.api.resolution.TypeResolver;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.HashFunction;
import io.github.alien.roseau.extractors.incremental.HashingChangedFilesProvider;
import io.github.alien.roseau.extractors.incremental.IncrementalTypesExtractor;
import io.github.alien.roseau.extractors.jdt.IncrementalJdtTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Roseau {
	private static final Logger LOGGER = LogManager.getLogger(Roseau.class);

	public static void main(String[] args) {
		Library v1 = Library.builder()
			.path(Path.of("/home/dig/repositories/api-evolution-data-corpus/lib-v1"))
			.build();
		Library v2 = Library.builder()
			.path(Path.of("/home/dig/repositories/api-evolution-data-corpus/lib-v2"))
			.build();

		API apiV1 = Roseau.buildAPI(v1);
		API apiV2 = Roseau.buildAPI(v2);

		Stopwatch sw = Stopwatch.createStarted();
		List<BreakingChange> bcs1 = Roseau.diff(apiV1, apiV2);
		long simpleTime = sw.elapsed().toMillis();
		sw.reset().start();
		List<BreakingChange> bcs2 = Roseau.diff(v1, v2);
		long parallelTime = sw.elapsed().toMillis();

		System.out.printf("Simple: %d BCs, %dms%n", bcs1.size(), simpleTime);
		System.out.printf("Parallel: %d BCs, %dms%n", bcs2.size(), parallelTime);
	}

	public static API buildAPI(Library library) {
		return toAPI(library, extractTypes(library));
	}

	public static LibraryTypes extractTypes(Library library) {
		TypesExtractor extractor = newExtractor(library);

		Stopwatch sw = Stopwatch.createStarted();
		LibraryTypes types = extractor.extractTypes(library);
		LOGGER.debug("Extracting types from library {} using {} took {}ms ({} types)",
			library.getPath(), extractor.getName(), sw.elapsed().toMillis(), types.getAllTypes().size());

		return types;
	}

	public static API toAPI(Library library, LibraryTypes types) {
		TypeReferenceFactory factory = new CachingTypeReferenceFactory();
		TypeProvider reflectiveTypeProvider = new SpoonTypeProvider(factory, library.getClasspath());
		TypeResolver cachingTypeResolver = new CachingTypeResolver(List.of(types, reflectiveTypeProvider));

		return new API(types, cachingTypeResolver);
	}

	public static List<BreakingChange> diff(API v1, API v2) {
		Stopwatch sw = Stopwatch.createStarted();
		List<BreakingChange> bcs = new APIDiff(v1, v2).diff();
		LOGGER.debug("Comparing APIs took {}ms ({} breaking changes)", sw.elapsed().toMillis(), bcs.size());
		return bcs;
	}

	// FIXME: only works with JDT
	// Current issue is that updatedFiles point to the new version, not the old, so they're not discarded in incremental
	public static List<BreakingChange> diff(Library v1, Library v2) {
		HashingChangedFilesProvider provider = new HashingChangedFilesProvider(HashFunction.XXHASH);

		// Build the first API asynchronously
		CompletableFuture<LibraryTypes> futureV1 = CompletableFuture.supplyAsync(() -> extractTypes(v1));

		// In parallel, compute the changed files between the two library versions
		CompletableFuture<ChangedFiles> futureChanges = CompletableFuture.supplyAsync(
			() -> provider.getChangedFiles(v1.getPath(), v2.getPath()));

		// Once both the API v1 and the changed files are ready, build API v2
		CompletableFuture<LibraryTypes> futureV2 = futureV1.thenCombineAsync(futureChanges, (types, changes) -> {
			IncrementalTypesExtractor extractor = new IncrementalJdtTypesExtractor();
			return extractor.incrementalUpdate(types, v2, changes);
		});

		// Block until both APIs are ready and then compute the diff.
		API api1 = toAPI(v1, futureV1.join());
		API api2 = toAPI(v2, futureV2.join());
		return new APIDiff(api1, api2).diff();
	}

	private static TypesExtractor newExtractor(Library library) {
		if (library.isJar()) {
			return new AsmTypesExtractor();
		}

		if (library.isSources()) {
			return new JdtTypesExtractor();
		}

		throw new IllegalStateException("Unknown library type: " + library);
	}
}
