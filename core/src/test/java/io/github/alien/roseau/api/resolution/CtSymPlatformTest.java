package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CtSymPlatformTest {
	private static final Path HOST_CT_SYM = Path.of(System.getProperty("java.home"), "lib", "ct.sym");
	private final AsmTypesExtractor extractor =
		new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));

	@TempDir
	Path tempDir;

	@Test
	void reads_class_files_including_nested_types() throws IOException {
		var platform = CtSymPlatform.forRelease(Runtime.version().feature()).orElseThrow();

		try (InputStream list = platform.openClass("java/util/List.class");
		     InputStream entry = platform.openClass("java/util/Map$Entry.class")) {
			assertThat(list).isNotNull();
			assertThat(entry).isNotNull();
		}
	}

	@Test
	void unknown_class_is_null() throws IOException {
		var platform = CtSymPlatform.forRelease(Runtime.version().feature()).orElseThrow();

		assertThat(platform.openClass("java/lang/Unknown.class")).isNull();
		assertThat(platform.openClass("module-info.class")).isNull();
	}

	@Test
	void jdk_internal_types_are_not_part_of_the_platform() throws IOException {
		var platform = CtSymPlatform.forRelease(Runtime.version().feature()).orElseThrow();

		assertThat(platform.openClass("jdk/internal/misc/Unsafe.class")).isNull();
		try (InputStream unsupported = platform.openClass("sun/misc/Unsafe.class")) {
			assertThat(unsupported).isNotNull();
		}
	}

	@Test
	void selects_the_declarations_of_the_requested_release() {
		var java8 = provider(8);
		var java21 = provider(21);

		assertThat(java8.findType("java.util.SequencedCollection")).isEmpty();
		assertThat(methodNames(java8.findType("java.util.List", InterfaceDecl.class).orElseThrow().getDeclaredMethods()))
			.doesNotContain("reversed", "getFirst");
		assertThat(java21.findType("java.util.SequencedCollection")).isPresent();
		assertThat(methodNames(java21.findType("java.util.List", InterfaceDecl.class).orElseThrow().getDeclaredMethods()))
			.contains("reversed", "getFirst");
	}

	@Test
	void java_8_platform_has_modules_removed_since() {
		var java8 = provider(8);
		var java11 = provider(11);

		// java.xml.bind was removed in Java 11
		assertThat(java8.findType("javax.xml.bind.JAXBContext", ClassDecl.class)).isPresent();
		assertThat(java11.findType("javax.xml.bind.JAXBContext")).isEmpty();
	}

	@Test
	void release_not_in_ct_sym_is_empty() {
		assertThat(CtSymPlatform.open(HOST_CT_SYM, 7)).isEmpty();
		assertThat(CtSymPlatform.open(HOST_CT_SYM, Runtime.version().feature() + 1)).isEmpty();
	}

	@Test
	void missing_ct_sym_is_empty() {
		assertThat(CtSymPlatform.open(tempDir.resolve("ct.sym"), 21)).isEmpty();
	}

	@Test
	void corrupt_ct_sym_is_empty() throws IOException {
		var ctSym = Files.writeString(tempDir.resolve("ct.sym"), "not a zip file");

		assertThat(CtSymPlatform.open(ctSym, 21)).isEmpty();
	}

	private ClasspathTypeProvider provider(int release) {
		var platform = CtSymPlatform.open(HOST_CT_SYM, release).orElseThrow();
		return new ClasspathTypeProvider(extractor, List.of(), release, platform);
	}

	private static List<String> methodNames(Set<MethodDecl> methods) {
		return methods.stream().map(MethodDecl::getSimpleName).toList();
	}
}
