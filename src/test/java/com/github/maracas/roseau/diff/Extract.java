package com.github.maracas.roseau.diff;

import com.google.common.base.CaseFormat;
import spoon.Launcher;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Extract {
	static final Path LIB_V1 = Path.of("lib-v1/src/testing_lib");
	static final Path LIB_V2 = Path.of("lib-v2/src/testing_lib");

	public static void main(String[] args) {
		LIB_V1.toFile().mkdirs();
		LIB_V2.toFile().mkdirs();

		var launcher = new Launcher();
		launcher.addInputResource("src/test/java/com/github/maracas/roseau/diff");
		launcher.getEnvironment().setComplianceLevel(21);

		var model = launcher.buildModel();

		var testMethods = model.getElements(new TypeFilter<>(CtMethod.class))
			.stream()
			.filter(method -> method.getSimpleName().contains("_"))
			.filter(method -> method.getAnnotations().stream()
				.map(CtAnnotation::getAnnotationType)
				.map(CtTypeReference::getQualifiedName)
				.anyMatch(name -> name.equals("org.junit.jupiter.api.Test"))) // Match the annotation name
			.toList();

		testMethods.forEach(m -> {
			var v1 = m.getElements(new TypeFilter<>(CtVariable.class))
				.stream()
				.filter(v -> v.getSimpleName().equals("v1"))
				.map(CtVariable::getDefaultExpression)
				.findFirst()
				.orElseThrow();

			var v2 = m.getElements(new TypeFilter<>(CtVariable.class))
				.stream()
				.filter(v -> v.getSimpleName().equals("v1"))
				.map(CtVariable::getDefaultExpression)
				.findFirst()
				.orElseThrow();

			var caseName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, m.getSimpleName());
			var v1path = LIB_V1.resolve(caseName).resolve("A.java");
			var v2path = LIB_V2.resolve(caseName).resolve("A.java");

			try {
				v1path.getParent().toFile().mkdirs();
				v2path.getParent().toFile().mkdirs();
				v1path.toFile().createNewFile();
				v2path.toFile().createNewFile();
				Files.write(v1path, ("package testing_lib." + caseName + ";\n\n" +
					v1.toString().substring(1, v1.toString().length() - 1)).getBytes(StandardCharsets.UTF_8));
				Files.write(v2path, ("package testing_lib." + caseName + ";\n\n" +
					v2.toString().substring(1, v2.toString().length() - 1)).getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}
