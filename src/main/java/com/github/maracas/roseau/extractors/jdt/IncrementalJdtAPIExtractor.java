package com.github.maracas.roseau.extractors.jdt;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.CachedTypeReferenceFactory;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import com.github.maracas.roseau.extractors.ChangedFilesProvider;
import com.github.maracas.roseau.extractors.IncrementalAPIExtractor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class IncrementalJdtAPIExtractor implements IncrementalAPIExtractor {
	@Override
	public API refreshAPI(Path sources, ChangedFilesProvider.ChangedFiles changedFiles, API previousApi) {
		TypeReferenceFactory typeRefFactory = new CachedTypeReferenceFactory();

		List<TypeDecl> typeDecls = new ArrayList<>();
		previousApi.getAllTypes().forEach(d -> {
			Path source = d.getLocation().file();
			if (!(changedFiles.deletedFiles().contains(source) || changedFiles.updatedFiles().contains(source)))
				typeDecls.add(d);
		});

		// 2. Prepare an array of absolute paths (as Strings) for the ASTParser
		String[] sourceFilePaths = Stream.concat(changedFiles.updatedFiles().stream(), changedFiles.createdFiles().stream())
			.map(p -> p.toAbsolutePath().toString())
			.toArray(String[]::new);

		// 3. Set up the ASTParser with the desired language level (JLS17 here) and enable bindings.
		ASTParser parser = ASTParser.newParser(AST.JLS21);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(false);
		parser.setIgnoreMethodBodies(true);

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
//				System.out.println("Parsed file: " + sourceFilePath);
//				IProblem[] problems = ast.getProblems();
//				if (problems != null && problems.length > 0) {
//					for (IProblem problem : problems) {
//						String severity = problem.isError() ? "Error" : "Warning";
//						System.out.printf("%s: %s at line %d%n", severity,
//							problem.getMessage(), problem.getSourceLineNumber());
//					}
//				} else {
//					System.out.println("No issues in " + sourceFilePath);
//				}
				JdtAPIVisitor visitor = new JdtAPIVisitor(ast, sourceFilePath, typeRefFactory);
				ast.accept(visitor);
				typeDecls.addAll(visitor.getCollectedTypeDecls());
			}
		};

		// 7. Parse all Java files. The third argument (binding keys) is empty here.
		parser.createASTs(sourceFilePaths, null, new String[0], requestor, null);

		return new API(typeDecls, typeRefFactory);
	}
}
