package io.github.alien.roseau.api.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleDeclTest {
	@Test
	void named_modules_only_export_their_exported_packages() {
		var module = new ModuleDecl("pkg.mod", Set.of("pkg.api"));

		assertThat(module.isExporting("pkg.api")).isTrue();
		assertThat(module.isExporting("pkg.internal")).isFalse();
		assertThat(module.isExporting("")).isFalse();
		assertThat(module.isUnnamed()).isFalse();
	}

	@Test
	void the_unnamed_module_exports_everything() {
		assertThat(ModuleDecl.UNNAMED_MODULE.isUnnamed()).isTrue();
		assertThat(ModuleDecl.UNNAMED_MODULE.getExports()).isEmpty();
		assertThat(ModuleDecl.UNNAMED_MODULE.isExporting("pkg.whatever")).isTrue();
		assertThat(ModuleDecl.UNNAMED_MODULE.isExporting("")).isTrue();
	}

	@Test
	void deserializing_the_unnamed_module_yields_the_singleton() {
		var unnamed = ModuleDecl.create("<unnamed module>", Set.of());

		assertThat(unnamed).isSameAs(ModuleDecl.UNNAMED_MODULE);
		assertThat(unnamed.isExporting("pkg.whatever")).isTrue();
	}

	@Test
	void deserializing_a_named_module_without_exports_is_supported() {
		var module = ModuleDecl.create("pkg.mod", null);

		assertThat(module.getQualifiedName()).isEqualTo("pkg.mod");
		assertThat(module.getExports()).isEmpty();
		assertThat(module.isExporting("pkg.api")).isFalse();
	}

	@Test
	void modules_are_identified_by_their_name_and_exports() {
		var m1 = new ModuleDecl("pkg.mod", Set.of("pkg.api"));
		var m2 = new ModuleDecl("pkg.mod", Set.of("pkg.api"));
		var otherExports = new ModuleDecl("pkg.mod", Set.of("pkg.api", "pkg.spi"));
		var otherName = new ModuleDecl("pkg.other", Set.of("pkg.api"));

		assertThat(m1).isEqualTo(m2).isNotEqualTo(otherExports).isNotEqualTo(otherName);
		assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
	}
}
