package io.github.alien.roseau;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.resolution.CachingTypeResolver;
import io.github.alien.roseau.api.resolution.ClasspathTypeProvider;
import io.github.alien.roseau.api.resolution.TypeProvider;
import io.github.alien.roseau.api.resolution.TypeResolver;
import io.github.alien.roseau.diff.ApiDiffer;
import io.github.alien.roseau.diff.ApiWalker;
import io.github.alien.roseau.diff.BreakingChangeAnalyzer;
import io.github.alien.roseau.diff.DefaultSymbolMatcher;
import io.github.alien.roseau.diff.RoseauReport;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Entry point for extracting library snapshots, building resolved APIs, and computing diffs.
 */
public final class Roseau {
	private static final Logger LOGGER = LogManager.getLogger(Roseau.class);

	private Roseau() {

	}

	/**
	 * Extracts the immutable snapshot of a library.
	 *
	 * @param library the library to analyze
	 * @return the extracted library types
	 */
	public static LibraryTypes buildLibraryTypes(Library library) {
		Preconditions.checkNotNull(library);
		return extractTypes(library, defaultApiFactory());
	}

	/**
	 * Builds a resolved {@link API} from the given {@link Library}.
	 *
	 * @param library the library to analyze
	 * @return the built API model
	 */
	public static API buildAPI(Library library) {
		Preconditions.checkNotNull(library);
		return buildAPI(buildLibraryTypes(library));
	}

	/**
	 * Builds a resolved {@link API} from the given extracted library types using the default resolver.
	 *
	 * @param types the extracted library types
	 * @return the built API model
	 */
	public static API buildAPI(LibraryTypes types) {
		Preconditions.checkNotNull(types);
		return buildAPI(types, defaultApiFactory());
	}

	/**
	 * Builds a resolved {@link API} from the given extracted library types and resolver.
	 *
	 * @param types    the extracted library types
	 * @param resolver the resolver used for type resolution
	 * @return the built API model
	 */
	public static API buildAPI(LibraryTypes types, TypeResolver resolver) {
		Preconditions.checkNotNull(types);
		Preconditions.checkNotNull(resolver);
		return new API(types, resolver);
	}

	/**
	 * Computes a diff between two API versions.
	 *
	 * @param v1 the baseline API
	 * @param v2 the target API
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport diff(API v1, API v2) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);

		Stopwatch sw = Stopwatch.createStarted();
		ApiWalker walker = new ApiWalker(v1, v2, new DefaultSymbolMatcher());
		ApiDiffer<RoseauReport> differ = new BreakingChangeAnalyzer(v1, v2);
		RoseauReport report = walker.walk(differ);
		LOGGER.debug("Diffing APIs took {}ms ({} breaking changes)",
			() -> sw.elapsed().toMillis(), () -> report.breakingChanges().size());

		return report;
	}

	/**
	 * Computes a diff between two libraries and applies the supplied {@link DiffPolicy}.
	 *
	 * @param request the diff request
	 * @return a filtered {@link RoseauReport}
	 */
	public static RoseauReport diff(DiffRequest request) {
		Preconditions.checkNotNull(request);
		return diff(request.v1(), request.v2()).filter(request.policy());
	}

	/**
	 * Builds both APIs in parallel using the provided {@link Executor} and computes their diff.
	 *
	 * @param v1       the baseline library
	 * @param v2       the target library
	 * @param executor the executor to use
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport diff(Library v1, Library v2, Executor executor) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		Preconditions.checkNotNull(executor);

		Stopwatch sw = Stopwatch.createStarted();
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1), executor);
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2), executor);

		try {
			API api1 = futureV1.join();
			API api2 = futureV2.join();
			LOGGER.debug("Building APIs in parallel took {}ms ({} vs {} types)",
				() -> sw.elapsed().toMillis(), () -> api1.getExportedTypes().size(), () -> api2.getExportedTypes().size());
			return diff(api1, api2);
		} catch (CompletionException e) {
			throw new RoseauException("Failed to build diff", e.getCause() != null ? e.getCause() : e);
		}
	}

	/**
	 * Builds both APIs in parallel using the provided {@link Executor}, computes their diff, and applies the supplied
	 * {@link DiffPolicy}.
	 *
	 * @param v1       the baseline library
	 * @param v2       the target library
	 * @param policy   the diff policy to apply
	 * @param executor the executor to use
	 * @return a filtered {@link RoseauReport}
	 */
	public static RoseauReport diff(Library v1, Library v2, DiffPolicy policy, Executor executor) {
		Preconditions.checkNotNull(policy);
		return diff(v1, v2, executor).filter(policy);
	}

	/**
	 * Builds both APIs in parallel using the default {@link ForkJoinPool#commonPool()}, computes their diff, and
	 * applies the default empty {@link DiffPolicy}.
	 *
	 * @param v1 the baseline library
	 * @param v2 the target library
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport diff(Library v1, Library v2) {
		return diff(v1, v2, DiffPolicy.empty(), ForkJoinPool.commonPool());
	}

	/**
	 * Builds both APIs in parallel using the default {@link ForkJoinPool#commonPool()}, computes their diff, and
	 * applies the supplied {@link DiffPolicy}.
	 *
	 * @param v1     the baseline library
	 * @param v2     the target library
	 * @param policy the diff policy to apply
	 * @return a filtered {@link RoseauReport}
	 */
	public static RoseauReport diff(Library v1, Library v2, DiffPolicy policy) {
		return diff(v1, v2, policy, ForkJoinPool.commonPool());
	}

	/**
	 * Incrementally updates a previously extracted source snapshot.
	 *
	 * @param previousTypes the previously extracted source snapshot
	 * @param newVersion    the new source library version
	 * @param changedFiles  the changed source files, relative to the library root
	 * @return the updated library types
	 */
	public static LibraryTypes incrementalBuild(LibraryTypes previousTypes, Library newVersion,
	                                            ChangedFiles changedFiles) {
		Preconditions.checkNotNull(previousTypes);
		Preconditions.checkNotNull(newVersion);
		Preconditions.checkNotNull(changedFiles);
		Preconditions.checkArgument(previousTypes.getLibrary().isSources());
		Preconditions.checkArgument(newVersion.isSources());

		ApiFactory factory = defaultApiFactory();
		IncrementalTypesExtractor incremental = new IncrementalJdtTypesExtractor(new JdtTypesExtractor(factory));
		return incremental.incrementalUpdate(previousTypes, newVersion, changedFiles);
	}

	/**
	 * Incrementally computes the diff between two source libraries using the provided {@link Executor}.
	 *
	 * @param v1       the baseline source library
	 * @param v2       the target source library
	 * @param executor the executor to use
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport incrementalDiff(Library v1, Library v2, Executor executor) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		Preconditions.checkNotNull(executor);
		Preconditions.checkArgument(v1.isSources());
		Preconditions.checkArgument(v2.isSources());
		HashingChangedFilesProvider provider = new HashingChangedFilesProvider(HashFunction.XXHASH);

		Stopwatch sw = Stopwatch.createStarted();
		CompletableFuture<LibraryTypes> futureV1 = CompletableFuture.supplyAsync(() -> buildLibraryTypes(v1), executor);
		CompletableFuture<ChangedFiles> futureChanges = CompletableFuture.supplyAsync(
			() -> provider.getChangedFiles(v1.location(), v2.location()), executor);
		CompletableFuture<LibraryTypes> futureV2 = futureV1.thenCombineAsync(
			futureChanges,
			(types, changes) -> incrementalBuild(types, v2, changes),
			executor);

		try {
			LibraryTypes types1 = futureV1.join();
			LibraryTypes types2 = futureV2.join();
			API api1 = buildAPI(types1);
			API api2 = buildAPI(types2);
			LOGGER.debug("Building APIs incrementally took {}ms ({} vs {} types)",
				() -> sw.elapsed().toMillis(), () -> api1.getExportedTypes().size(), () -> api2.getExportedTypes().size());
			return diff(api1, api2);
		} catch (CompletionException e) {
			throw new RoseauException("Failed to incrementally update APIs", e.getCause() != null ? e.getCause() : e);
		}
	}

	/**
	 * Incrementally computes the diff between two source libraries.
	 *
	 * @param v1 the baseline source library
	 * @param v2 the target source library
	 * @return a {@link RoseauReport} containing the list of breaking changes
	 */
	public static RoseauReport incrementalDiff(Library v1, Library v2) {
		return incrementalDiff(v1, v2, ForkJoinPool.commonPool());
	}

	private static LibraryTypes extractTypes(Library library, ApiFactory factory) {
		TypesExtractor extractor = library.isSources()
			? new JdtTypesExtractor(factory)
			: new AsmTypesExtractor(factory);

		Stopwatch sw = Stopwatch.createStarted();
		LibraryTypes types = extractor.extractTypes(library);
		LOGGER.debug("Extracting types from library {} using {} took {}ms ({} types)",
			library::location, () -> library.isSources() ? "JDT" : "ASM", () -> sw.elapsed().toMillis(),
			() -> types.getAllTypes().size());

		return types;
	}

	private static API buildAPI(LibraryTypes types, ApiFactory factory) {
		AsmTypesExtractor extractor = new AsmTypesExtractor(factory);
		TypeProvider classpathProvider = new ClasspathTypeProvider(extractor, types.getLibrary().classpath());
		TypeResolver cachingTypeResolver = new CachingTypeResolver(List.of(types, classpathProvider));
		return buildAPI(types, cachingTypeResolver);
	}

	private static ApiFactory defaultApiFactory() {
		return new DefaultApiFactory(new CachingTypeReferenceFactory());
	}
}
