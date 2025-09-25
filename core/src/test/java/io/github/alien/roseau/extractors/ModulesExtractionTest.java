package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ModulesExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void simple_export(ApiBuilder builder) {
		var api = builder.build("""
			module m {
				exports pkg1;
			}
			
			package pkg1;
			public class C {}
			public interface I {}
			
			package pkg2;
			public class C {}
			public interface I {}""");

		var module = api.getLibraryTypes().getModule();
		assertThat(module.getQualifiedName()).isEqualTo("m");
		assertThat(module.isUnnamed()).isFalse();
		assertThat(module.isExporting("pkg1")).isTrue();
		assertThat(module.isExporting("pkg2")).isFalse();

		assertThat(api.getExportedTypes().stream().map(TypeDecl::getQualifiedName))
			.containsExactlyInAnyOrder("pkg1.C", "pkg1.I");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void qualified_export(ApiBuilder builder) {
		var api = builder.build("""
			module m {
				exports pkg1 to java.xml;
			}
			
			package pkg1;
			public class C {}
			public interface I {}
			
			package pkg2;
			public class C {}
			public interface I {}""");

		var module = api.getLibraryTypes().getModule();
		assertThat(module.getQualifiedName()).isEqualTo("m");
		assertThat(module.isUnnamed()).isFalse();
		assertThat(module.isExporting("pkg1")).isFalse();
		assertThat(module.isExporting("pkg2")).isFalse();

		assertThat(api.getExportedTypes()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void simple_open(ApiBuilder builder) {
		var api = builder.build("""
			module m {
				opens pkg1;
			}
			
			package pkg1;
			public class C {}
			public interface I {}
			
			package pkg2;
			public class C {}
			public interface I {}""");

		var module = api.getLibraryTypes().getModule();
		assertThat(module.getQualifiedName()).isEqualTo("m");
		assertThat(module.isUnnamed()).isFalse();
		assertThat(module.isExporting("pkg1")).isFalse();
		assertThat(module.isExporting("pkg2")).isFalse();

		assertThat(api.getExportedTypes()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void unnamed_module(ApiBuilder builder) {
		var api = builder.build("""
			package pkg1;
			public class C {}
			public interface I {}
			
			package pkg2;
			public class C {}
			public interface I {}""");

		var module = api.getLibraryTypes().getModule();
		assertThat(module.getQualifiedName()).isEqualTo("<unnamed module>");
		assertThat(module.isUnnamed()).isTrue();
		assertThat(module.isExporting("pkg1")).isTrue();
		assertThat(module.isExporting("pkg2")).isTrue();

		assertThat(api.getExportedTypes().stream().map(TypeDecl::getQualifiedName))
			.containsExactlyInAnyOrder("pkg1.C", "pkg1.I", "pkg2.C", "pkg2.I");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void qualified_module(ApiBuilder builder) {
		var api = builder.build("""
			module my.module {
				exports pkg1;
			}
			
			package pkg1;
			public class C {}
			
			package pkg2;
			public class C {}""");

		var module = api.getLibraryTypes().getModule();
		assertThat(module.getQualifiedName()).isEqualTo("my.module");
		assertThat(module.isUnnamed()).isFalse();
		assertThat(module.isExporting("pkg1")).isTrue();

		assertThat(api.getExportedTypes().stream().map(TypeDecl::getQualifiedName))
			.containsExactlyInAnyOrder("pkg1.C");
	}
}
