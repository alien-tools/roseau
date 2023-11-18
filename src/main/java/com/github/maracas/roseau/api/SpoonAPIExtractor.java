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
import com.github.maracas.roseau.api.model.TypeReference;
import spoon.reflect.CtModel;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
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
import java.util.Collection;
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
				.flatMap(p -> getAllTypes(p).stream().map(this::convertCtType))
				.toList();

		API api = new API(allTypes);

		// Within-library type resolution
		new TypeResolver(api).$(api).visit();

		return api;
	}

	// Returns all types within a package
	private List<CtType<?>> getAllTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.flatMap(type -> Stream.concat(
				Stream.of(type),
				getNestedTypes(type).stream()
			))
			.toList();
	}

	// Returns (recursively) nested types within a type
	private List<CtType<?>> getNestedTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.flatMap(nestedType -> Stream.concat(
				Stream.of(nestedType),
				getNestedTypes(nestedType).stream()
			))
			.toList();
	}

	private TypeDecl convertCtType(CtType<?> type) {
		return switch (type) {
			case CtAnnotationType<?> a -> convertCtAnnotationType(a);
			case CtInterface<?> i      -> convertCtInterface(i);
			case CtRecord r            -> convertCtRecord(r);
			case CtEnum<?> e           -> convertCtEnum(e);
			case CtClass<?> c          -> convertCtClass(c);
			default -> throw new IllegalArgumentException("Unknown type kind: " + type);
		};
	}

	private ClassDecl convertCtClass(CtClass<?> cls) {
		return new ClassDecl(
			cls.getQualifiedName(),
			convertSpoonVisibility(cls.getVisibility()),
			isExported(cls),
			convertSpoonNonAccessModifiers(cls.getModifiers()),
			convertSpoonPosition(cls.getPosition()),
			makeTypeReference(cls.getDeclaringType()),
			makeTypeReferences(cls.getSuperInterfaces()),
			convertCtFormalTypeParameters(cls),
			convertCtFields(cls),
			convertCtMethods(cls),
			makeTypeReference(cls.getSuperclass()),
			convertCtConstructors(cls)
		);
	}

	private InterfaceDecl convertCtInterface(CtInterface<?> intf) {
		return new InterfaceDecl(
			intf.getQualifiedName(),
			convertSpoonVisibility(intf.getVisibility()),
			isExported(intf),
			convertSpoonNonAccessModifiers(intf.getModifiers()),
			convertSpoonPosition(intf.getPosition()),
			makeTypeReference(intf.getDeclaringType()),
			makeTypeReferences(intf.getSuperInterfaces()),
			convertCtFormalTypeParameters(intf),
			convertCtFields(intf),
			convertCtMethods(intf)
		);
	}

	private AnnotationDecl convertCtAnnotationType(CtAnnotationType<?> annotation) {
		return new AnnotationDecl(
			annotation.getQualifiedName(),
			convertSpoonVisibility(annotation.getVisibility()),
			isExported(annotation),
			convertSpoonNonAccessModifiers(annotation.getModifiers()),
			convertSpoonPosition(annotation.getPosition()),
			makeTypeReference(annotation.getDeclaringType()),
			convertCtFields(annotation),
			convertCtMethods(annotation)
		);
	}

	private EnumDecl convertCtEnum(CtEnum<?> enm) {
		return new EnumDecl(
			enm.getQualifiedName(),
			convertSpoonVisibility(enm.getVisibility()),
			isExported(enm),
			convertSpoonNonAccessModifiers(enm.getModifiers()),
			convertSpoonPosition(enm.getPosition()),
			makeTypeReference(enm.getDeclaringType()),
			makeTypeReferences(enm.getSuperInterfaces()),
			convertCtFields(enm),
			convertCtMethods(enm),
			convertCtConstructors(enm)
		);
	}

	private RecordDecl convertCtRecord(CtRecord record) {
		return new RecordDecl(
			record.getQualifiedName(),
			convertSpoonVisibility(record.getVisibility()),
			isExported(record),
			convertSpoonNonAccessModifiers(record.getModifiers()),
			convertSpoonPosition(record.getPosition()),
			makeTypeReference(record.getDeclaringType()),
			makeTypeReferences(record.getSuperInterfaces()),
			convertCtFormalTypeParameters(record),
			convertCtFields(record),
			convertCtMethods(record),
			convertCtConstructors(record)
		);
	}

	private FieldDecl convertCtField(CtField<?> field) {
		return new FieldDecl(
			makeQualifiedName(field),
			convertSpoonVisibility(field.getVisibility()),
			isExported(field),
			convertSpoonNonAccessModifiers(field.getModifiers()),
			convertSpoonPosition(field.getPosition()),
			makeTypeReference(field.getDeclaringType()),
			makeTypeReference(field.getType())
		);
	}

	private MethodDecl convertCtMethod(CtMethod<?> method) {
		return new MethodDecl(
			makeQualifiedName(method),
			convertSpoonVisibility(method.getVisibility()),
			isExported(method),
			convertSpoonNonAccessModifiers(method.getModifiers()),
			convertSpoonPosition(method.getPosition()),
			makeTypeReference(method.getDeclaringType()),
			makeTypeReference(method.getType()),
			convertCtParameters(method),
			convertCtFormalTypeParameters(method),
			makeTypeReferences(new ArrayList<>(method.getThrownTypes())),
			method.isDefaultMethod(),
			method.isAbstract()
		);
	}

	private ConstructorDecl convertCtConstructor(CtConstructor<?> cons) {
		return new ConstructorDecl(
			makeQualifiedName(cons),
			convertSpoonVisibility(cons.getVisibility()),
			isExported(cons),
			convertSpoonNonAccessModifiers(cons.getModifiers()),
			convertSpoonPosition(cons.getPosition()),
			makeTypeReference(cons.getDeclaringType()),
			makeTypeReference(cons.getType()),
			convertCtParameters(cons),
			convertCtFormalTypeParameters(cons),
			makeTypeReferences(new ArrayList<>(cons.getThrownTypes()))
		);
	}

	private List<FieldDecl> convertCtFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(this::isExported)
			.map(this::convertCtField)
			.toList();
	}

	private List<MethodDecl> convertCtMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(this::isExported)
			.map(this::convertCtMethod)
			.toList();
	}

	private List<ConstructorDecl> convertCtConstructors(CtClass<?> cls) {
		return cls.getConstructors().stream()
			.filter(this::isExported)
			.map(this::convertCtConstructor)
			.toList();
	}

	private List<FormalTypeParameter> convertCtFormalTypeParameters(CtFormalTypeDeclarer declarer) {
		return declarer.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();
	}

	private FormalTypeParameter convertCtTypeParameter(CtTypeParameter parameter) {
		return new FormalTypeParameter(
			parameter.getSimpleName(),
			parameter.getSuperInterfaces().stream()
				.map(this::makeTypeReference)
				.toList()
		);
	}

	private List<ParameterDecl> convertCtParameters(CtExecutable<?> executable) {
		return executable.getParameters().stream()
			.map(this::convertCtParameter)
			.toList();
	}

	private ParameterDecl convertCtParameter(CtParameter<?> parameter) {
		return new ParameterDecl(parameter.getSimpleName(), makeTypeReference(parameter.getType()), parameter.isVarArgs());
	}

	private AccessModifier convertSpoonVisibility(ModifierKind visibility) {
		return switch (visibility) {
			case PUBLIC    -> AccessModifier.PUBLIC;
			case PRIVATE   -> AccessModifier.PRIVATE;
			case PROTECTED -> AccessModifier.PROTECTED;
			case null      -> AccessModifier.PACKAGE_PRIVATE;
			default        -> throw new IllegalArgumentException("Unknown visibility " + visibility);
		};
	}

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

	private <T extends TypeDecl> TypeReference<T> makeTypeReference(CtTypeReference<?> typeRef) {
		return typeRef != null ? new TypeReference<>(typeRef.getQualifiedName()) : null;
	}

	private <T extends TypeDecl> TypeReference<T> makeTypeReference(CtType<?> type) {
		return type != null ? new TypeReference<>(type.getQualifiedName()) : null;
	}

	private <T extends TypeDecl> List<TypeReference<T>> makeTypeReferences(Collection<CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::<T>makeTypeReference)
			.filter(Objects::nonNull)
			.toList();
	}

	private String makeQualifiedName(CtTypeMember member) {
		return String.format("%s.%s", member.getDeclaringType().getQualifiedName(), member.getSimpleName());
	}
}
