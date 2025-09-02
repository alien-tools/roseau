package io.github.alien.roseau.extractors.spoon;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spoon.Launcher;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtRecordComponent;
import spoon.reflect.declaration.CtSealable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtIntersectionTypeReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A factory of {@link TypeDecl} and {@link TypeReference} instances using Spoon. For internal use only.
 */
public class SpoonAPIFactory {
	private final TypeFactory typeFactory;
	private final TypeReferenceFactory typeReferenceFactory;

	private static final Logger LOGGER = LogManager.getLogger(SpoonAPIFactory.class);

	public SpoonAPIFactory(TypeReferenceFactory typeReferenceFactory, List<Path> classpath) {
		Factory spoonFactory = new Launcher().createFactory();
		spoonFactory.getEnvironment().setSourceClasspath(
			sanitizeClasspath(classpath).stream()
				.map(p -> p.toAbsolutePath().toString())
				.toArray(String[]::new));
		this.typeFactory = spoonFactory.Type();
		this.typeReferenceFactory = typeReferenceFactory;
	}

	public TypeReferenceFactory getTypeReferenceFactory() {
		return typeReferenceFactory;
	}

	// Avoid having Spoon throwing at us due to "invalid" classpath
	private List<Path> sanitizeClasspath(List<Path> classpath) {
		return classpath.stream()
			.map(Path::toFile)
			.filter(File::exists)
			.filter(f -> f.isDirectory() || !f.getName().endsWith(".class"))
			.map(File::toPath)
			.toList();
	}

	private ITypeReference createITypeReference(CtTypeReference<?> typeRef) {
		return switch (typeRef) {
			case CtArrayTypeReference<?> arrayRef ->
				typeReferenceFactory.createArrayTypeReference(createITypeReference(arrayRef.getArrayType()),
					arrayRef.getDimensionCount());
			case CtWildcardReference wcRef ->
				typeReferenceFactory.createWildcardTypeReference(convertCtTypeParameterBounds(wcRef.getBoundingType()),
					wcRef.isUpper());
			case CtTypeParameterReference tpRef ->
				typeReferenceFactory.createTypeParameterReference(tpRef.getQualifiedName());
			case CtTypeReference<?> ref when ref.isPrimitive() ->
				typeReferenceFactory.createPrimitiveTypeReference(ref.getQualifiedName());
			default -> createTypeReference(typeRef);
		};
	}

	private <T extends TypeDecl> TypeReference<T> createTypeReference(CtTypeReference<?> typeRef) {
		return typeRef != null
			? typeReferenceFactory.createTypeReference(typeRef.getQualifiedName(),
			createITypeReferences(typeRef.getActualTypeArguments()))
			: null;
	}

	private <T extends TypeDecl> TypeReference<T> createTypeReference(CtType<?> type) {
		return type != null ? createTypeReference(type.getReference()) : null;
	}

	private List<ITypeReference> createITypeReferences(Collection<CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::createITypeReference)
			.filter(Objects::nonNull)
			.toList();
	}

	private <T extends TypeDecl> List<TypeReference<T>> createTypeReferences(Collection<CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::<T>createTypeReference)
			.filter(Objects::nonNull)
			.toList();
	}

	public TypeDecl convertCtType(CtType<?> type) {
		return switch (type) {
			case CtAnnotationType<?> a -> convertCtAnnotationType(a);
			case CtInterface<?> i -> convertCtInterface(i);
			case CtRecord r -> convertCtRecord(r);
			case CtEnum<?> e -> convertCtEnum(e);
			case CtClass<?> c -> convertCtClass(c);
			default -> throw new IllegalArgumentException("Unknown type kind: " + type);
		};
	}

	public TypeDecl convertCtType(String qualifiedName) {
		CtTypeReference<?> ref = typeFactory.createReference(qualifiedName);
		// Spoon's null newShadowClass thingy
		try {
			return ref.getTypeDeclaration() != null ? convertCtType(ref.getTypeDeclaration()) : null;
		} catch (Exception e) {
			LOGGER.warn("Couldn't convert {}", qualifiedName, e);
			return null;
		}
	}

	private ClassDecl convertCtClass(CtClass<?> cls) {
		return new ClassDecl(
			cls.getQualifiedName(),
			convertSpoonVisibility(cls.getVisibility()),
			convertSpoonNonAccessModifiers(cls.getModifiers()),
			convertSpoonAnnotations(cls.getAnnotations()),
			convertSpoonPosition(cls.getPosition(), cls),
			createTypeReferences(cls.getSuperInterfaces()),
			convertCtFormalTypeParameters(cls),
			convertCtFields(cls),
			convertCtMethods(cls),
			createTypeReference(cls.getDeclaringType()),
			createTypeReference(cls.getSuperclass()),
			convertCtConstructors(cls),
			convertCtSealable(cls)
		);
	}

	private InterfaceDecl convertCtInterface(CtInterface<?> intf) {
		return new InterfaceDecl(
			intf.getQualifiedName(),
			convertSpoonVisibility(intf.getVisibility()),
			convertSpoonNonAccessModifiers(intf.getModifiers()),
			convertSpoonAnnotations(intf.getAnnotations()),
			convertSpoonPosition(intf.getPosition(), intf),
			createTypeReferences(intf.getSuperInterfaces()),
			convertCtFormalTypeParameters(intf),
			convertCtFields(intf),
			convertCtMethods(intf),
			createTypeReference(intf.getDeclaringType()),
			convertCtSealable(intf)
		);
	}

	private AnnotationDecl convertCtAnnotationType(CtAnnotationType<?> annotation) {
		return new AnnotationDecl(
			annotation.getQualifiedName(),
			convertSpoonVisibility(annotation.getVisibility()),
			convertSpoonNonAccessModifiers(annotation.getModifiers()),
			convertSpoonAnnotations(annotation.getAnnotations()),
			convertSpoonPosition(annotation.getPosition(), annotation),
			convertCtFields(annotation),
			convertCtMethods(annotation),
			createTypeReference(annotation.getDeclaringType())
		);
	}

	private EnumDecl convertCtEnum(CtEnum<?> enm) {
		return new EnumDecl(
			enm.getQualifiedName(),
			convertSpoonVisibility(enm.getVisibility()),
			convertSpoonNonAccessModifiers(enm.getModifiers()),
			convertSpoonAnnotations(enm.getAnnotations()),
			convertSpoonPosition(enm.getPosition(), enm),
			createTypeReferences(enm.getSuperInterfaces()),
			convertCtFields(enm),
			convertCtMethods(enm),
			createTypeReference(enm.getDeclaringType()),
			convertCtConstructors(enm),
			convertCtEnumValues(enm)
		);
	}

	private RecordDecl convertCtRecord(CtRecord rcrd) {
		return new RecordDecl(
			rcrd.getQualifiedName(),
			convertSpoonVisibility(rcrd.getVisibility()),
			convertSpoonNonAccessModifiers(rcrd.getModifiers()),
			convertSpoonAnnotations(rcrd.getAnnotations()),
			convertSpoonPosition(rcrd.getPosition(), rcrd),
			createTypeReferences(rcrd.getSuperInterfaces()),
			convertCtFormalTypeParameters(rcrd),
			convertCtFields(rcrd),
			convertCtMethods(rcrd),
			createTypeReference(rcrd.getDeclaringType()),
			convertCtConstructors(rcrd),
			convertCtRecordComponents(rcrd)
		);
	}

	private FieldDecl convertCtField(CtField<?> field) {
		return new FieldDecl(
			makeQualifiedName(field),
			convertSpoonVisibility(field.getVisibility()),
			convertSpoonNonAccessModifiers(field.getModifiers()),
			convertSpoonAnnotations(field.getAnnotations()),
			convertSpoonPosition(field.getPosition(), field.getDeclaringType()),
			createTypeReference(field.getDeclaringType()),
			createITypeReference(field.getType())
		);
	}

	private MethodDecl convertCtMethod(CtMethod<?> method) {
		// Spoon does not store 'default' information as modifier, but we do
		EnumSet<Modifier> modifiers = Stream.concat(
			convertSpoonNonAccessModifiers(method.getModifiers()).stream(),
			method.isDefaultMethod() ? Stream.of(Modifier.DEFAULT) : Stream.empty()
		).collect(Collectors.toCollection(() -> EnumSet.noneOf(Modifier.class)));

		return new MethodDecl(
			makeQualifiedName(method),
			convertSpoonVisibility(method.getVisibility()),
			modifiers,
			convertSpoonAnnotations(method.getAnnotations()),
			convertSpoonPosition(method.getPosition(), method.getDeclaringType()),
			createTypeReference(method.getDeclaringType()),
			createITypeReference(method.getType()),
			convertCtParameters(method),
			convertCtFormalTypeParameters(method),
			createITypeReferences(new ArrayList<>(method.getThrownTypes()))
		);
	}

	private ConstructorDecl convertCtConstructor(CtConstructor<?> cons) {
		return new ConstructorDecl(
			makeQualifiedName(cons),
			convertSpoonVisibility(cons.getVisibility()),
			convertSpoonNonAccessModifiers(cons.getModifiers()),
			convertSpoonAnnotations(cons.getAnnotations()),
			convertSpoonPosition(cons.getPosition(), cons.getDeclaringType()),
			createTypeReference(cons.getDeclaringType()),
			createITypeReference(cons.getType()),
			convertCtParameters(cons),
			convertCtFormalTypeParameters(cons),
			createITypeReferences(new ArrayList<>(cons.getThrownTypes()))
		);
	}

	private List<FieldDecl> convertCtFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(SpoonAPIFactory::isExported)
			.map(this::convertCtField)
			.toList();
	}

	private List<MethodDecl> convertCtMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(SpoonAPIFactory::isExported)
			.map(this::convertCtMethod)
			.toList();
	}

	private List<ConstructorDecl> convertCtConstructors(CtClass<?> cls) {
		// We need to keep track of default constructors in the API model.
		// In such case, Spoon indeed returns an (implicit) constructor, but its visibility is null,
		// so we need to handle it separately.
		return cls.getConstructors().stream()
			.filter(SpoonAPIFactory::isExported)
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
			convertCtTypeParameterBounds(parameter.getSuperclass() != null
				// If there are no bounds, we make the bound to java.lang.Object explicit
				? parameter.getSuperclass()
				: typeFactory.objectType()
			)
		);
	}

	private List<ITypeReference> convertCtTypeParameterBounds(CtTypeReference<?> ref) {
		return switch (ref) {
			case CtIntersectionTypeReference<?> intersection ->
				intersection.getBounds().stream().map(this::createITypeReference).toList();
			case CtTypeReference<?> reference -> List.of(createITypeReference(reference));
			case null -> Collections.emptyList();
		};
	}

	private List<ParameterDecl> convertCtParameters(CtExecutable<?> executable) {
		return executable.getParameters().stream()
			.map(this::convertCtParameter)
			.toList();
	}

	private ParameterDecl convertCtParameter(CtParameter<?> parameter) {
		// Spoon treats varargs as arrays, which is correct but not what we want to properly match signatures
		return parameter.isVarArgs() && parameter.getType() instanceof CtArrayTypeReference<?> atr
			? new ParameterDecl(parameter.getSimpleName(), createITypeReference(atr.getComponentType()), true)
			: new ParameterDecl(parameter.getSimpleName(), createITypeReference(parameter.getType()), false);
	}

	private List<String> convertCtSealable(CtSealable sealable) {
		return sealable.getPermittedTypes().stream()
			.map(CtTypeReference::getSimpleName)
			.toList();
	}

	private List<RecordComponentDecl> convertCtRecordComponents(CtRecord rcrd) {
		return rcrd.getRecordComponents().stream()
			.map(rcrdCpt -> convertCtRecordComponent(rcrdCpt, rcrd))
			.toList();
	}

	private RecordComponentDecl convertCtRecordComponent(CtRecordComponent rcrdCpt, CtRecord rcrd) {
		return new RecordComponentDecl(
			makeQualifiedName(rcrdCpt, rcrd),
			convertSpoonAnnotations(rcrdCpt.getAnnotations()),
			convertSpoonPosition(rcrdCpt.getPosition(), rcrd),
			createTypeReference(rcrd),
			createITypeReference(rcrdCpt.getType()),
			false
		);
	}

	private List<EnumValueDecl> convertCtEnumValues(CtEnum<?> enm) {
		return enm.getEnumValues().stream()
			.map(this::convertCtEnumValue)
			.toList();
	}

	private EnumValueDecl convertCtEnumValue(CtEnumValue<?> enmVal) {
		return new EnumValueDecl(
			makeQualifiedName(enmVal),
			convertSpoonAnnotations(enmVal.getAnnotations()),
			convertSpoonPosition(enmVal.getPosition(), enmVal.getDeclaringType()),
			createTypeReference(enmVal.getDeclaringType()),
			createITypeReference(enmVal.getType())
		);
	}

	private AccessModifier convertSpoonVisibility(ModifierKind visibility) {
		return switch (visibility) {
			case PUBLIC -> AccessModifier.PUBLIC;
			case PRIVATE -> AccessModifier.PRIVATE;
			case PROTECTED -> AccessModifier.PROTECTED;
			case null -> AccessModifier.PACKAGE_PRIVATE;
			default -> throw new IllegalArgumentException("Unknown visibility " + visibility);
		};
	}

	private static Modifier convertSpoonModifier(ModifierKind modifier) {
		return switch (modifier) {
			case STATIC -> Modifier.STATIC;
			case FINAL -> Modifier.FINAL;
			case ABSTRACT -> Modifier.ABSTRACT;
			case SYNCHRONIZED -> Modifier.SYNCHRONIZED;
			case VOLATILE -> Modifier.VOLATILE;
			case TRANSIENT -> Modifier.TRANSIENT;
			case SEALED -> Modifier.SEALED;
			case NON_SEALED -> Modifier.NON_SEALED;
			case NATIVE -> Modifier.NATIVE;
			case STRICTFP -> Modifier.STRICTFP;
			default -> throw new IllegalArgumentException("Unknown modifier " + modifier);
		};
	}

	private static EnumSet<Modifier> convertSpoonNonAccessModifiers(Collection<ModifierKind> modifiers) {
		return modifiers.stream()
			.filter(mod -> !Set.of(ModifierKind.PUBLIC, ModifierKind.PROTECTED, ModifierKind.PRIVATE).contains(mod))
			.map(SpoonAPIFactory::convertSpoonModifier)
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(Modifier.class)));
	}

	private List<Annotation> convertSpoonAnnotations(List<CtAnnotation<?>> annotations) {
		return annotations.stream()
			.map(this::convertSpoonAnnotation)
			.toList();
	}

	private Annotation convertSpoonAnnotation(CtAnnotation<?> annotation) {
		return new Annotation(createTypeReference(annotation.getAnnotationType()));
	}

	/**
	 * If the provided position isn't valid, fallback to the other element's file. e.g., an implicit constructor will
	 * point to the file it's contained in.
	 */
	private static SourceLocation convertSpoonPosition(SourcePosition position, CtElement fallback) {
		if (position.isValidPosition()) {
			return new SourceLocation(
				position.getFile() != null ? position.getFile().toPath() : null,
				position.getLine(),
				position.getColumn());
		} else if (fallback != null && fallback.getPosition() != null) {
			SourcePosition fallbackPosition = fallback.getPosition();
			if (fallbackPosition.isValidPosition() && fallbackPosition.getFile() != null) {
				return new SourceLocation(fallback.getPosition().getFile().toPath(), -1, -1);
			}
		}

		return SourceLocation.NO_LOCATION;
	}

	private static boolean isExported(CtTypeMember member) {
		/*
		 * This is kinda tricky due to API types leaking internal types. In the following,
		 * A itself and anything it declares cannot be accessed outside 'pkg'.
		 * However, B re-opens A through subclassing and effectively re-exports the declarations
		 * it sees through its own public visibility. A client class C extending B would see m().
		 * So we have to keep A's potentially-leaked declarations to mark them later as part of B's API.
		 * It's possible to re-open and leak a type within the API if:
		 *   - It has a non-private constructor (package-private can be re-opened within the same package)
		 *   - It is not explicitly 'final' or 'sealed' (sealed subclasses can attempt to leak but they're final
		 *    themselves so they won't leak to clients)
		 *
		 * class A { // package 'pkg'
		 *   A() {}
		 *   protected void m() {}
		 * }
		 * public class B extends A {} // package 'pkg'
		 */
		return member.isPublic() || (member.isProtected() && !isEffectivelyFinal(member.getDeclaringType()));
	}

	/**
	 * Checks whether the given type is effectively final _within the API_. While package-private constructors cannot be
	 * accessed from client code, they can be from the API itself, and sub-classes can leak internals.
	 */
	private static boolean isEffectivelyFinal(CtType<?> type) {
		if (type instanceof CtClass<?> cls &&
			!cls.getConstructors().isEmpty() &&
			cls.getConstructors().stream().allMatch(CtModifiable::isPrivate)) {
			return true;
		}

		return (type.isFinal() || type.hasModifier(ModifierKind.SEALED))
			&& !type.hasModifier(ModifierKind.NON_SEALED);
	}

	private static String makeQualifiedName(CtTypeMember member) {
		return member.getDeclaringType().getQualifiedName() + "." + member.getSimpleName();
	}

	private static String makeQualifiedName(CtNamedElement member, CtType<?> declaringType) {
		return String.format("%s.%s", declaringType.getQualifiedName(), member.getSimpleName());
	}
}
