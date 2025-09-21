package io.github.alien.roseau.extractors.asm;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.TypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An ASM-based {@link TypesExtractor}.
 */
public class AsmTypesExtractor implements TypesExtractor {
	private static final int ASM_VERSION = Opcodes.ASM9;
	// TODO: how bad is the performance penalty for no SKIP_CODE for source locations?
	private static final int PARSING_OPTIONS = ClassReader.SKIP_FRAMES;
	private static final Logger LOGGER = LogManager.getLogger(AsmTypesExtractor.class);

	@Override
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(canExtract(library));
		try (JarFile jar = new JarFile(library.getPath().toFile())) {
			return extractTypes(library, jar);
		} catch (IOException e) {
			throw new RoseauException("Error processing JAR file", e);
		}
	}

	@Override
	public boolean canExtract(Library library) {
		return library != null && library.isJar();
	}

	/**
	 * Extracts the {@link LibraryTypes} stored in the provided JAR file.
	 *
	 * @param jar the JAR file to analyze
	 * @return the extracted {@link LibraryTypes}
	 */
	public LibraryTypes extractTypes(Library library, JarFile jar) {
		TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();

		List<TypeDecl> typeDecls =
			Objects.requireNonNull(jar).stream()
				.filter(entry -> entry.getName().endsWith(".class") && !entry.isDirectory())
				// Multi-release JARs store version-specific class files there, so we could have duplicates
				.filter(entry -> !entry.getName().startsWith("META-INF/"))
				.parallel()
				.flatMap(entry -> extractTypeDecl(jar, entry, typeRefFactory).stream())
				.toList();

		return new LibraryTypes(library, typeDecls);
	}

	private static Optional<TypeDecl> extractTypeDecl(JarFile jar, JarEntry entry, TypeReferenceFactory typeRefFactory) {
		try (InputStream is = jar.getInputStream(entry)) {
			ClassReader reader = new ClassReader(is);
			AsmClassVisitor visitor = new AsmClassVisitor(ASM_VERSION, typeRefFactory);
			reader.accept(visitor, PARSING_OPTIONS);
			return Optional.ofNullable(visitor.getTypeDecl());
		} catch (IOException e) {
			LOGGER.error("Error processing JAR entry {}", entry.getName(), e);
			return Optional.empty();
		}
	}
}
