package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpoonTypeProviderTest {
	SpoonTypeProvider newProvider(Set<Path> classpath) {
		var factory = new DefaultApiFactory(new CachingTypeReferenceFactory());
		return new SpoonTypeProvider(factory, classpath);
	}

	@Test
	void resolve_jdk() {
		var cp = Set.<Path>of();
		var provider = newProvider(cp);
		var opt = provider.findType("java.lang.Number", ClassDecl.class);

		assertThat(opt).isPresent();

		var number = opt.orElseThrow();
		assertThat(number.isAbstract()).isTrue();
		assertThat(number.getDeclaredConstructors()).hasSize(1);
		assertThat(number.getDeclaredMethods()).hasSize(6);
		assertThat(number.getDeclaredFields()).isEmpty();
		assertThat(number.getSuperClass()).isEqualTo(TypeReference.OBJECT);
		assertThat(number.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("java.io.Serializable"));
	}

	@Test
	void resolve_unknown() {
		var cp = Set.<Path>of();
		var provider = newProvider(cp);
		var opt = provider.findType("java.lang.Unknown");

		assertThat(opt).isEmpty();
	}

	@Test
	void resolve_with_classpath() {
		var showcase = Path.of("src/test/resources/api-showcase.jar");
		var cp = Set.of(showcase);
		var provider = newProvider(cp);
		var opt = provider.findType("io.github.alien.roseau.APIShowcase$Square", ClassDecl.class);

		assertThat(opt).isPresent();

		var square = opt.orElseThrow();
		assertThat(square.isFinal()).isTrue();
		assertThat(square.getDeclaredConstructors()).isEmpty();
		assertThat(square.getDeclaredMethods()).hasSize(1);
		assertThat(square.getDeclaredFields()).isEmpty();
		assertThat(square.getSuperClass()).isEqualTo(TypeReference.OBJECT);
		assertThat(square.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("io.github.alien.roseau.APIShowcase$Shape"));
	}

	@Test
	void resolve_unexpected_kind() {
		var provider = newProvider(Set.of());
		var opt = provider.findType("java.lang.Number", InterfaceDecl.class);

		assertThat(opt).isEmpty();
	}

	@Test
	void resolve_unexpected_kind_sub() {
		var provider = newProvider(Set.of());
		var opt = provider.findType("java.time.DayOfWeek", ClassDecl.class);

		assertThat(opt).isPresent();
	}
}
