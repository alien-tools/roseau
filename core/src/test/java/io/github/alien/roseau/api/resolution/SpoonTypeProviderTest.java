package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		opt.ifPresentOrElse(number -> {
			assertTrue(number.isAbstract());
			assertThat(number.getDeclaredConstructors(), hasSize(1));
			assertThat(number.getDeclaredMethods(), hasSize(6));
			assertThat(number.getDeclaredFields(), hasSize(0));
			assertThat(number.getSuperClass(), is(equalTo(TypeReference.OBJECT)));
			assertThat(number.getImplementedInterfaces(), hasSize(1));
			assertThat(number.getImplementedInterfaces().getFirst(),
				is(equalTo(new TypeReference<>("java.io.Serializable"))));
		}, Assertions::fail);
	}

	@Test
	void resolve_unknown() {
		var cp = List.<Path>of();
		var provider = newProvider(cp);
		var opt = provider.<ClassDecl>findType("java.lang.Unknown");

		assertThat(opt, isEmpty());
	}

	@Test
	void resolve_with_classpath() {
		var cp = List.of(Path.of("src/test/resources/api-showcase.jar"));
		var provider = newProvider(cp);
		var opt = provider.<ClassDecl>findType("io.github.alien.roseau.APIShowcase$Square");

		opt.ifPresentOrElse(square -> {
			assertTrue(square.isFinal());
			assertThat(square.getDeclaredConstructors(), hasSize(0));
			assertThat(square.getDeclaredMethods(), hasSize(1));
			assertThat(square.getDeclaredFields(), hasSize(0));
			assertThat(square.getSuperClass(), is(equalTo(TypeReference.OBJECT)));
			assertThat(square.getImplementedInterfaces(), hasSize(1));
			assertThat(square.getImplementedInterfaces().getFirst(),
				is(equalTo(new TypeReference<>("io.github.alien.roseau.APIShowcase$Shape"))));
		}, Assertions::fail);
	}
}