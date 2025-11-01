package io.github.alien.roseau.extractors.spoon;

import com.google.common.collect.ImmutableSet;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spoon.Launcher;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationMethod;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtModule;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtPackageExport;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtSealable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtIntersectionTypeReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A factory of {@link TypeDecl} and {@link TypeReference} instances using Spoon.
 */
public class SpoonApiFactory {
	private final TypeFactory typeFactory;
	private final ApiFactory factory;
	private final Path basePath;

	private static final Logger LOGGER = LogManager.getLogger(SpoonApiFactory.class);

	public SpoonApiFactory(Library library, ApiFactory factory) {
		Factory spoonFactory = new Launcher().createFactory();
		spoonFactory.getEnvironment().setSourceClasspath(
			sanitizeClasspath(library.getClasspath()).stream()
				.map(p -> p.toAbsolutePath().toString())
				.toArray(String[]::new));
		SpoonUtils.setupEnvironment(spoonFactory.getEnvironment());
		this.typeFactory = spoonFactory.Type();
		this.factory = factory;
		this.basePath = library.getLocation();
	}

	// Avoid having Spoon throwing at us due to "invalid" classpath
	private List<Path> sanitizeClasspath(Set<Path> classpath) {
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
				factory.references().createArrayTypeReference(createITypeReference(arrayRef.getArrayType()),
					arrayRef.getDimensionCount());
			case CtWildcardReference wcRef ->
				factory.references().createWildcardTypeReference(convertCtTypeParameterBounds(wcRef.getBoundingType()),
					wcRef.isUpper());
			case CtTypeParameterReference tpRef ->
				factory.references().createTypeParameterReference(tpRef.getQualifiedName());
			case CtTypeReference<?> ref when ref.isPrimitive() ->
				factory.references().createPrimitiveTypeReference(ref.getQualifiedName());
			default -> createTypeReference(typeRef);
		};
	}

	private <T extends TypeDecl> TypeReference<T> createTypeReference(CtTypeReference<?> typeRef) {
		return typeRef != null
			? factory.references().createTypeReference(typeRef.getQualifiedName(),
			createITypeReferencesList(typeRef.getActualTypeArguments()))
			: null;
	}

	private <T extends TypeDecl> TypeReference<T> createTypeReference(CtType<?> type) {
		return type != null ? createTypeReference(type.getReference()) : null;
	}

	private Set<ITypeReference> createITypeReferences(Collection<CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::createITypeReference)
			.filter(Objects::nonNull)
			.collect(ImmutableSet.toImmutableSet());
	}

	private List<ITypeReference> createITypeReferencesList(Collection<CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::createITypeReference)
			.filter(Objects::nonNull)
			.toList();
	}

	private <T extends TypeDecl> Set<TypeReference<T>> createTypeReferences(Collection<CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::<T>createTypeReference)
			.filter(Objects::nonNull)
			.collect(ImmutableSet.toImmutableSet());
	}

	public ModuleDecl convertCtModule(CtModule module) {
		return factory.createModule(
			module.getSimpleName(),
			module.getExportedPackages().stream()
				// qualified exports (`exports pkg to m`) are not considered exported
				.filter(pkg -> pkg.getTargetExport().isEmpty())
				.map(CtPackageExport::getPackageReference)
				.map(CtPackageReference::getQualifiedName)
				.collect(ImmutableSet.toImmutableSet()));
	}

	public TypeDecl convertCtType(CtType<?> type) {
		return switch (type) {
			case CtAnnotationType<?> a -> convertCtAnnotationType(a);
			case CtInterface<?> i -> convertCtInterface(i);
			case CtRecord r -> convertCtRecord(r);
			case CtEnum<?> e -> convertCtEnum(e);
			case CtClass<?> c -> convertCtClass(c);
			default -> throw new RoseauException("Unexpected type kind: " + type);
		};
	}

	public TypeDecl convertCtType(String qualifiedName) {
		CtTypeReference<?> ref = typeFactory.createReference(qualifiedName);
		// Spoon's null newShadowClass thingy
		try {
			return ref.getTypeDeclaration() != null ? convertCtType(ref.getTypeDeclaration()) : null;
		} catch (RuntimeException e) {
			LOGGER.warn("Couldn't convert {}", qualifiedName, e);
			return null;
		}
	}

	private ClassDecl convertCtClass(CtClass<?> cls) {
		return factory.createClass(
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
		return factory.createInterface(
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
		return factory.createAnnotation(
			annotation.getQualifiedName(),
			convertSpoonVisibility(annotation.getVisibility()),
			convertSpoonNonAccessModifiers(annotation.getModifiers()),
			convertSpoonAnnotations(annotation.getAnnotations()),
			convertSpoonPosition(annotation.getPosition(), annotation),
			convertCtFields(annotation),
			convertCtAnnotationMethods(annotation),
			createTypeReference(annotation.getDeclaringType()),
			convertAnnotationTargets(annotation)
		);
	}

	private EnumDecl convertCtEnum(CtEnum<?> enm) {
		return factory.createEnum(
			enm.getQualifiedName(),
			convertSpoonVisibility(enm.getVisibility()),
			convertSpoonNonAccessModifiers(enm.getModifiers()),
			convertSpoonAnnotations(enm.getAnnotations()),
			convertSpoonPosition(enm.getPosition(), enm),
			createTypeReferences(enm.getSuperInterfaces()),
			convertCtFields(enm),
			convertCtMethods(enm),
			createTypeReference(enm.getDeclaringType()),
			convertCtConstructors(enm)
		);
	}

	private RecordDecl convertCtRecord(CtRecord rcrd) {
		return factory.createRecord(
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
			convertCtConstructors(rcrd)
		);
	}

	private FieldDecl convertCtField(CtField<?> field) {
		return factory.createField(
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
		Set<Modifier> modifiers = Stream.concat(
			convertSpoonNonAccessModifiers(method.getModifiers()).stream(),
			method.isDefaultMethod() ? Stream.of(Modifier.DEFAULT) : Stream.empty()
		).collect(Collectors.toCollection(() -> EnumSet.noneOf(Modifier.class)));

		return factory.createMethod(
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

	private AnnotationMethodDecl convertCtAnnotationMethod(CtAnnotationMethod<?> method) {
		return factory.createAnnotationMethod(
			makeQualifiedName(method),
			convertSpoonAnnotations(method.getAnnotations()),
			convertSpoonPosition(method.getPosition(), method.getDeclaringType()),
			createTypeReference(method.getDeclaringType()),
			createITypeReference(method.getType()),
			method.getDefaultExpression() != null
		);
	}

	private ConstructorDecl convertCtConstructor(CtConstructor<?> cons) {
		return factory.createConstructor(
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

	private Set<FieldDecl> convertCtFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(SpoonApiFactory::isExported)
			.map(this::convertCtField)
			.collect(ImmutableSet.toImmutableSet());
	}

	private Set<MethodDecl> convertCtMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(SpoonApiFactory::isExported)
			.map(this::convertCtMethod)
			.collect(ImmutableSet.toImmutableSet());
	}

	private Set<AnnotationMethodDecl> convertCtAnnotationMethods(CtAnnotationType<?> type) {
		return type.getAnnotationMethods().stream()
			.map(this::convertCtAnnotationMethod)
			.collect(ImmutableSet.toImmutableSet());
	}

	private Set<ConstructorDecl> convertCtConstructors(CtClass<?> cls) {
		// We need to keep track of default constructors in the API model.
		// In such case, Spoon indeed returns an (implicit) constructor, but its visibility is null,
		// so we need to handle it separately.
		return cls.getConstructors().stream()
			.filter(SpoonApiFactory::isExported)
			.map(this::convertCtConstructor)
			.collect(ImmutableSet.toImmutableSet());
	}

	private List<FormalTypeParameter> convertCtFormalTypeParameters(CtFormalTypeDeclarer declarer) {
		return declarer.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();
	}

	private FormalTypeParameter convertCtTypeParameter(CtTypeParameter parameter) {
		return factory.createFormalTypeParameter(
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
			case null -> List.of();
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
			? factory.createParameter(parameter.getSimpleName(), createITypeReference(atr.getComponentType()), true)
			: factory.createParameter(parameter.getSimpleName(), createITypeReference(parameter.getType()), false);
	}

	private Set<TypeReference<TypeDecl>> convertCtSealable(CtSealable sealable) {
		return sealable.getPermittedTypes().stream()
			.map(this::createTypeReference)
			.collect(ImmutableSet.toImmutableSet());
	}

	private AccessModifier convertSpoonVisibility(ModifierKind visibility) {
		return switch (visibility) {
			case PUBLIC -> AccessModifier.PUBLIC;
			case PRIVATE -> AccessModifier.PRIVATE;
			case PROTECTED -> AccessModifier.PROTECTED;
			case null -> AccessModifier.PACKAGE_PRIVATE;
			default -> throw new RoseauException("Unexpected visibility " + visibility);
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
			default -> throw new RoseauException("Unexpected modifier " + modifier);
		};
	}

	private static Set<Modifier> convertSpoonNonAccessModifiers(Collection<ModifierKind> modifiers) {
		return modifiers.stream()
			.filter(mod -> !Set.of(ModifierKind.PUBLIC, ModifierKind.PROTECTED, ModifierKind.PRIVATE).contains(mod))
			.map(SpoonApiFactory::convertSpoonModifier)
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(Modifier.class)));
	}

	private Set<Annotation> convertSpoonAnnotations(List<CtAnnotation<?>> annotations) {
		return annotations.stream()
			.filter(ann -> !isSourceAnnotation(ann))
			.map(this::convertSpoonAnnotation)
			.collect(ImmutableSet.toImmutableSet());
	}

	private boolean isSourceAnnotation(CtAnnotation<?> annotation) {
		CtType<?> type = annotation.getAnnotationType().getTypeDeclaration();
		if (type != null) {
			CtAnnotation<?> retention = type.getAnnotation(
				typeFactory.createReference(Retention.class.getCanonicalName()));
			if (retention != null) {
				CtExpression<?> value = retention.getValue("value");
				if (value instanceof CtFieldRead<?> fieldRead) {
					return fieldRead.getVariable().getSimpleName().equals("SOURCE");
				}
			}
		}

		return false;
	}

	private Annotation convertSpoonAnnotation(CtAnnotation<?> annotation) {
		Map<String, String> values = annotation.getValues().entrySet().stream()
			.filter(e -> e.getValue() != null)
			.collect(Collectors.toMap(
				Map.Entry::getKey, e -> extractAnnotationValue(e.getValue())
			));
		return factory.createAnnotation(createTypeReference(annotation.getAnnotationType()), values);
	}

	private String extractAnnotationValue(Object value) {
		return switch (value) {
			case CtLiteral<?> literal -> {
				Object lit = literal.getValue();
				yield Optional.ofNullable(lit).map(Object::toString).orElse("null");
			}
			case CtFieldRead<?> field -> {
				CtFieldReference<?> variable = field.getVariable();
				if (Class.class.getCanonicalName().equals(variable.getType().getQualifiedName())) {
					yield variable.getDeclaringType().getQualifiedName();
				}
				yield variable.getDeclaringType().getQualifiedName() + "." + variable.getSimpleName();
			}
			case CtNewArray<?> ignored -> "{}";
			default -> value.toString();
		};
	}

	private Set<ElementType> convertAnnotationTargets(CtAnnotationType<?> annotation) {
		CtAnnotation<Target> target = annotation.getAnnotation(typeFactory.createReference(Target.class));

		if (target != null) {
			Object value = target.getValue("value");

			if (value instanceof CtNewArray<?> array) {
				List<CtExpression<?>> elems = array.getElements();
				return elems.stream()
					.map(CtFieldRead.class::cast)
					.map(fieldRead -> ElementType.valueOf(fieldRead.getVariable().getSimpleName()))
					.collect(ImmutableSet.toImmutableSet());
			} else if (value instanceof CtFieldRead<?> fieldRead) {
				return Set.of(ElementType.valueOf(fieldRead.getVariable().getSimpleName()));
			}
		}

		return Collections.emptySet();
	}

	/**
	 * If the provided position isn't valid, fallback to the other element's file. e.g., an implicit constructor will
	 * point to the file it's contained in.
	 */
	private SourceLocation convertSpoonPosition(SourcePosition position, CtElement fallback) {
		if (position.isValidPosition()) {
			return factory.location(
				position.getFile() != null ? basePath.relativize(position.getFile().toPath()) : null,
				position.getLine());
		} else if (fallback != null && fallback.getPosition() != null) {
			SourcePosition fallbackPosition = fallback.getPosition();
			if (fallbackPosition.isValidPosition() && fallbackPosition.getFile() != null) {
				return factory.location(basePath.relativize(fallbackPosition.getFile().toPath()), -1);
			}
		}

		return factory.unknownLocation();
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
		 *   - It is not explicitly 'final' or 'sealed' (sealed subclasses can attempt to leak, but they're final
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
	 * accessed from client code, they can be from the API itself, and subclasses can leak internals.
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
