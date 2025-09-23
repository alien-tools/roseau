package io.github.alien.roseau.extractors;

import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertNoType;
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

		assertClass(api, "pkg1.C");
		assertInterface(api, "pkg1.I");
		assertNoType(api, "pkg2.C");
		assertNoType(api, "pkg2.I");
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

		assertNoType(api, "pkg1.C");
		assertNoType(api, "pkg1.I");
		assertNoType(api, "pkg2.C");
		assertNoType(api, "pkg2.I");
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

		assertNoType(api, "pkg1.C");
		assertNoType(api, "pkg1.I");
		assertNoType(api, "pkg2.C");
		assertNoType(api, "pkg2.I");
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

		assertClass(api, "pkg1.C");
		assertInterface(api, "pkg1.I");
		assertClass(api, "pkg2.C");
		assertInterface(api, "pkg2.I");
	}
}
