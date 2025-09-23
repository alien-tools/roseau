package io.github.alien.roseau.extractors.jdt;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.extractors.TypesExtractor;
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
import java.util.stream.Stream;

/**
 * A JDT-based {@link TypesExtractor}.
 */
public class JdtTypesExtractor implements TypesExtractor {
	private static final Logger LOGGER = LogManager.getLogger(JdtTypesExtractor.class);

	@Override
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(canExtract(library));
		try (Stream<Path> files = Files.walk(library.getLocation())) {
			List<Path> sourceFiles = files
				.filter(JdtTypesExtractor::isRegularJavaFile)
				.toList();

			TypeReferenceFactory typeRefFactory = new CachingTypeReferenceFactory();
			List<TypeDecl> parsedTypes = parseTypes(library, sourceFiles, typeRefFactory);
			return new LibraryTypes(library, ModuleDecl.UNNAMED_MODULE, parsedTypes);
		} catch (IOException e) {
			throw new RoseauException("Failed to retrieve sources at " + library.getLocation(), e);
		}
	}

	@Override
	public boolean canExtract(Library library) {
		return library != null && library.isSources();
	}

	List<TypeDecl> parseTypes(Library library, List<Path> sourcesToParse, TypeReferenceFactory typeRefFactory) {
		String[] sourcesArray = sourcesToParse.stream()
			.map(p -> p.toAbsolutePath().toString())
			.toArray(String[]::new);

		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_21);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_21);

		String[] sourcesRootArray = { library.getLocation().toAbsolutePath().toString() };
		String[] classpathEntries = library.getClasspath().stream()
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
		List<TypeDecl> typeDecls = new ArrayList<>(sourcesToParse.size());
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
			throw new RoseauException("Failed to parse code from " + library.getLocation(), e);
		}
	}

	private static boolean isRegularJavaFile(Path file) {
		return Files.isRegularFile(file) && file.toString().endsWith(".java") &&
			!file.endsWith("package-info.java") && !file.endsWith("module-info.java");
	}
}
