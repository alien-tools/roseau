package io.github.alien.roseau;

import com.google.common.base.Preconditions;
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
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.extractors.ExtractorType;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.HashFunction;
import io.github.alien.roseau.extractors.incremental.HashingChangedFilesProvider;
import io.github.alien.roseau.extractors.incremental.IncrementalTypesExtractor;
import io.github.alien.roseau.extractors.jdt.IncrementalJdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Roseau {
	private static final Logger LOGGER = LogManager.getLogger(Roseau.class);

	private Roseau() {

	}

	public static API buildAPI(Library library) {
		Preconditions.checkNotNull(library);
		return toAPI(library, extractTypes(library));
	}

	public static RoseauReport diff(API v1, API v2) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);

		Stopwatch sw = Stopwatch.createStarted();
		RoseauReport report = new APIDiff(v1, v2).diff();
		LOGGER.debug("Diffing APIs took {}ms ({} breaking changes)",
			sw.elapsed().toMillis(), report.breakingChanges().size());

		return report;
	}

	public static RoseauReport parallelDiff(Library v1, Library v2) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);

		Stopwatch sw = Stopwatch.createStarted();
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2));

		API api1 = futureV1.join();
		API api2 = futureV2.join();
		LOGGER.debug("Building APIs in parallel took {}ms ({} vs {} types)",
			sw.elapsed().toMillis(), api1.getExportedTypes().size(), api2.getExportedTypes().size());

		return diff(api1, api2);
	}

	public static RoseauReport incrementalDiff(Library v1, Library v2) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkArgument(v2 != null && v2.getExtractorType() == ExtractorType.JDT,
			"Incremental building is only available with JDT");
		HashingChangedFilesProvider provider = new HashingChangedFilesProvider(HashFunction.XXHASH);

		Stopwatch sw = Stopwatch.createStarted();
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1));
		CompletableFuture<ChangedFiles> futureChanges = CompletableFuture.supplyAsync(
			() -> provider.getChangedFiles(v1.getLocation(), v2.getLocation()));
		CompletableFuture<API> futureV2 = futureV1.thenCombineAsync(futureChanges, (api, changes) -> {
			IncrementalTypesExtractor extractor = new IncrementalJdtTypesExtractor();
			return toAPI(v2, extractor.incrementalUpdate(api.getLibraryTypes(), v2, changes));
		});

		API api1 = futureV1.join();
		API api2 = futureV2.join();
		LOGGER.debug("Building APIs incrementally took {}ms ({} vs {} types)",
			sw.elapsed().toMillis(), api1.getExportedTypes().size(), api2.getExportedTypes().size());

		return diff(api1, api2);
	}

	private static LibraryTypes extractTypes(Library library) {
		TypesExtractor extractor = library.getExtractorType().newExtractor();

		Stopwatch sw = Stopwatch.createStarted();
		LibraryTypes types = extractor.extractTypes(library);
		LOGGER.debug("Extracting types from library {} using {} took {}ms ({} types)",
			library.getLocation(), library.getExtractorType(), sw.elapsed().toMillis(), types.getAllTypes().size());

		return types;
	}

	private static API toAPI(Library library, LibraryTypes types) {
		TypeReferenceFactory factory = new CachingTypeReferenceFactory();
		TypeProvider reflectiveTypeProvider = new SpoonTypeProvider(factory, library.getClasspath());
		TypeResolver cachingTypeResolver = new CachingTypeResolver(List.of(types, reflectiveTypeProvider));

		return new API(types, cachingTypeResolver);
	}
}
