package io.github.alien.roseau.footprint;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Generates a single {@code Footprint.java} source file from a source tree.
 */
public final class FootprintService {
	/**
	 * Default package used for generated footprint sources.
	 */
	public static final String DEFAULT_PACKAGE = "io.github.alien.roseau.footprint.generated";

	/**
	 * Default class name used for generated footprint sources.
	 */
	public static final String DEFAULT_CLASS_NAME = "Footprint";

	public String generate(Path sourceTree, String packageName, String className) {
		return generate(sourceTree, Optional.empty(), packageName, className);
	}

	public String generate(Path sourceTree, Path pomFile, String packageName, String className) {
		return generate(sourceTree, Optional.ofNullable(pomFile), packageName, className);
	}

	private String generate(Path sourceTree, Optional<Path> pomFile, String packageName, String className) {
		return generateResult(sourceTree, pomFile, packageName, className).source();
	}

	private record GenerationResult(API api, String source) {
	}

	private GenerationResult generateResult(Path sourceTree, Optional<Path> pomFile, String packageName, String className) {
		Objects.requireNonNull(sourceTree);
		Objects.requireNonNull(pomFile);
		Objects.requireNonNull(packageName);
		Objects.requireNonNull(className);

		Library.Builder builder = Library.builder().location(sourceTree.toAbsolutePath().normalize());
		pomFile.map(path -> path.toAbsolutePath().normalize()).ifPresent(builder::pom);
		Library library = builder.build();
		API api = Roseau.buildAPI(library);
		return new GenerationResult(api, new FootprintGenerator(packageName, className).generate(api));
	}

	public Path generateToFile(Path sourceTree, Path outputFile, String packageName, String className) throws IOException {
		return generateToFile(sourceTree, outputFile, null, packageName, className);
	}

	public Path generateToFile(Path sourceTree, Path outputFile, Path pomFile, String packageName, String className)
		throws IOException {
		Objects.requireNonNull(outputFile);
		GenerationResult generated = generateResult(sourceTree, Optional.ofNullable(pomFile), packageName, className);
		String source = generated.source();

		Path target = outputFile.toAbsolutePath().normalize();
		if (Files.isDirectory(target)) {
			target = target.resolve(className + ".java");
		}

		Path parent = target.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		Files.writeString(target, source, StandardCharsets.UTF_8);
		writePackageInfoCompanion(target, packageName, generated.api());
		return target;
	}

	private void writePackageInfoCompanion(Path generatedFile, String packageName, API api) throws IOException {
		if (packageName.isBlank()) {
			return;
		}
		List<String> packageAnnotations = api.getExportedTypes().stream()
			.filter(AnnotationDecl.class::isInstance)
			.map(AnnotationDecl.class::cast)
			.filter(annotation -> isDirectlyAccessible(annotation, api))
			.filter(annotation -> annotation.getTargets().contains(ElementType.PACKAGE))
			.map(annotation -> renderAnnotationApplication(annotation, api))
			.filter(Optional::isPresent)
			.map(Optional::get)
			.sorted()
			.toList();
		if (packageAnnotations.isEmpty()) {
			return;
		}

		Path packageInfo = generatedFile.getParent().resolve("package-info.java");
		StringBuilder content = new StringBuilder(256);
		for (String annotation : packageAnnotations) {
			content.append(annotation).append('\n');
		}
		content.append("package ").append(packageName).append(";\n");
		Files.writeString(packageInfo, content.toString(), StandardCharsets.UTF_8);
	}

	private boolean isDirectlyAccessible(TypeDecl type, API api) {
		if (type.getPackageName().isEmpty() || !type.isPublic()) {
			return false;
		}
		Optional<TypeReference<TypeDecl>> enclosing = type.getEnclosingType();
		while (enclosing.isPresent()) {
			Optional<TypeDecl> resolved = api.resolver().resolve(enclosing.get());
			if (resolved.isEmpty() || !resolved.get().isPublic()) {
				return false;
			}
			enclosing = resolved.get().getEnclosingType();
		}
		return true;
	}

	private Optional<String> renderAnnotationApplication(AnnotationDecl annotation, API api) {
		String annotationName = "@" + annotation.getQualifiedName().replace('$', '.');
		List<AnnotationMethodDecl> required = annotation.getAnnotationMethods().stream()
			.filter(method -> !method.hasDefault())
			.sorted(Comparator.comparing(AnnotationMethodDecl::getSignature))
			.toList();
		if (required.isEmpty()) {
			return Optional.of(annotationName);
		}
		if (required.size() == 1 && "value".equals(required.getFirst().getSimpleName())) {
			return renderAnnotationElementValue(required.getFirst().getType(), api, new HashSet<>())
				.map(value -> annotationName + "(" + value + ")");
		}
		List<String> values = new ArrayList<>(required.size());
		for (AnnotationMethodDecl method : required) {
			Optional<String> rendered = renderAnnotationElementValue(method.getType(), api, new HashSet<>());
			if (rendered.isEmpty()) {
				return Optional.empty();
			}
			values.add(method.getSimpleName() + " = " + rendered.get());
		}
		return Optional.of(annotationName + "(" + String.join(", ", values) + ")");
	}

	private Optional<String> renderAnnotationElementValue(ITypeReference type, API api, Set<String> visiting) {
		return switch (type) {
			case PrimitiveTypeReference primitive -> Optional.of(switch (primitive.name()) {
				case "boolean" -> "false";
				case "char" -> "'x'";
				case "long" -> "0L";
				case "float" -> "0f";
				case "double" -> "0d";
				default -> "0";
			});
			case ArrayTypeReference array -> renderAnnotationElementValue(array.componentType(), api, visiting)
				.map(value -> "{ " + value + " }");
			case TypeReference<?> reference -> {
				if (String.class.getCanonicalName().equals(reference.getQualifiedName())) {
					yield Optional.of("\"value\"");
				}
				if (Class.class.getCanonicalName().equals(reference.getQualifiedName())) {
					yield Optional.of("java.lang.Object.class");
				}
				Optional<TypeDecl> resolved = api.resolver().resolve(reference);
				if (resolved.isEmpty()) {
					yield Optional.empty();
				}
				if (resolved.get() instanceof EnumDecl enumDecl) {
					yield enumDecl.getValues().stream()
						.sorted(Comparator.comparing(EnumValueDecl::getSimpleName))
						.map(value -> enumDecl.getQualifiedName().replace('$', '.') + "." + value.getSimpleName())
						.findFirst();
				}
				if (resolved.get() instanceof AnnotationDecl annDecl) {
					if (!visiting.add(annDecl.getQualifiedName())) {
						yield Optional.empty();
					}
					Optional<String> nested = renderAnnotationApplication(annDecl, api);
					visiting.remove(annDecl.getQualifiedName());
					yield nested;
				}
				yield Optional.empty();
			}
			case WildcardTypeReference _ -> Optional.empty();
			default -> Optional.empty();
		};
	}
}
