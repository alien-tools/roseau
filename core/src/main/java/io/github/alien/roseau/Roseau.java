package io.github.alien.roseau;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.resolution.CachingTypeResolver;
import io.github.alien.roseau.api.resolution.SpoonTypeProvider;
import io.github.alien.roseau.api.resolution.TypeProvider;
import io.github.alien.roseau.api.resolution.TypeResolver;
import io.github.alien.roseau.diff.ApiDiff;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.extractors.ExtractorType;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.HashFunction;
import io.github.alien.roseau.extractors.incremental.HashingChangedFilesProvider;
import io.github.alien.roseau.extractors.incremental.IncrementalTypesExtractor;
import io.github.alien.roseau.extractors.jdt.IncrementalJdtTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Entry point utilities for building APIs and computing breaking changes between versions.
 */
public final class Roseau {
	private static final Logger LOGGER = LogManager.getLogger(Roseau.class);

	private Roseau() {

	}

	/**
	 * Builds an {@link API} model from the given {@link Library}.
	 *
	 * @param library the library to analyze (must not be null)
	 * @return the built API model
	 */
	public static API buildAPI(Library library) {
		Preconditions.checkNotNull(library);
		ApiFactory factory = defaultApiFactory();
		return toAPI(library, extractTypes(library, factory), factory);
	}

	/**
	 * Computes a diff between two API versions.
	 *
	 * @param v1 the baseline API (must not be null)
	 * @param v2 the target API to compare against (must not be null)
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport diff(API v1, API v2) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);

		Stopwatch sw = Stopwatch.createStarted();
		RoseauReport report = new ApiDiff(v1, v2).diff();
		LOGGER.debug("Diffing APIs took {}ms ({} breaking changes)",
			() -> sw.elapsed().toMillis(), () -> report.getBreakingChanges().size());

		return report;
	}

	/**
	 * Builds both APIs in parallel using the provided {@link Executor} and computes their diff.
	 *
	 * @param v1       the baseline library (must not be null)
	 * @param v2       the target library (must not be null)
	 * @param executor the executor to use
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport diff(Library v1, Library v2, Executor executor) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);

		Stopwatch sw = Stopwatch.createStarted();
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1), executor);
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2), executor);

		try {
			API api1 = futureV1.join();
			API api2 = futureV2.join();
			LOGGER.debug("Building APIs in parallel took {}ms ({} vs {} types)",
				() -> sw.elapsed().toMillis(), () -> api1.getExportedTypes().size(), () -> api2.getExportedTypes().size());
			return diff(api1, api2);
		} catch (RuntimeException e) {
			throw new RoseauException("Failed to build diff", e);
		}
	}

	/**
	 * Builds both APIs in parallel using the default {@link ForkJoinPool#commonPool()} and computes their diff.
	 *
	 * @param v1 the baseline library (must not be null)
	 * @param v2 the target library (must not be null)
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport diff(Library v1, Library v2) {
		return diff(v1, v2, ForkJoinPool.commonPool());
	}

	/**
	 * Performs an incremental build of the target API when possible and computes the diff. The baseline API is fully
	 * built. The target API is incrementally built from the baseline based on changed files.
	 *
	 * @param v1 the baseline library (must not be null)
	 * @param v2 the target library (must not be null and use {@link ExtractorType#JDT})
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 * @throws IllegalArgumentException if {@code v2} is not using the JDT extractor
	 */
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
			ApiFactory factory = defaultApiFactory();
			JdtTypesExtractor jdtExtractor = new JdtTypesExtractor(factory);
			IncrementalTypesExtractor incremental = new IncrementalJdtTypesExtractor(jdtExtractor);
			return toAPI(v2, incremental.incrementalUpdate(api.getLibraryTypes(), v2, changes), factory);
		});

		try {
			API api1 = futureV1.join();
			API api2 = futureV2.join();
			LOGGER.debug("Building APIs incrementally took {}ms ({} vs {} types)",
				() -> sw.elapsed().toMillis(), () -> api1.getExportedTypes().size(), () -> api2.getExportedTypes().size());
			return diff(api1, api2);
		} catch (RuntimeException e) {
			throw new RoseauException("Failed to incrementally update APIs", e);
		}
	}

	private static LibraryTypes extractTypes(Library library, ApiFactory factory) {
		TypesExtractor extractor = library.getExtractorType().newExtractor(factory);

		Stopwatch sw = Stopwatch.createStarted();
		LibraryTypes types = extractor.extractTypes(library);
		LOGGER.debug("Extracting types from library {} using {} took {}ms ({} types)",
			library::getLocation, library::getExtractorType, () -> sw.elapsed().toMillis(),
			() -> types.getAllTypes().size());

		return types;
	}

	private static API toAPI(Library library, LibraryTypes types, ApiFactory factory) {
		TypeProvider typeProvider = new SpoonTypeProvider(factory, library.getClasspath());
		TypeResolver cachingTypeResolver = new CachingTypeResolver(List.of(types, typeProvider));

		return new API(types, cachingTypeResolver);
	}

	private static ApiFactory defaultApiFactory() {
		return new DefaultApiFactory(new CachingTypeReferenceFactory());
	}
}
