package com.github.maracas.roseau.api.extractors.jar;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.extractors.APIExtractor;
import com.github.maracas.roseau.api.extractors.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.SpoonTypeReferenceFactory;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import com.github.maracas.roseau.diff.APIDiff;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JarAPIExtractor implements APIExtractor {
	private static final int ASM_VERSION = Opcodes.ASM9;
	private static final int PARSING_OPTIONS = ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES; // | ClassReader.SKIP_DEBUG;
	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public API extractAPI(Path sources) {
		SpoonAPIFactory apiFactory = new SpoonAPIFactory();
		TypeReferenceFactory typeRefFactory = new SpoonTypeReferenceFactory(apiFactory);

		try (JarFile jar = new JarFile(sources.toFile())) {
			List<TypeDecl> typeDecls =
				jar.stream()
					.filter(entry -> entry.getName().endsWith(".class") && !entry.isDirectory())
					.flatMap(entry -> {
						try (InputStream is = jar.getInputStream(entry)) {
							ClassReader reader = new ClassReader(is);
							APIClassVisitor visitor = new APIClassVisitor(ASM_VERSION, typeRefFactory);
							reader.accept(visitor, PARSING_OPTIONS);
							return Optional.ofNullable(visitor.getTypeDecl()).stream();
						} catch (IOException e) {
							LOGGER.error("Error processing JAR entry", e);
							return Stream.empty();
						}
					})
					.toList();

			return new API(typeDecls, new SpoonAPIFactory());
		} catch (IOException e) {
			throw new RuntimeException("Error processing JAR file", e);
		}
	}

	static void compChanges() {
		var jarApi = new JarAPIExtractor().extractAPI(Path.of("/home/dig/repositories/maracas/test-data/comp-changes/old/target/comp-changes-old-0.0.1.jar"));
		var sourcesApi = new SpoonAPIExtractor().extractAPI(Path.of("/home/dig/repositories/maracas/test-data/comp-changes/old/src"));

		diffAPIs(jarApi, sourcesApi);
		diffAPIs(sourcesApi, jarApi);
		diffBCs(jarApi, sourcesApi);
		diffBCs(sourcesApi, jarApi);
	}

	static void guava() {
		var jarApi = new JarAPIExtractor().extractAPI(Path.of("/home/dig/repositories/guava-31.1/guava/target/guava-31.1-jre.jar"));
		var sourcesApi = new SpoonAPIExtractor().extractAPI(Path.of("/home/dig/repositories/guava-31.1/guava/src"));

//		diffAPIs(jarApi, sourcesApi);
//		diffAPIs(sourcesApi, jarApi);
		diffBCs(jarApi, sourcesApi);
		diffBCs(sourcesApi, jarApi);
	}

	static void asmtest() {
		var jarApi = new JarAPIExtractor().extractAPI(Path.of("/home/dig/repositories/asmtest/target/asmtest-1.0-SNAPSHOT.jar"));
		var sourcesApi = new SpoonAPIExtractor().extractAPI(Path.of("/home/dig/repositories/asmtest/src"));

		diffAPIs(jarApi, sourcesApi);
		diffAPIs(sourcesApi, jarApi);
		diffBCs(jarApi, sourcesApi);
		diffBCs(sourcesApi, jarApi);
	}

	static void diffAPIs(API api1, API api2) {
		api1.getAllTypes().forEach(t1 -> {
			var optT2 = api2.findType(t1.getQualifiedName());

			optT2.ifPresentOrElse(t2 -> {
				System.out.println("###" + t1.getQualifiedName());

				if (!t1.getClass().equals(t2.getClass()))
					System.out.printf("\t%s != %s%n", t1, t2);

				if (!t1.getModifiers().equals(t2.getModifiers()))
					System.out.printf("\t%s[%s] %s != %s%n", t1.getQualifiedName(), t1.isEnum(), t1.getModifiers(), t2.getModifiers());

				if (t1.getVisibility() != t2.getVisibility())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getVisibility(), t2.getVisibility());

				if (t1.getFormalTypeParameters().size() != t2.getFormalTypeParameters().size())
					System.out.printf("\t%s != %s%n", t1.getFormalTypeParameters(), t2.getFormalTypeParameters());
				for (int i = 0; i < t1.getFormalTypeParameters().size(); i++)
					if (!t1.getFormalTypeParameters().get(i).equals(t2.getFormalTypeParameters().get(i)))
						System.out.printf("\t%s != %s%n", t1.getFormalTypeParameters().get(i), t2.getFormalTypeParameters().get(i));

				if (t1.getImplementedInterfaces().size() != t2.getImplementedInterfaces().size())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getImplementedInterfaces(), t2.getImplementedInterfaces());
				for (int i = 0; i < t1.getImplementedInterfaces().size(); i++)
					if (!t1.getImplementedInterfaces().get(i).equals(t2.getImplementedInterfaces().get(i)))
						System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getImplementedInterfaces().get(i), t2.getImplementedInterfaces().get(i));

				if (t1.getDeclaredFields().size() != t2.getDeclaredFields().size())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredFields(), t2.getDeclaredFields());
				t1.getDeclaredFields().forEach(f1 -> {
					if (!t2.getDeclaredFields().contains(f1)) {
						System.out.printf("\tNo match for field %s: %s%n", f1, t2.getDeclaredFields());
					}
				});

				if (t1.getDeclaredMethods().size() != t2.getDeclaredMethods().size())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredMethods(), t2.getDeclaredMethods());
				t1.getDeclaredMethods().forEach(m1 -> {
					if (!t2.getDeclaredMethods().stream().anyMatch(m2 -> {
						if (!Objects.equals(m1.getQualifiedName(), m2.getQualifiedName()))
							return false;
						if (!Objects.equals(m1.getFormalTypeParameters(), m2.getFormalTypeParameters()))
							return false;
						if (!Objects.equals(m1.getParameters().stream().map(ParameterDecl::type).toList(), m2.getParameters().stream().map(ParameterDecl::type).toList()))
							return false;
						if (!Objects.equals(m1.getType(), m2.getType()))
							return false;
						if (!Objects.equals(m1.getVisibility(), m2.getVisibility()))
							return false;
						if (!Objects.equals(m1.getModifiers(), m2.getModifiers()))
							return false;
						if (!Objects.equals(m1.getThrownExceptions(), m2.getThrownExceptions()))
							return false;
						return true;
					})) {
						System.out.printf("\tNo match for method %s: %s%n", m1, t2.getDeclaredMethods());
					}
				});

				//				if (t1.getDeclaredMethods().size() != t2.getDeclaredMethods().size())
//					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredMethods(), t2.getDeclaredMethods());
//				for (int i = 0; i < t1.getDeclaredMethods().size(); i++)
//					if (!t1.getDeclaredMethods().get(i).equals(t2.getDeclaredMethods().get(i)))
//						System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredMethods().get(i), t2.getDeclaredMethods().get(i));

				if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2) {
					var sup1 = c1.getSuperClass();
					var sup2 = c2.getSuperClass();

					if (sup1.isPresent() != sup2.isPresent())
						System.out.printf("\t%s %s != %s%n", c1.getQualifiedName(), sup1, sup2);
					if (sup1.isPresent()) {
						if (!sup1.get().equals(sup2.get())) {
							System.out.printf("\t%s %s != %s%n", c1.getQualifiedName(), sup1.get(), sup2.get());
						}
					}

					if (c1.getConstructors().size() != c2.getConstructors().size())
						System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), c1.getConstructors(), c2.getConstructors());
					c1.getConstructors().forEach(cons1 -> {
						if (!c2.getConstructors().stream().anyMatch(cons2 -> {
							if (!Objects.equals(cons1.getQualifiedName(), cons2.getQualifiedName()))
								return false;
							if (!Objects.equals(cons1.getFormalTypeParameters(), cons2.getFormalTypeParameters()))
								return false;
							if (!Objects.equals(cons1.getParameters().stream().map(ParameterDecl::type).toList(), cons2.getParameters().stream().map(ParameterDecl::type).toList()))
								return false;
							if (!Objects.equals(cons1.getType(), cons2.getType()))
								return false;
							if (!Objects.equals(cons1.getVisibility(), cons2.getVisibility()))
								return false;
							if (!Objects.equals(cons1.getModifiers(), cons2.getModifiers()))
								return false;
							if (!Objects.equals(cons1.getThrownExceptions(), cons2.getThrownExceptions()))
								return false;
							return true;
						})) {
							System.out.printf("\tNo match for constructor %s: %s%n", cons1, c2.getConstructors());
						}
					});
				}
			}, () -> System.out.printf("%s not found%n", t1.getQualifiedName()));
		});
	}

	static void diffBCs(API api1, API api2) {
		var diff = new APIDiff(api1, api2);
		var bcs = diff.diff();
		System.out.printf("%s BCs:%n%s%n", bcs.size(),
			bcs.stream().map(Object::toString).collect(Collectors.joining("\n")));
	}

	public static void main(String[] args) throws Exception {
//		compChanges();
		guava();
//		asmtest();
	}
}
