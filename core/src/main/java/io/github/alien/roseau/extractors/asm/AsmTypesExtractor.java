package io.github.alien.roseau.extractors.asm;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.TypesExtractor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * An ASM-based {@link TypesExtractor}.
 */
public class AsmTypesExtractor implements TypesExtractor {
	private final ApiFactory factory;

	private static final int ASM_VERSION = Opcodes.ASM9;
	private static final Pattern ANONYMOUS_MATCHER = Pattern.compile("\\$\\d+");
	private static final Logger LOGGER = LoggerFactory.getLogger(AsmTypesExtractor.class);

	public enum ParsingMode {
		// Can't SKIP_DEBUG cause we need to retain the SourceFile
		WITH_LOCATIONS(ClassReader.SKIP_FRAMES),
		WITHOUT_LOCATIONS(ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

		private final int flags;

		ParsingMode(int flags) {
			this.flags = flags;
		}

		int flags() {
			return flags;
		}
	}

	public AsmTypesExtractor(ApiFactory factory) {
		this.factory = Preconditions.checkNotNull(factory);
	}

	@Override
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(canExtract(library));
		try (JarFile jar = new JarFile(library.getLocation().toFile(), false, ZipFile.OPEN_READ, JarFile.runtimeVersion())) {
			return extractTypes(library, jar);
		} catch (IOException e) {
			throw new RoseauException("Failed to process JAR file", e);
		}
	}

	public boolean canExtract(Library library) {
		return library != null && library.isJar();
	}

	/**
	 * Extracts the {@link LibraryTypes} stored in the provided JAR file.
	 *
	 * @param jar the JAR file to analyze
	 * @return the extracted {@link LibraryTypes}
	 */
	private LibraryTypes extractTypes(Library library, JarFile jar) {
		ExtractorSink sink = new ExtractorSink(jar.size() << 1);
		jar.versionedStream().parallel()
			.filter(AsmTypesExtractor::isRegularClassFile)
			.forEach(entry -> processJarEntry(jar, entry, sink));

		Set<TypeDecl> types = sink.getTypes();
		Set<ModuleDecl> modules = sink.getModules();
		return switch (modules.size()) {
			case 0 -> new LibraryTypes(library, types);
			case 1 -> new LibraryTypes(library, modules.iterator().next(), types);
			default -> throw new RoseauException("%s contains multiple module declarations: %s".formatted(library, modules));
		};
	}

	private void processJarEntry(JarFile jar, JarEntry entry, ExtractorSink sink) {
		try (InputStream is = jar.getInputStream(entry)) {
			processInputStream(is, sink, ParsingMode.WITH_LOCATIONS);
		} catch (IOException e) {
			LOGGER.error("Error processing JAR entry {}", entry.getName(), e);
		}
	}

	public void processInputStream(InputStream is, ExtractorSink sink, ParsingMode parsingMode) {
		try {
			ClassReader reader = new ClassReader(is);
			AsmClassVisitor visitor = new AsmClassVisitor(ASM_VERSION, sink, factory);
			reader.accept(visitor, parsingMode.flags());
		} catch (IOException | RuntimeException e) {
			// Catching wide here as ASM can throw both IOException and different RuntimeException on corrupt entries
			LOGGER.error("Error processing JAR input stream", e);
		}
	}

	private boolean isRegularClassFile(JarEntry entry) {
		return !entry.isDirectory()
			&& entry.getName().endsWith(".class")
			&& !ANONYMOUS_MATCHER.matcher(entry.getName()).find();
	}
}
