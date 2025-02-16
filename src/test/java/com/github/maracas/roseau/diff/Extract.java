package com.github.maracas.roseau.diff;

import com.google.common.base.CaseFormat;
import spoon.Launcher;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extract {
	static final Path LIB_V1 = Path.of("lib-v1/src/testing_lib");
	static final Path LIB_V2 = Path.of("lib-v2/src/testing_lib");
	static final Path CLIENT = Path.of("client/src");

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
				.anyMatch(name -> name.equals("org.junit.jupiter.api.Test")))
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
				.filter(v -> v.getSimpleName().equals("v2"))
				.map(CtVariable::getDefaultExpression)
				.findFirst()
				.orElseThrow();

			var caseName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
				m.getDeclaringType().getSimpleName() + "_" + m.getSimpleName());
			var code1 = v1.toString().startsWith("\"\"\"")
				? v1.toString().substring(3, v1.toString().length() - 3)
				: v1.toString().substring(1, v1.toString().length() - 1);
			var code2 = v2.toString().startsWith("\"\"\"")
				? v2.toString().substring(3, v2.toString().length() - 3)
				: v2.toString().substring(1, v2.toString().length() - 1);

			buildSourcesMap(code1).forEach((typeName, code) -> {
				try {
					var v1path = LIB_V1.resolve(caseName).resolve(typeName + ".java");
					if (v1path.toFile().exists()) {
						System.err.println("Exists: " + v1path);
					}
					v1path.getParent().toFile().mkdirs();
					v1path.toFile().createNewFile();
					Files.writeString(v1path, "package testing_lib." + caseName + ";\n\n" + code + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			buildSourcesMap(code2).forEach((typeName, code) -> {
				try {
					var v2path = LIB_V2.resolve(caseName).resolve(typeName + ".java");
					if (v2path.toFile().exists()) {
						System.err.println("Exists: " + v2path);
					}
					v2path.getParent().toFile().mkdirs();
					v2path.toFile().createNewFile();
					Files.writeString(v2path, "package testing_lib." + caseName + ";\n\n" + code + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			try {
				var clientPath = CLIENT.resolve(caseName).resolve("Main.java");
				if (clientPath.toFile().exists()) {
					System.err.println("Exists: " + clientPath);
				}
				clientPath.getParent().toFile().mkdirs();
				clientPath.toFile().createNewFile();
				Files.writeString(clientPath, "package " + caseName + ";\n\n" +
					"public class Main {\n" +
					"\tpublic static void main(String[] args) {\n" +
					"\t/*V1:\n" + code1 + "*/\n" +
					"\t/*V2:\n" + code2 + "*/\n" +
					"\t}\n" +
					"}\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public static Map<String, String> buildSourcesMap(String sources) {
		Pattern typePattern = Pattern.compile(
			"(?m)^(?!\\s)(?:@\\w+(?:\\([^)]*\\))?\\s+)*(?:(?:public|protected|private|static|final|abstract|sealed)\\s+)*" +
				"(class|interface|@interface|enum|record)\\s+(\\w+)");
		Matcher matcher = typePattern.matcher(sources);

		List<Integer> typeStartIndices = new ArrayList<>();
		List<String> typeNames = new ArrayList<>();
		while (matcher.find()) {
			typeStartIndices.add(matcher.start());
			typeNames.add(matcher.group(2));
		}

		Map<String, String> sourcesMap = new HashMap<>();
		for (int i = 0; i < typeStartIndices.size(); i++) {
			var startPos = typeStartIndices.get(i);
			var endPos = i < typeStartIndices.size() - 1 ? typeStartIndices.get(i + 1) : sources.length();
			var typeName = typeNames.get(i);
			sourcesMap.put(typeName, sources.substring(startPos, endPos));
		}

		return sourcesMap;
	}
}
