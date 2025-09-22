package io.github.alien.roseau.utils;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonTypesExtractor;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibilityChange;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
import japicmp.output.Filter;
import org.opentest4j.AssertionFailedError;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.support.compiler.VirtualFile;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestUtils {
	private TestUtils() {
	}

	public static void assertBC(String symbol, BreakingChangeKind kind, int line, List<BreakingChange> bcs) {
		List<BreakingChange> matches = bcs.stream()
			.filter(bc ->
				   kind == bc.kind()
				&& line == bc.impactedSymbol().getLocation().line()
				&& symbol.equals(bc.impactedSymbol().getQualifiedName())
			).toList();

		if (matches.size() != 1) {
			String desc = "[%s, %s, %d]".formatted(symbol, kind, line);
			String found = bcs.stream()
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
				.collect(Collectors.joining(", "));
			throw new AssertionFailedError("No breaking change", desc, found);
		}
	}

	public static void assertNoBC(List<BreakingChange> bcs) {
		if (!bcs.isEmpty()) {
			String found = bcs.stream()
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
				.collect(Collectors.joining(", "));
			throw new AssertionFailedError("Unexpected breaking change", "No breaking change", found);
		}
	}

	public static void assertNoBC(BreakingChangeKind kind, List<BreakingChange> bcs) {
		String found = bcs.stream()
			.filter(bc -> bc.kind() == kind)
			.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
			.collect(Collectors.joining(", "));

		if (!found.isEmpty())
			throw new AssertionFailedError("Unexpected breaking change", "No breaking change", found);
	}

	public static void assertNoBC(int line, List<BreakingChange> bcs) {
		String found = bcs.stream()
			.filter(bc -> bc.impactedSymbol().getLocation().line() == line)
			.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
			.collect(Collectors.joining(", "));

		if (!found.isEmpty())
			throw new AssertionFailedError("Unexpected breaking change", "No breaking change", found);
	}

	public static TypeDecl assertType(API api, String name, String kind) {
		Optional<TypeDecl> findType = api.getLibraryTypes().findType(name);

		if (findType.isEmpty())
			throw new AssertionFailedError("No such type", kind + " " + name, "No such type");
		else {
			TypeDecl cls = findType.get();

			if ("class".equals(kind) && !cls.isClass())
				throw new AssertionFailedError("Wrong kind", "class " + name, cls.getClass().getSimpleName() + " " + name);
			if ("annotation".equals(kind) && !cls.isAnnotation())
				throw new AssertionFailedError("Wrong kind", "annotation " + name, cls.getClass().getSimpleName() + " " + name);
			if ("interface".equals(kind) && !cls.isInterface())
				throw new AssertionFailedError("Wrong kind", "interface " + name, cls.getClass().getSimpleName() + " " + name);
			if ("enum".equals(kind) && !cls.isEnum())
				throw new AssertionFailedError("Wrong kind", "enum " + name, cls.getClass().getSimpleName() + " " + name);
			if ("record".equals(kind) && !cls.isRecord())
				throw new AssertionFailedError("Wrong kind", "record " + name, cls.getClass().getSimpleName() + " " + name);

			return cls;
		}
	}

	public static void assertNoType(API api, String name) {
		Optional<TypeDecl> findType = api.getLibraryTypes().findType(name);

		if (findType.isPresent())
			throw new AssertionFailedError("Unexpected type", "No such type", findType.get().getQualifiedName());
	}

	public static FieldDecl assertField(API api, TypeDecl decl, String name) {
		Optional<FieldDecl> findField = api.findField(decl, name);

		if (findField.isEmpty())
			throw new AssertionFailedError("No such field", name, "No such field");
		else
			return findField.get();
	}

	public static void assertNoField(API api, TypeDecl decl, String name) {
		Optional<FieldDecl> findField = api.findField(decl, name);

		if (findField.isPresent())
			throw new AssertionFailedError("Unexpected field", "No such field", findField.get().getQualifiedName());
	}

	public static void assertNoMethod(API api, TypeDecl decl, String erasure) {
		Optional<MethodDecl> findMethod = api.findMethod(decl, erasure);

		if (findMethod.isPresent())
			throw new AssertionFailedError("Unexpected method", "No such method", api.getErasure(findMethod.get()));
	}

	public static void assertNoConstructor(API api, ClassDecl decl, String erasure) {
		Optional<ConstructorDecl> findCons = api.findConstructor(decl, erasure);

		if (findCons.isPresent())
			throw new AssertionFailedError("Unexpected constructor", "No such constructor", api.getErasure(findCons.get()));
	}

	public static MethodDecl assertMethod(API api, TypeDecl decl, String erasure) {
		List<MethodDecl> findMethod = decl.getDeclaredMethods().stream()
			.filter(m -> api.getErasure(m).equals(erasure))
			.toList();

		if (findMethod.isEmpty())
			throw new AssertionFailedError("No such method", erasure, "No such method");
		return findMethod.getFirst();
	}

	public static AnnotationMethodDecl assertAnnotationMethod(API api, AnnotationDecl decl, String erasure) {
		List<AnnotationMethodDecl> findMethod = decl.getAnnotationMethods().stream()
			.filter(m -> api.getErasure(m).equals(erasure))
			.toList();

		if (findMethod.isEmpty())
			throw new AssertionFailedError("No such method", erasure, "No such method");
		return findMethod.getFirst();
	}

	public static ConstructorDecl assertConstructor(API api, ClassDecl decl, String erasure) {
		List<ConstructorDecl> findCons = decl.getDeclaredConstructors().stream()
			.filter(m -> api.getErasure(m).equals(erasure))
			.toList();

		if (findCons.isEmpty())
			throw new AssertionFailedError("No such constructor", erasure, "No such constructor");
		return findCons.getFirst();
	}

	public static ClassDecl assertClass(API api, String name) {
		return (ClassDecl) assertType(api, name, "class");
	}

	public static InterfaceDecl assertInterface(API api, String name) {
		return (InterfaceDecl) assertType(api, name, "interface");
	}

	public static RecordDecl assertRecord(API api, String name) {
		return (RecordDecl) assertType(api, name, "record");
	}

	public static EnumDecl assertEnum(API api, String name) {
		return (EnumDecl) assertType(api, name, "enum");
	}

	public static AnnotationDecl assertAnnotation(API api, String name) {
		return (AnnotationDecl) assertType(api, name, "annotation");
	}

	public static CtModel buildModel(Map<String, String> sourcesMap) {
		Launcher launcher = new Launcher();

		sourcesMap.forEach((typeName, sources) -> {
			launcher.addInputResource(new VirtualFile(sources, typeName + ".java"));
		});
		launcher.getEnvironment().setComplianceLevel(17);
		launcher.getEnvironment().setLevel("TRACE");

		return launcher.buildModel();
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

	public static API buildSpoonAPI(String sources) {
		try {
			Map<String, String> sourcesMap = buildSourcesMap(sources);
			Path sourcesPath = writeSources(sourcesMap);
			Library library = Library.of(sourcesPath);
			LibraryTypes api = new SpoonTypesExtractor().extractTypes(library);
			MoreFiles.deleteRecursively(sourcesPath, RecursiveDeleteOption.ALLOW_INSECURE);
			return api.toAPI();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static API buildAsmAPI(String sources) {
		try {
			Map<String, String> sourcesMap = buildSourcesMap(sources);
			File tempJarFile = File.createTempFile("inMemoryJar", ".jar");
			tempJarFile.deleteOnExit();
			buildJar(sourcesMap, tempJarFile.toPath());
			Library library = Library.of(tempJarFile.toPath());
			return new AsmTypesExtractor().extractTypes(library).toAPI();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static API buildJdtAPI(String sources) {
		try {
			Map<String, String> sourcesMap = buildSourcesMap(sources);
			Path sourcesPath = writeSources(sourcesMap);
			Library library = Library.of(sourcesPath);
			LibraryTypes api = new JdtTypesExtractor().extractTypes(library);
			MoreFiles.deleteRecursively(sourcesPath, RecursiveDeleteOption.ALLOW_INSECURE);
			return api.toAPI();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Path writeSources(Map<String, String> sourcesMap) throws IOException {
		Path tempDir = Files.createTempDirectory("sources");
		for (Map.Entry<String, String> entry : sourcesMap.entrySet()) {
			Files.writeString(tempDir.resolve(entry.getKey() + ".java"), entry.getValue());
		}
		return tempDir;
	}

	public static List<BreakingChange> buildDiff(String sourcesV1, String sourcesV2) {
		APIDiff apiDiff = new APIDiff(buildSpoonAPI(sourcesV1), buildSpoonAPI(sourcesV2));
		var roseauBCs = apiDiff.diff();

		/*try {
			var jApiBCs = buildJApiCmpDiff(sourcesV1, sourcesV2);

			if (roseauBCs.size() != jApiBCs.size()) {
				System.out.printf("Roseau  found %d BCs: %s%n", roseauBCs.size(), roseauBCs);
				System.out.printf("JApiCmp found %d BCs: %s%n", jApiBCs.size(), jApiBCs);
			}
		} catch (Exception e) {
			System.out.println("JApiCmp comparison failed: " + e.getMessage());
		}*/

		return roseauBCs.breakingChanges();
	}

	public static List<JApiCompatibilityChange> buildJApiCmpDiff(String sourcesV1, String sourcesV2) {
		/*Map<String, String> sourcesMap1 = buildSourcesMap(sourcesV1);
		Map<String, String> sourcesMap2 = buildSourcesMap(sourcesV2);
		Path jar1 = Path.of(buildJar(sourcesMap1).getName());
		Path jar2 = Path.of(buildJar(sourcesMap2).getName());

		Options opts = Options.newDefault();
		opts.setOutputOnlyModifications(true);
		opts.setIgnoreMissingClasses(true);
		var comparatorOptions = JarArchiveComparatorOptions.of(opts);
		var jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
		var v1Archive = new JApiCmpArchive(jar1.toFile(), "1.0.0");
		var v2Archive = new JApiCmpArchive(jar2.toFile(), "2.0.0");
		List<JApiClass> jApiClasses = jarArchiveComparator.compare(v1Archive, v2Archive);
		List<JApiCompatibilityChange> bcs = new ArrayList<>();
		Filter.filter(jApiClasses, new Filter.FilterVisitor() {
			@Override public void visit(Iterator<JApiClass> iterator, JApiClass jApiClass) {
				bcs.addAll(jApiClass.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiMethod> iterator, JApiMethod jApiMethod) {
				bcs.addAll(jApiMethod.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiConstructor> iterator, JApiConstructor jApiConstructor) {
				bcs.addAll(jApiConstructor.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiImplementedInterface> iterator, JApiImplementedInterface jApiImplementedInterface) {
				bcs.addAll(jApiImplementedInterface.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiField> iterator, JApiField jApiField) {
				bcs.addAll(jApiField.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiAnnotation> iterator, JApiAnnotation jApiAnnotation) {
				bcs.addAll(jApiAnnotation.getCompatibilityChanges());
			}
			@Override public void visit(JApiSuperclass jApiSuperclass) {
				bcs.addAll(jApiSuperclass.getCompatibilityChanges());
			}
		});
		return bcs.stream().filter(bc -> !bc.isSourceCompatible() || !bc.isBinaryCompatible()).toList();*/
		return List.of();
	}

	public static JarFile buildJar(Map<String, String> sourcesMap, Path jar) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, null, null);
		MemoryJavaFileManager fileManager = new MemoryJavaFileManager(stdFileManager);

		List<JavaFileObject> compilationUnits = new ArrayList<>();
		for (Map.Entry<String, String> entry : sourcesMap.entrySet()) {
			String className = entry.getKey();
			String sourceCode = entry.getValue();
			compilationUnits.add(new MemorySourceJavaFileObject(className, sourceCode));
		}

		List<String> options = List.of(
			"-source", "21",
			"-target", "21",
			"-proc:none");
		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits);
		Boolean success = task.call();
		if (success == null || !success) {
			throw new IllegalStateException("Compilation failed.");
		}

		ByteArrayOutputStream jarByteStream = new ByteArrayOutputStream();
		try (JarOutputStream jarOut = new JarOutputStream(jarByteStream)) {
			for (Map.Entry<String, ByteArrayOutputStream> entry : fileManager.getCompiledClasses().entrySet()) {
				String className = entry.getKey();
				ByteArrayOutputStream classByteStream = entry.getValue();
				String entryName = className.replace('.', '/') + ".class";
				JarEntry jarEntry = new JarEntry(entryName);
				jarOut.putNextEntry(jarEntry);
				jarOut.write(classByteStream.toByteArray());
				jarOut.closeEntry();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while creating JAR file", e);
		}

		try {
			try (FileOutputStream fos = new FileOutputStream(jar.toFile())) {
				fos.write(jarByteStream.toByteArray());
			}
			return new JarFile(jar.toFile());
		} catch (IOException e) {
			throw new RuntimeException("Error while creating temporary JarFile", e);
		}
	}

	private static class MemoryJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
		private final Map<String, ByteArrayOutputStream> compiledClasses = new HashMap<>();

		protected MemoryJavaFileManager(StandardJavaFileManager fileManager) {
			super(fileManager);
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className,
		                                           JavaFileObject.Kind kind, FileObject sibling)
			throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			compiledClasses.put(className, baos);
			return new SimpleJavaFileObject(
				URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
				@Override
				public OutputStream openOutputStream() {
					return baos;
				}
			};
		}

		public Map<String, ByteArrayOutputStream> getCompiledClasses() {
			return compiledClasses;
		}
	}

	private static class MemorySourceJavaFileObject extends SimpleJavaFileObject {
		private final String sourceCode;

		public MemorySourceJavaFileObject(String className, String sourceCode) {
			super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
				Kind.SOURCE);
			this.sourceCode = sourceCode;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return sourceCode;
		}
	}
}
