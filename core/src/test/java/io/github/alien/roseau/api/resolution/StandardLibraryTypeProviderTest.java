package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StandardLibraryTypeProviderTest {
	private StandardLibraryTypeProvider provider;

	@BeforeEach
	void setUp() {
		var extractor = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
		provider = new StandardLibraryTypeProvider(extractor);
	}

	@Test
	void findType_object_class() {
		var result = provider.findType("java.lang.Object").orElseThrow();
		assertThat(result).isInstanceOf(ClassDecl.class);
	}

	@Test
	void findType_string_class() {
		var result = provider.findType("java.lang.String").orElseThrow();
		assertThat(result).isInstanceOf(ClassDecl.class);
	}

	@Test
	void findType_sql_module() {
		var result = provider.findType("java.sql.SQLData", InterfaceDecl.class).orElseThrow();
		assertThat(result).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void findType_jdk_tools() {
		var result = provider.findType("jdk.tools.jlink.internal.Jlink$JlinkConfiguration").orElseThrow();
		assertThat(result).isInstanceOf(ClassDecl.class);
	}

	@Test
	void findType_javax_swing() {
		var result = provider.findType("javax.swing.tree.TreeModel").orElseThrow();
		assertThat(result).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void findType_inner_class() {
		var result = provider.findType("java.util.Map$Entry").orElseThrow();
		assertThat(result).isInstanceOf(InterfaceDecl.class);
	}

	@Test
	void findType_inner_enum() {
		var result = provider.findType("java.lang.Thread$State").orElseThrow();
		assertThat(result).isInstanceOf(EnumDecl.class);
	}

	@Test
	void findType_unknown_class() {
		var result = provider.findType("java.lang.Unknown");
		assertThat(result).isEmpty();
	}

	@Test
	void findType_unknown_package() {
		var result = provider.findType("java.nonexisting.Cls");
		assertThat(result).isEmpty();
	}

	@Test
	void findType_unexpected_decl() {
		var result = provider.findType("java.lang.String", InterfaceDecl.class);
		assertThat(result).isEmpty();
	}
}
