package io.github.alien.roseau.extractors.asm;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.ModuleDecl;
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
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An ASM-based {@link TypesExtractor}.
 */
public class AsmTypesExtractor implements TypesExtractor {
	private static final int ASM_VERSION = Opcodes.ASM9;
	private static final int PARSING_OPTIONS = ClassReader.SKIP_FRAMES;
	private static final Logger LOGGER = LogManager.getLogger(AsmTypesExtractor.class);

	@Override
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(canExtract(library));
		try (JarFile jar = new JarFile(library.getLocation().toFile())) {
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
	private LibraryTypes extractTypes(Library library, JarFile jar) {
		TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();

		List<TypeDecl> typeDecls = jar.stream()
			.filter(this::isRegularClassFile)
			.parallel()
			.flatMap(entry -> extractTypeDecl(jar, entry, typeRefFactory).stream())
			.toList();
		List<ModuleDecl> moduleDecls = jar.stream()
			.filter(this::isModuleInfo)
			.flatMap(entry -> extractModuleDecl(jar, entry).stream())
			.toList();

		if (moduleDecls.isEmpty()) {
			return new LibraryTypes(library, typeDecls);
		} else if (moduleDecls.size() == 1) {
			return new LibraryTypes(library, moduleDecls.getFirst(), typeDecls);
		} else {
			throw new IllegalStateException("%s contains multiple module declarations: %s".formatted(library, moduleDecls));
		}
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

	private static Optional<ModuleDecl> extractModuleDecl(JarFile jar, JarEntry entry) {
		try (InputStream is = jar.getInputStream(entry)) {
			ClassReader reader = new ClassReader(is);
			AsmModuleVisitor visitor = new AsmModuleVisitor(ASM_VERSION);
			reader.accept(visitor, PARSING_OPTIONS);
			return Optional.ofNullable(visitor.getModuleDecl());
		} catch (IOException e) {
			LOGGER.error("Error processing JAR entry {}", entry.getName(), e);
			return Optional.empty();
		}
	}

	private boolean isModuleInfo(JarEntry entry) {
		return !entry.isDirectory() &&
			entry.getName().equals("module-info.class");
	}

	private boolean isRegularClassFile(JarEntry entry) {
		return !entry.isDirectory() &&
			entry.getName().endsWith(".class") &&
			// Multi-release JARs store version-specific class files there, so we could have duplicates
			!entry.getName().startsWith("META-INF/") &&
			!isModuleInfo(entry);
	}
}
