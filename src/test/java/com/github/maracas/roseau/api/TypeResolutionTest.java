package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.reflect.CtModel;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeResolutionTest {
	API api;
	TypeDecl a;

	@BeforeEach
	void setUp() {
		CtModel model = SpoonAPIExtractor.buildModel(Path.of("src/test/resources/api-type-resolver/"));
		APIExtractor extractor = new SpoonAPIExtractor(model);
		api = extractor.extractAPI();
		a = api.getType("A").get();
	}

	@Test
	void self_reference() {
		var selfReference = a.getField("selfReference").get().getType();

		assertEquals(10, selfReference.getFields().size());
	}

	@Test
	void public_api_reference() {
		var publicApiReference = a.getField("publicApiReference").get().getType();

		assertTrue(publicApiReference.isPublic());
	}

	@Test
	void private_api_reference() {
		var privateApiReference = a.getField("privateApiReference").get().getType();

		assertTrue(privateApiReference.isPackagePrivate());
	}

	@Test
	void primitive_reference() {
		var primitiveReference = a.getField("primitiveReference").get().getType();

		assertEquals("int", primitiveReference.getQualifiedName());
	}

	@Test
	void jdk_reference() {
		var jdkReference = a.getField("jdkReference").get().getType();

		assertEquals("java.lang.String", jdkReference.getQualifiedName());
	}

	@Test
	void jdk_parameterized_reference() {
		var jdkParameterizedReference = a.getField("jdkParameterizedReference").get().getType();

		assertEquals("java.util.List", jdkParameterizedReference.getQualifiedName());
		assertEquals("java.lang.String", jdkParameterizedReference.getFormalTypeParameters().get(0).bounds().get(0));
	}
}
