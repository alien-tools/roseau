package com.github.maracas.roseau.api;

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
import com.github.maracas.roseau.api.model.TypeMemberDecl;
import com.github.maracas.roseau.api.model.TypeReference;
import spoon.reflect.CtModel;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This class represents roseau's API extraction tool.
 * <br/>
 * Types are resolved within the universe of API types (exported or not).
 * We don't know anything about the outside world.
 */
public class SpoonAPIExtractor implements APIExtractor {
	private final CtModel model;

	/**
	 * Constructs an APIExtractor instance with the provided CtModel to extract its information.
	 */
	public SpoonAPIExtractor(CtModel model) {
		this.model = Objects.requireNonNull(model);
	}

	/**
	 * Extracts the library's (model's) structured API.
	 *
	 * @return Library's (model's) API.
	 */
	public API extractAPI() {
		List<TypeDecl> allTypes =
			model.getAllPackages().stream()
				.flatMap(p -> getAllTypes(p).stream().map(t -> convertCtType(t.getTypeDeclaration())))
				.toList();

		API api = new API(allTypes);

		// Within-library type resolution
		new TypeResolver(api).$(api).visit();

		return api;
	}

	// Returns all types within a package
	private List<CtTypeReference<?>> getAllTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.flatMap(type -> Stream.concat(
				Stream.of(type.getReference()),
				getNestedTypes(type).stream()
			))
			.toList();
	}

	// Returns (recursively) nested types within a type
	private List<CtTypeReference<?>> getNestedTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.flatMap(nestedType -> Stream.concat(
				Stream.of(nestedType.getReference()),
				getNestedTypes(nestedType).stream()
			))
			.toList();
	}

	// Returns all exported fields within a type
	private List<CtField<?>> getExportedFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns all exported methods within a type
	private List<CtMethod<?>> getExportedMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns all exported constructors within a class
	private List<CtConstructor<?>> getExportedConstructors(CtClass<?> cls) {
		return new ArrayList<>(cls.getConstructors().stream()
			.filter(this::isExported)
			.toList());
	}

	// Converts a CtType to a Type declaration
	private TypeDecl convertCtType(CtType<?> spoonType) {
		String qualifiedName = spoonType.getQualifiedName();
		AccessModifier visibility = convertSpoonVisibility(spoonType.getVisibility());
		boolean isExported = isExported(spoonType);
		List<Modifier> modifiers = convertSpoonNonAccessModifiers(spoonType.getModifiers());
		List<FormalTypeParameter> formalTypeParameters = spoonType.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();
		TypeReference<TypeDecl> containingType = spoonType.getDeclaringType() != null
			? makeTypeReference(spoonType.getDeclaringType().getReference())
			: null;
		SourceLocation location = convertSpoonPosition(spoonType.getPosition());
		List<TypeReference<InterfaceDecl>> superInterfaces = spoonType.getSuperInterfaces().stream()
			.map(this::makeInterfaceReference)
			.toList();
		List<FieldDecl> convertedFields = getExportedFields(spoonType).stream()
			.map(f -> (FieldDecl) convertCtTypeMember(f))
			.toList();
		List<MethodDecl> convertedMethods = getExportedMethods(spoonType).stream()
			.map(m -> (MethodDecl) convertCtTypeMember(m))
			.toList();

		return switch (spoonType) {
			case CtAnnotationType<?> a ->
				new AnnotationDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, convertedFields, convertedMethods);
			case CtInterface<?> i ->
				new InterfaceDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods);
			case CtClass<?> c -> {
				TypeReference<ClassDecl> superClass = spoonType.getSuperclass() != null
					? makeClassReference(spoonType.getSuperclass())
					: null;
				List<ConstructorDecl> convertedConstructors =
					getExportedConstructors(c).stream()
						.map(cons -> (ConstructorDecl) convertCtTypeMember(cons))
						.toList();

				yield switch (c) {
					case CtRecord r ->
						new RecordDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, convertedConstructors);
					case CtEnum<?> e ->
						new EnumDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, convertedFields, convertedMethods, convertedConstructors);
					case CtClass<?> cc ->
						new ClassDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, superClass, convertedConstructors);
				};
			}
			default -> throw new IllegalStateException("Unknown type kind: " + spoonType);
		};
	}

	// Converts a field/method/constructor to a TypeMemberDecl
	private TypeMemberDecl convertCtTypeMember(CtTypeMember spoonMember) {
		String qualifiedName = makeQualifiedName(spoonMember);
		AccessModifier visibility = convertSpoonVisibility(spoonMember.getVisibility());
		boolean isExported = isExported(spoonMember);
		List<Modifier> modifiers = convertSpoonNonAccessModifiers(spoonMember.getModifiers());
		SourceLocation location = convertSpoonPosition(spoonMember.getPosition());
		TypeReference<TypeDecl> containingType = makeTypeReference(spoonMember.getDeclaringType().getReference());

		return switch (spoonMember) {
			case CtField<?> spoonField -> {
				TypeReference<TypeDecl> type = makeTypeReference(spoonField.getType());
				yield new FieldDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, type);
			}
			case CtExecutable<?> spoonExecutable -> {
				TypeReference<TypeDecl> returnType = makeTypeReference(spoonExecutable.getType());
				List<ParameterDecl> parameters = spoonExecutable.getParameters().stream()
					.map(this::convertCtParameter)
					.toList();

				List<TypeReference<ClassDecl>> exceptions = spoonExecutable.getThrownTypes()
					.stream()
					.map(this::makeClassReference)
					.toList();

				switch (spoonExecutable) {
					case CtMethod<?> spoonMethod -> {
						boolean isDefault = spoonMethod.isDefaultMethod();
						boolean isAbstract = spoonMethod.isAbstract();
						List<FormalTypeParameter> formalTypeParameters = spoonMethod.getFormalCtTypeParameters().stream()
							.map(this::convertCtTypeParameter)
							.toList();

						yield new MethodDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, returnType, parameters, formalTypeParameters, exceptions, isDefault, isAbstract);
					}
					case CtConstructor<?> spoonCons -> {
						List<FormalTypeParameter> formalTypeParameters = spoonCons.getFormalCtTypeParameters().stream()
							.map(this::convertCtTypeParameter)
							.toList();

						yield new ConstructorDecl(qualifiedName, visibility, isExported, modifiers, location, containingType, returnType, parameters, formalTypeParameters, exceptions);
					}
					default -> throw new IllegalArgumentException("Unknown member type " + spoonExecutable);
				}
			}
			default -> throw new IllegalArgumentException("Unknown member type " + spoonMember);
		};
	}

	private ParameterDecl convertCtParameter(CtParameter<?> parameter) {
		return new ParameterDecl(parameter.getSimpleName(), makeTypeReference(parameter.getType()), parameter.isVarArgs());
	}

	private FormalTypeParameter convertCtTypeParameter(CtTypeParameter parameter) {
		return new FormalTypeParameter(
			parameter.getSimpleName(),
			parameter.getSuperInterfaces().stream()
				.map(this::makeTypeReference)
				.toList()
		);
	}

	// Converts Spoon's access-specific ModifierKind to Roseau's AccessModifier
	private AccessModifier convertSpoonVisibility(ModifierKind visibility) {
		return switch (visibility) {
			case PUBLIC    -> AccessModifier.PUBLIC;
			case PRIVATE   -> AccessModifier.PRIVATE;
			case PROTECTED -> AccessModifier.PROTECTED;
			case null      -> AccessModifier.PACKAGE_PRIVATE;
			default        -> throw new IllegalArgumentException("Unknown visibility " + visibility);
		};
	}

	// Converts Spoon's non-access ModifierKind to Roseau's Modifier
	private Modifier convertSpoonModifier(ModifierKind modifier) {
		return switch (modifier) {
			case STATIC       -> Modifier.STATIC;
			case FINAL        -> Modifier.FINAL;
			case ABSTRACT     -> Modifier.ABSTRACT;
			case SYNCHRONIZED -> Modifier.SYNCHRONIZED;
			case VOLATILE     -> Modifier.VOLATILE;
			case TRANSIENT    -> Modifier.TRANSIENT;
			case SEALED       -> Modifier.SEALED;
			case NON_SEALED   -> Modifier.NON_SEALED;
			case NATIVE       -> Modifier.NATIVE;
			case STRICTFP     -> Modifier.STRICTFP;
			default           -> throw new IllegalArgumentException("Unknown modifier " + modifier);
		};
	}

	// Filtering access modifiers because the convertVisibility() handles them already
	private List<Modifier> convertSpoonNonAccessModifiers(Set<ModifierKind> modifiers) {
		return modifiers.stream()
			.filter(mod ->
				   mod != ModifierKind.PUBLIC
				&& mod != ModifierKind.PROTECTED
				&& mod != ModifierKind.PRIVATE)
			.map(this::convertSpoonModifier)
			.toList();
	}

	private SourceLocation convertSpoonPosition(SourcePosition position) {
		return !position.isValidPosition()
			? SourceLocation.NO_LOCATION
			: new SourceLocation(
				position.getFile() != null ? position.getFile().toPath() : Path.of("<unknown>"),
				position.getLine()
			);
	}

	private boolean isExported(CtType<?> type) {
		return
			   (type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type)))
			&& (type.getDeclaringType() == null || isExported(type.getDeclaringType()));
	}

	private boolean isExported(CtTypeMember member) {
		return member.isPublic() || member.isProtected();
	}

	private boolean isEffectivelyFinal(CtType<?> type) {
		return type.isFinal() || type.hasModifier(ModifierKind.SEALED);
	}

	private TypeReference<TypeDecl> makeTypeReference(CtTypeReference<?> spoonType) {
		return new TypeReference<>(spoonType.getQualifiedName());
	}

	private TypeReference<ClassDecl> makeClassReference(CtTypeReference<?> spoonType) {
		return new TypeReference<>(spoonType.getQualifiedName());
	}

	private TypeReference<InterfaceDecl> makeInterfaceReference(CtTypeReference<?> spoonType) {
		return new TypeReference<>(spoonType.getQualifiedName());
	}

	private String makeQualifiedName(CtTypeMember member) {
		return String.format("%s.%s", member.getDeclaringType().getQualifiedName(), member.getSimpleName());
	}
}
