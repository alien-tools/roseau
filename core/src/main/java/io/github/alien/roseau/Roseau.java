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
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;

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
		List<BreakingChange> bcs = Roseau.diff(apiV1, apiV2);

		System.out.println(bcs);
	}

	public static API buildAPI(Library library) {
		TypesExtractor extractor = newExtractor(library);

		Stopwatch sw = Stopwatch.createStarted();
		LibraryTypes types = extractor.extractTypes(library.getPath(), library.getClasspath());
		LOGGER.debug("Extracting types from library {} using {} took {}ms ({} types)",
			library.getPath(), extractor.getName(), sw.elapsed().toMillis(), types.getAllTypes().size());

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

	private static TypesExtractor newExtractor(Library library) {
		if (library.getExtractor() != null) {
			return library.getExtractor();
		}

		if (library.isJar()) {
			return new AsmTypesExtractor();
		}

		if (library.isSources()) {
			return new JdtTypesExtractor();
		}

		throw new IllegalStateException("Unknown library type: " + library);
	}
}
