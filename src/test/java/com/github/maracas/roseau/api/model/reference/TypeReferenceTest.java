package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.SpoonAPIFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.factory.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

class TypeReferenceTest {
	SpoonAPIFactory factory;

	@BeforeEach
	void setUp() {
		factory = new SpoonAPIFactory(new Launcher().createFactory().Type());
	}

	@Test
	void foo() {
		TypeReference<ClassDecl> x = new TypeReference<>("java.lang.String", factory);

		System.out.println(x.getResolvedApiType());
	}
}