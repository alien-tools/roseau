package com.github.maracas.roseau.api;

import com.github.javaparser.ParseResult;
import com.github.javaparser.Position;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeReference;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Not fully implemented yet; half of the info isn't extracted
 */
public class JavaParserAPIExtractor implements APIExtractor {
	private final Path sources;

	public JavaParserAPIExtractor(Path sources) {
		this.sources = sources;
	}

	/**
	 * Extracts the library's (model's) structured API.
	 *
	 * @return Library's (model's) API.
	 */
	public API extractAPI() {
		ProjectRoot projectRoot =
			new ParserCollectionStrategy()
				.collect(sources);

		List<CompilationUnit> cus = new ArrayList<>();
		projectRoot.getSourceRoots().forEach(sourceRoot -> {
			try {
				List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();
				cus.addAll(parseResults.stream()
					.map(ParseResult::getResult)
					.flatMap(Optional::stream)
					.toList()
				);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		List<TypeDecl> allTypes = cus.stream()
			.flatMap(cu -> getAllTypes(cu).stream().map(this::convertTypeDeclaration))
			.toList();

		return new API(allTypes);
	}

	private List<TypeDeclaration<?>> getAllTypes(CompilationUnit cu) {
		return cu.getTypes().stream()
			.flatMap(type -> Stream.concat(
				Stream.of(type),
				getNestedTypes(type).stream()
			))
			.toList();
	}

	private List<TypeDeclaration<?>> getNestedTypes(TypeDeclaration<?> decl) {
		return decl.getMembers().stream()
			.filter(BodyDeclaration::isTypeDeclaration)
			.map(m -> (TypeDeclaration<?>) m)
			.flatMap(t -> Stream.concat(
				Stream.of(t),
				getNestedTypes(t).stream()
			))
			.toList();
	}

	private List<FieldDeclaration> getExportedFields(TypeDeclaration<?> type) {
		return type.getFields().stream()
			.filter(this::isExported)
			.toList();
	}

	private List<MethodDeclaration> getExportedMethods(TypeDeclaration<?> type) {
		return type.getMethods().stream()
			.filter(this::isExported)
			.toList();
	}

	private boolean isExported(NodeWithAccessModifiers<?> node) {
		return node.isPublic() || node.isProtected();
	}

	private TypeDecl convertTypeDeclaration(TypeDeclaration<?> type) {
		String qualifiedName = type.getFullyQualifiedName().orElse("<null>");
		AccessModifier visibility = convertAccessSpecifier(type.getAccessSpecifier());
		List<Modifier> modifiers = Collections.emptyList();
		List<FormalTypeParameter> formalTypeParameters = Collections.emptyList();
		TypeReference<TypeDecl> containingType = type.getParentNode().orElse(null) instanceof TypeDeclaration<?> parent
			? makeTypeReference(parent.getFullyQualifiedName().orElse("<null>"))
			: null;
		SourceLocation location = convertPosition(type.getBegin().orElse(null));
		List<TypeReference<InterfaceDecl>> superInterfaces = Collections.emptyList();
		List<FieldDecl> convertedFields = getExportedFields(type).stream()
			.map(this::convertFieldDeclaration)
			.toList();
		List<MethodDecl> convertedMethods = getExportedMethods(type).stream()
			.map(this::convertMethodDeclaration)
			.toList();

		return switch (type) {
			case AnnotationDeclaration a ->
				new AnnotationDecl(qualifiedName, visibility, modifiers, location, containingType, convertedFields, convertedMethods);
			case EnumDeclaration e -> {
				List<ConstructorDecl> convertedConstructors = e.getConstructors().stream()
					.map(this::convertConstructorDeclaration)
					.toList();
				yield new EnumDecl(qualifiedName, visibility, modifiers, location, containingType, superInterfaces, convertedFields, convertedMethods, convertedConstructors);
			}
			case RecordDeclaration r -> {
				List<ConstructorDecl> convertedConstructors = r.getConstructors().stream()
					.map(this::convertConstructorDeclaration)
					.toList();
				yield new RecordDecl(qualifiedName, visibility, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, convertedConstructors);
			}
			case ClassOrInterfaceDeclaration c -> {
				if (c.isInterface())
					yield new InterfaceDecl(qualifiedName, visibility, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods);
				else {
					TypeReference<ClassDecl> superClass = null;
					List<ConstructorDecl> convertedConstructors = c.getConstructors().stream()
						.map(this::convertConstructorDeclaration)
						.toList();
					yield new ClassDecl(qualifiedName, visibility, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, superClass, convertedConstructors);
				}
			}
			default -> throw new IllegalStateException("Unknown type kind: " + type);
		};
	}

	// Warning: JavaParser does not infer visibility but just returns the modifier
	// e.g., interface methods aren't implicitly public here
	private AccessModifier convertAccessSpecifier(AccessSpecifier specifier) {
		return switch (specifier) {
			case PUBLIC -> AccessModifier.PUBLIC;
			case PRIVATE -> AccessModifier.PRIVATE;
			case PROTECTED -> AccessModifier.PROTECTED;
			case NONE -> AccessModifier.PACKAGE_PRIVATE; // Wrong
		};
	}

	// Converts a field/method/constructor to a TypeMemberDecl
	private FieldDecl convertFieldDeclaration(FieldDeclaration field) {
		String qualifiedName = field.getVariables().get(0).getNameAsString();
		AccessModifier visibility = convertAccessSpecifier(field.getAccessSpecifier());
		List<Modifier> modifiers = Collections.emptyList();
		SourceLocation location = convertPosition(field.getBegin().orElse(null));
		TypeReference<TypeDecl> containingType = field.getParentNode().orElse(null) instanceof TypeDeclaration<?> parent
			? makeTypeReference(parent.getFullyQualifiedName().orElse("<null>"))
			: null;
		TypeReference<TypeDecl> type = null;

		return new FieldDecl(qualifiedName, visibility, modifiers, location, containingType, type);
	}

	private MethodDecl convertMethodDeclaration(MethodDeclaration method) {
		String qualifiedName = method.getNameAsString();
		AccessModifier visibility = convertAccessSpecifier(method.getAccessSpecifier());
		List<Modifier> modifiers = Collections.emptyList();
		SourceLocation location = convertPosition(method.getBegin().orElse(null));
		TypeReference<TypeDecl> containingType = method.getParentNode().orElse(null) instanceof TypeDeclaration<?> parent
			? makeTypeReference(parent.getFullyQualifiedName().orElse("<null>"))
			: null;
		TypeReference<TypeDecl> returnType = null;
		boolean isDefault = method.isDefault();
		boolean isAbstract = method.isAbstract();
		List<FormalTypeParameter> formalTypeParameters = Collections.emptyList();
		List<ParameterDecl> parameters = Collections.emptyList();
		List<TypeReference<ClassDecl>> exceptions = Collections.emptyList();

		return new MethodDecl(qualifiedName, visibility, modifiers, location, containingType, returnType, parameters, formalTypeParameters, exceptions);
	}

	private ConstructorDecl convertConstructorDeclaration(ConstructorDeclaration cons) {
		String qualifiedName = cons.getNameAsString();
		AccessModifier visibility = convertAccessSpecifier(cons.getAccessSpecifier());
		List<Modifier> modifiers = Collections.emptyList();
		SourceLocation location = convertPosition(cons.getBegin().orElse(null));
		TypeReference<TypeDecl> containingType = cons.getParentNode().orElse(null) instanceof TypeDeclaration<?> parent
			? makeTypeReference(parent.getFullyQualifiedName().orElse("<null>"))
			: null;
		TypeReference<TypeDecl> returnType = null;
		List<FormalTypeParameter> formalTypeParameters = Collections.emptyList();
		List<ParameterDecl> parameters = Collections.emptyList();
		List<TypeReference<ClassDecl>> exceptions = Collections.emptyList();

		return new ConstructorDecl(qualifiedName, visibility, modifiers, location, containingType, returnType, parameters, formalTypeParameters, exceptions);
	}

	private SourceLocation convertPosition(Position pos) {
		return pos == null
			? SourceLocation.NO_LOCATION
			: new SourceLocation(
				Path.of("<unknown>"),
				pos.line
			);
	}

	private boolean isExported(TypeDeclaration<?> type) {
		return
			(type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type)))
				&& (!type.isNestedType() || isExported((TypeDeclaration<?>) type.getParentNode().get()));
	}

	private boolean isEffectivelyFinal(TypeDeclaration<?> type) {
		return type.asClassOrInterfaceDeclaration().isFinal(); // No support for Java > 17 / sealed classes yet
	}

	private TypeReference<TypeDecl> makeTypeReference(String fqn) {
		return new TypeReference<>(fqn);
	}
}
