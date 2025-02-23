package com.github.maracas.roseau.extractors.jdt;

import com.github.maracas.roseau.RoseauException;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.CachedTypeReferenceFactory;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import com.github.maracas.roseau.extractors.APIExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class JdtAPIExtractor implements APIExtractor {
	private static final Logger LOGGER = LogManager.getLogger(JdtAPIExtractor.class);

	@Override
	public API extractAPI(Path sources) {
		return extractAPI(sources, List.of());
	}

	public API extractAPI(Path sources, List<Path> classpath) {
		Objects.requireNonNull(classpath);
		try (Stream<Path> files = Files.walk(Objects.requireNonNull(sources))) {
			List<Path> sourceFiles = files
				.filter(this::isRegularJavaFile)
				.toList();

			TypeReferenceFactory typeRefFactory = new CachedTypeReferenceFactory();
			List<TypeDecl> parsedTypes = parseTypes(sourceFiles, sources, classpath, typeRefFactory);
			return new API(parsedTypes, typeRefFactory);
		} catch (IOException e) {
			throw new RoseauException("Failed to retrieve sources at " + sources, e);
		}
	}

	List<TypeDecl> parseTypes(List<Path> sourcesToParse, Path sourcesRoot, List<Path> classpath,
	                          TypeReferenceFactory typeRefFactory) {
		List<TypeDecl> typeDecls = new ArrayList<>(sourcesToParse.size());

		String[] sourcesArray = sourcesToParse.stream()
			.map(p -> p.toAbsolutePath().toString())
			.toArray(String[]::new);

		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_21);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_21);

		String[] sourcesRootArray = { sourcesRoot.toAbsolutePath().toString() };
		String[] classpathEntries = classpath.stream()
			.map(p -> p.toAbsolutePath().toString())
			.toArray(String[]::new);

		ASTParser parser = ASTParser.newParser(AST.JLS21);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		// Bindings recovery allows us to resolve incomplete bindings
		// e.g. 'A extends unknown.B' => allows us to obtain the FQN of A's superclass, which would be null otherwise
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(false);
		parser.setIgnoreMethodBodies(true);
		parser.setCompilerOptions(options);
		parser.setEnvironment(classpathEntries, sourcesRootArray, null, true);

		// Receive parsed ASTs and forward them to the visitor
		FileASTRequestor requestor = new FileASTRequestor() {
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				IProblem[] problems = ast.getProblems();
				if (problems != null) {
					// Actual parsing errors are just warnings for us
					Arrays.stream(problems)
						.filter(IProblem::isError)
						.forEach(p -> LOGGER.warn("{} [{}:{}]: {}", p.isError() ? "error" : "warning",
							sourceFilePath, p.getSourceLineNumber(), p.getMessage()));
				}

				JdtAPIVisitor visitor = new JdtAPIVisitor(ast, sourceFilePath, typeRefFactory);
				ast.accept(visitor);
				typeDecls.addAll(visitor.getCollectedTypeDecls());
			}
		};

		// Start parsing and forwarding ASTs
		try {
			parser.createASTs(sourcesArray, null, new String[0], requestor, null);
			return typeDecls;
		} catch (RuntimeException e) {
			// Catching JDT's internal messy errors
			throw new RoseauException("Failed to parse code from " + sourcesRoot, e);
		}
	}

	private boolean isRegularJavaFile(Path file) {
		return Files.isRegularFile(file) && file.toString().endsWith(".java") &&
			!file.endsWith("package-info.java") && !file.endsWith("module-info.java");
	}
}
