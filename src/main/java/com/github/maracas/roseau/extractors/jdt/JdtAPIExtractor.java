package com.github.maracas.roseau.extractors.jdt;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.CachedTypeReferenceFactory;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.extractors.APIExtractor;
import com.github.maracas.roseau.extractors.sources.SpoonAPIExtractor;
import com.google.common.base.Stopwatch;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdtAPIExtractor implements APIExtractor {
	public static void main(String[] args) {
		var sw = Stopwatch.createUnstarted();
		var sources = Path.of("src/main/java");
//		for (int i = 0; i < 5; i++) {
			sw.reset().start();
			var api1 = new JdtAPIExtractor().extractAPI(sources);
			System.out.printf("JDT took %dms (%d types)%n", sw.elapsed().toMillis(), api1.getAllTypes().count());
			sw.reset().start();
			var api2 = new SpoonAPIExtractor().extractAPI(sources);
			System.out.printf("Spoon took %dms (%d types)%n", sw.elapsed().toMillis(), api2.getAllTypes().count());
			var bcs = new APIDiff(api1, api2).diff();
			System.out.println(bcs.size());
			try {
				api1.writeJson(Path.of("jdt.json"));
				api2.writeJson(Path.of("spoon.json"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
//		}
	}

	@Override
	public API extractAPI(Path sources) {
		TypeReferenceFactory typeRefFactory = new CachedTypeReferenceFactory();
		List<TypeDecl> typeDecls = new ArrayList<>();

		try {
			// 1. Recursively collect all .java files
			List<Path> javaFiles = Files.walk(sources)
				.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
				.toList();

			// 2. Prepare an array of absolute paths (as Strings) for the ASTParser
			String[] sourceFilePaths = javaFiles.stream()
				.map(p -> p.toAbsolutePath().toString())
				.toArray(String[]::new);

			// 3. Set up the ASTParser with the desired language level (JLS17 here) and enable bindings.
			ASTParser parser = ASTParser.newParser(AST.JLS21);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setResolveBindings(true);
			parser.setBindingsRecovery(true);

			// 4. Set compiler options (adjust the version as needed)
			Map<String, String> options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_21);
			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_21);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_21);
			parser.setCompilerOptions(options);

			// 5. Set the environment for binding resolution.
			//    The source path is the given source directory.
			String[] sourcePathEntries = new String[]{sources.toAbsolutePath().toString()};
			parser.setEnvironment(null, sourcePathEntries, null, true);

			// 6. Create a FileASTRequestor to receive the CompilationUnits as they are parsed.
			FileASTRequestor requestor = new FileASTRequestor() {
				@Override
				public void acceptAST(String sourceFilePath, CompilationUnit ast) {
//					System.out.println("Parsed file: " + sourceFilePath);
//					IProblem[] problems = ast.getProblems();
//					if (problems != null && problems.length > 0) {
//						for (IProblem problem : problems) {
//							String severity = problem.isError() ? "Error" : "Warning";
//							System.out.printf("%s: %s at line %d%n", severity,
//								problem.getMessage(), problem.getSourceLineNumber());
//						}
//					} else {
//						System.out.println("No issues in " + sourceFilePath);
//					}
					JdtAPIVisitor visitor = new JdtAPIVisitor(ast, sourceFilePath, typeRefFactory);
					ast.accept(visitor);
					typeDecls.addAll(visitor.getCollectedTypeDecls());
				}
			};

			// 7. Parse all Java files. The third argument (binding keys) is empty here.
			parser.createASTs(sourceFilePaths, null, new String[0], requestor, null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new API(typeDecls, typeRefFactory);
	}
}
