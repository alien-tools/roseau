package com.github.maracas.roseau.extractors.jdt;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class JdtAPIExtractor implements APIExtractor {
	private static final Logger LOGGER = LogManager.getLogger(JdtAPIExtractor.class);

	@Override
	public API extractAPI(Path sources) {
		try (Stream<Path> files = Files.walk(sources)) {
			List<Path> sourceFiles = files
				.filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".java"))
				.toList();

			TypeReferenceFactory typeRefFactory = new CachedTypeReferenceFactory();
			List<TypeDecl> parsedTypes = parseTypes(sourceFiles, sources, typeRefFactory);
			return new API(parsedTypes, typeRefFactory);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	List<TypeDecl> parseTypes(List<Path> sourcesToParse, Path sourcesRoot, TypeReferenceFactory typeRefFactory) {
		List<TypeDecl> typeDecls = new ArrayList<>();

		String[] sourcesArray = sourcesToParse.stream()
			.map(p -> p.toAbsolutePath().toString())
			.toArray(String[]::new);

		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_21);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_21);

		String[] sourcesRootArray = { sourcesRoot.toAbsolutePath().toString() };

		ASTParser parser = ASTParser.newParser(AST.JLS21);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(false);
		parser.setStatementsRecovery(false);
		parser.setIgnoreMethodBodies(true);
		parser.setCompilerOptions(options);
		parser.setEnvironment(null, sourcesRootArray, null, true);

		// Receive parsed ASTs and forward them to the visitor
		FileASTRequestor requestor = new FileASTRequestor() {
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				IProblem[] problems = ast.getProblems();
				if (problems != null) {
					for (IProblem problem : problems) {
						LOGGER.warn("Error [{}:{}]: {}", sourceFilePath, problem.getSourceLineNumber(), problem.getMessage());
					}
				}

				JdtAPIVisitor visitor = new JdtAPIVisitor(ast, sourceFilePath, typeRefFactory);
				ast.accept(visitor);
				typeDecls.addAll(visitor.getCollectedTypeDecls());
			}
		};

		// Start parsing and forwarding ASTs
		parser.createASTs(sourcesArray, null, new String[0], requestor, null);

		return typeDecls;
	}
}
