package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpoonTypeProviderTest {
	SpoonTypeProvider newProvider(List<Path> classpath) {
		CachingTypeReferenceFactory factory = new CachingTypeReferenceFactory();
		return new SpoonTypeProvider(factory, classpath);
	}

	@Test
	void resolve_jdk() {
		var cp = List.<Path>of();
		var provider = newProvider(cp);
		var opt = provider.<ClassDecl>findType("java.lang.Number");

		assertThat(opt).isPresent();

		var number = opt.orElseThrow();
		assertThat(number.isAbstract()).isTrue();
		assertThat(number.getDeclaredConstructors()).singleElement();
		assertThat(number.getDeclaredMethods()).hasSize(6);
		assertThat(number.getDeclaredFields()).isEmpty();
		assertThat(number.getSuperClass()).isEqualTo(TypeReference.OBJECT);
		assertThat(number.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("java.io.Serializable"));
	}

	@Test
	void resolve_unknown() {
		var cp = List.<Path>of();
		var provider = newProvider(cp);
		var opt = provider.<ClassDecl>findType("java.lang.Unknown");

		assertThat(opt).isEmpty();
	}

	@Test
	void resolve_with_classpath() {
		var cp = List.of(Path.of("src/test/resources/api-showcase.jar"));
		var provider = newProvider(cp);
		var opt = provider.<ClassDecl>findType("io.github.alien.roseau.APIShowcase$Square");

		assertThat(opt).isPresent();

		var square = opt.orElseThrow();
		assertThat(square.isFinal()).isTrue();
		assertThat(square.getDeclaredConstructors()).isEmpty();
		assertThat(square.getDeclaredMethods()).singleElement();
		assertThat(square.getDeclaredFields()).isEmpty();
		assertThat(square.getSuperClass()).isEqualTo(TypeReference.OBJECT);
		assertThat(square.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("io.github.alien.roseau.APIShowcase$Shape"));
	}
}
