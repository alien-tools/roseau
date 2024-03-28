package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.google.common.base.Stopwatch;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JDTAPIExtractor {
	public static void main(String[] args) throws Exception {
		// Path to the directory containing Java source files
		//Path srcPath = Paths.get("/home/dig/repositories/roseau/src/main/java");
		Path srcPath = Paths.get("/home/dig/jdk/17");

		// Create an ASTParser
		Stopwatch sw = Stopwatch.createStarted();
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setIgnoreMethodBodies(true);

		// List to store contents of all Java files
		List<char[]> javaFilesContent = new ArrayList<>();

		// Read all Java files in the directory
		Files.walk(srcPath)
			.filter(Files::isRegularFile)
			.filter(path -> path.toString().endsWith(".java"))
			.forEach(path -> {
				try {
					String content = new String(Files.readAllBytes(path));
					javaFilesContent.add(content.toCharArray());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});

		// Parse each Java file content
		for (char[] content : javaFilesContent) {
			parser.setSource(content);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			cu.accept(new ASTVisitor() {
				@Override
				public boolean visit(TypeDeclaration node) {
					String typeName = node.getName().getIdentifier();
					//System.out.println("Type: " + typeName);
					return true; // Visit children to get methods
				}

				@Override
				public boolean visit(EnumDeclaration node) {
					String typeName = node.getName().getIdentifier();
					//System.out.println("Enum: " + typeName);
					return true; // Visit children to get methods
				}

				@Override
				public boolean visit(RecordDeclaration node) {
					String typeName = node.getName().getIdentifier();
					//System.out.println("Record: " + typeName);
					return true; // Visit children to get methods
				}

				@Override
				public boolean visit(AnnotationTypeDeclaration node) {
					String typeName = node.getName().getIdentifier();
					//System.out.println("Annotation: " + typeName);
					return true; // Visit children to get methods
				}

				@Override
				public boolean visit(MethodDeclaration node) {
					String methodName = node.getName().getIdentifier();
					// Create method signature
					StringBuilder signature = new StringBuilder(node.getReturnType2() == null ? "void" : node.getReturnType2().toString());
					signature.append(" ").append(methodName).append("(");
					for (int i = 0; i < node.parameters().size(); i++) {
						if (i > 0) signature.append(", ");
						signature.append(node.parameters().get(i).toString());
					}
					signature.append(")");
					//System.out.println("Method: " + signature);
					return false; // No need to visit children (method body)
				}

				@Override
				public boolean visit(FieldDeclaration node) {
					return false; // No need to visit children (method body)
				}
			});
		}

		System.out.println("Took: " + sw.elapsed(TimeUnit.MILLISECONDS));
	}

	private InterfaceDecl convertTypeDeclaration(TypeDeclaration t) {
		if (t.isInterface()) {
			ITypeBinding binding = t.resolveBinding();
			return new InterfaceDecl(
				binding.getQualifiedName(),
				convertAccessModifier(t.getModifiers()),
				convertModifiers(t.modifiers()),
				Collections.emptyList(), // FIXME
				null,
				null,
				null,
				null,
				null,
				t.isLocalTypeDeclaration() ? null : null, // FIXME
			);
		}

		return null;
	}

	private AccessModifier convertAccessModifier(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return AccessModifier.PUBLIC;
		if (Modifier.isProtected(modifiers))
			return AccessModifier.PROTECTED;
		if (Modifier.isPrivate(modifiers))
			return AccessModifier.PRIVATE;
		else return AccessModifier.PACKAGE_PRIVATE;
	}

	private List<com.github.maracas.roseau.api.model.Modifier> convertModifiers(List<Modifier> modifiers) {
		return modifiers.stream()
			.filter(m -> !m.isPublic() && !m.isPrivate() && ! m.isProtected())
			.map(this::convertModifier)
			.toList();
	}

	private com.github.maracas.roseau.api.model.Modifier convertModifier(Modifier modifier) {
		if (modifier.isAbstract())
			return com.github.maracas.roseau.api.model.Modifier.ABSTRACT;
		if (modifier.isDefault())
			return com.github.maracas.roseau.api.model.Modifier.DEFAULT;
		if (modifier.isFinal())
			return com.github.maracas.roseau.api.model.Modifier.FINAL;
		if (modifier.isNative())
			return com.github.maracas.roseau.api.model.Modifier.NATIVE;
		if (modifier.isNonSealed())
			return com.github.maracas.roseau.api.model.Modifier.NON_SEALED;
		if (modifier.isSealed())
			return com.github.maracas.roseau.api.model.Modifier.SEALED;
		if (modifier.isStatic())
			return com.github.maracas.roseau.api.model.Modifier.STATIC;
		if (modifier.isStrictfp())
			return com.github.maracas.roseau.api.model.Modifier.STRICTFP;
		if (modifier.isTransient())
			return com.github.maracas.roseau.api.model.Modifier.TRANSIENT;
		if (modifier.isVolatile())
			return com.github.maracas.roseau.api.model.Modifier.VOLATILE;
		if (modifier.isSynchronized())
			return com.github.maracas.roseau.api.model.Modifier.SYNCHRONIZED;
		throw new RuntimeException("Unexpected modifier: " + modifier);
	}
}
