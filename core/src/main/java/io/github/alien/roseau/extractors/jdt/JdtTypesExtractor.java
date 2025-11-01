package io.github.alien.roseau.extractors.jdt;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.extractors.ExtractorSink;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A JDT-based {@link TypesExtractor}.
 */
public final class JdtTypesExtractor implements TypesExtractor {
	private final ApiFactory factory;

	private static final Logger LOGGER = LogManager.getLogger(JdtTypesExtractor.class);

	record ParsingResult(Set<TypeDecl> types, Set<ModuleDecl> modules) {
	}

	public JdtTypesExtractor(ApiFactory factory) {
		this.factory = Preconditions.checkNotNull(factory);
	}

	@Override
	public LibraryTypes extractTypes(Library library) {
		Preconditions.checkArgument(canExtract(library));
		try (Stream<Path> files = Files.walk(library.getLocation())) {
			Set<Path> sourceFiles = files
				.filter(JdtTypesExtractor::isRegularJavaFile)
				.collect(Collectors.toSet());

			ParsingResult result = parseTypes(library, sourceFiles);
			Set<TypeDecl> types = result.types();
			Set<ModuleDecl> modules = result.modules();

			return switch (modules.size()) {
				case 0 -> new LibraryTypes(library, types);
				case 1 -> new LibraryTypes(library, modules.iterator().next(), result.types());
				default -> throw new RoseauException("%s contains multiple module declarations: %s".formatted(library, modules));
			};
		} catch (IOException e) {
			throw new RoseauException("Failed to parse sources", e);
		}
	}

	ParsingResult parseTypes(Library library, Set<Path> sourcesToParse) {
		String[] sourcesArray = sourcesToParse.stream()
			.map(Path::toString)
			.toArray(String[]::new);

		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_21);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_21);

		String[] sourcesRootArray = {library.getLocation().toAbsolutePath().toString()};
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

		ExtractorSink sink = new ExtractorSink(sourcesToParse.size() << 1);
		// Receive parsed ASTs and forward them to the visitor
		FileASTRequestor requestor = new FileASTRequestor() {
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				Path filePath = library.getLocation().relativize(Path.of(sourceFilePath));
				IProblem[] problems = ast.getProblems();
				if (problems != null) {
					// Actual parsing errors are just warnings for us
					Arrays.stream(problems)
						.filter(IProblem::isError)
						.forEach(p -> LOGGER.warn("JDT error [{}:{}]: {}", filePath, p.getSourceLineNumber(), p.getMessage()));
				}

				JdtApiVisitor visitor = new JdtApiVisitor(ast, filePath, sink, factory);
				ast.accept(visitor);
			}
		};

		// Start parsing and forwarding ASTs
		try {
			parser.createASTs(sourcesArray, null, new String[0], requestor, null);
			return new ParsingResult(sink.getTypes(), sink.getModules());
		} catch (RuntimeException e) {
			// Catching JDT's internal messy errors
			throw new RoseauException("JDT failed to parse code from " + library.getLocation(), e);
		}
	}

	private static boolean canExtract(Library library) {
		return library != null && library.isSources();
	}

	private static boolean isRegularJavaFile(Path file) {
		return Files.isRegularFile(file) && file.toString().endsWith(".java") && !file.endsWith("package-info.java");
	}
}
