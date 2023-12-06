package com.github.maracas.roseau.api.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.junit.jupiter.api.Assertions.*;

class TypeReferenceTest {
	@Test
	void jdk_type() {
		var api = buildAPI("""
			class A extends String {}""");

		var a = assertClass(api, "A");
		var sup = a.getSuperClass();

		System.out.println("sup="+sup);
		System.out.println(a.getAllMethods());
		System.out.println(a.getAllImplementedInterfaces());

		var chars = a.getAllMethods().stream()
			.filter(m -> "java.lang.String.chars".equals(m.getQualifiedName()))
			.findFirst()
			.get();

		System.out.println("chars="+chars);
		System.out.println("transitiveRef="+chars.getType().getAllMethods());
	}

	@Test
	void unknown_type() {
		var api = buildAPI("""
			class A extends Unknown {}""");

		var a = assertClass(api, "A");
		var sup = a.getSuperClass();

		System.out.println("sup="+sup);
		System.out.println(a.getAllMethods());
		System.out.println(a.getAllImplementedInterfaces());
	}
}
