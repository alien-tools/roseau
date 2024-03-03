package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.Annotation;
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
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.SpoonTypeReferenceFactory;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtFormalTypeDeclarer;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.TypeFactory;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtIntersectionTypeReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtWildcardReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class SpoonAPIFactory {
	private final TypeFactory typeFactory;
	private final TypeReferenceFactory typeReferenceFactory;

	public SpoonAPIFactory(TypeFactory typeFactory) {
		this.typeFactory = Objects.requireNonNull(typeFactory);
		this.typeReferenceFactory = new SpoonTypeReferenceFactory(this);
	}

	public TypeReferenceFactory getTypeReferenceFactory() {
		return typeReferenceFactory;
	}

	private ITypeReference createITypeReference(CtTypeReference<?> typeRef) {
		return switch (typeRef) {
			case CtArrayTypeReference<?> arrayRef -> typeReferenceFactory.createArrayTypeReference(createITypeReference(arrayRef.getArrayType()), arrayRef.getDimensionCount());
			case CtWildcardReference wcRef -> typeReferenceFactory.createWildcardTypeReference(convertCtTypeParameterBounds(wcRef.getBoundingType()), wcRef.isUpper());
			case CtTypeParameterReference tpRef -> typeReferenceFactory.createTypeParameterReference(tpRef.getQualifiedName());
			case CtTypeReference<?> ref when ref.isPrimitive() -> typeReferenceFactory.createPrimitiveTypeReference(ref.getQualifiedName());
			default -> createTypeReference(typeRef);
		};
	}

	private <T extends TypeDecl> TypeReference<T> createTypeReference(CtTypeReference<?> typeRef) {
		return typeRef != null ? typeReferenceFactory.createTypeReference(typeRef.getQualifiedName(), createITypeReferences(typeRef.getActualTypeArguments())) : null;
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
			case CtInterface<?> i      -> convertCtInterface(i);
			case CtRecord r            -> convertCtRecord(r);
			case CtEnum<?> e           -> convertCtEnum(e);
			case CtClass<?> c          -> convertCtClass(c);
			default -> throw new IllegalArgumentException("Unknown type kind: " + type);
		};
	}

	public TypeDecl convertCtType(String qualifiedName) {
		CtTypeReference<?> ref = typeFactory.createReference(qualifiedName);

		return ref.getTypeDeclaration() != null
			? convertCtType(ref.getTypeDeclaration())
			: null;
	}

	private ClassDecl convertCtClass(CtClass<?> cls) {
		return new ClassDecl(
			cls.getQualifiedName(),
			convertSpoonVisibility(cls.getVisibility()),
			convertSpoonNonAccessModifiers(cls.getModifiers()),
			convertSpoonAnnotations(cls.getAnnotations()),
			convertSpoonPosition(cls.getPosition()),
			createTypeReferences(cls.getSuperInterfaces()),
			convertCtFormalTypeParameters(cls),
			convertCtFields(cls),
			convertCtMethods(cls),
			createTypeReference(cls.getDeclaringType()),
			createTypeReference(cls.getSuperclass()),
			convertCtConstructors(cls)
		);
	}

	private InterfaceDecl convertCtInterface(CtInterface<?> intf) {
		return new InterfaceDecl(
			intf.getQualifiedName(),
			convertSpoonVisibility(intf.getVisibility()),
			convertSpoonNonAccessModifiers(intf.getModifiers()),
			convertSpoonAnnotations(intf.getAnnotations()),
			convertSpoonPosition(intf.getPosition()),
			createTypeReferences(intf.getSuperInterfaces()),
			convertCtFormalTypeParameters(intf),
			convertCtFields(intf),
			convertCtMethods(intf),
			createTypeReference(intf.getDeclaringType())
			);
	}

	private AnnotationDecl convertCtAnnotationType(CtAnnotationType<?> annotation) {
		return new AnnotationDecl(
			annotation.getQualifiedName(),
			convertSpoonVisibility(annotation.getVisibility()),
			convertSpoonNonAccessModifiers(annotation.getModifiers()),
			convertSpoonAnnotations(annotation.getAnnotations()),
			convertSpoonPosition(annotation.getPosition()),
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
			convertSpoonPosition(enm.getPosition()),
			createTypeReferences(enm.getSuperInterfaces()),
			convertCtFields(enm),
			convertCtMethods(enm),
			createTypeReference(enm.getDeclaringType()),
			convertCtConstructors(enm)
		);
	}

	private RecordDecl convertCtRecord(CtRecord record) {
		return new RecordDecl(
			record.getQualifiedName(),
			convertSpoonVisibility(record.getVisibility()),
			convertSpoonNonAccessModifiers(record.getModifiers()),
			convertSpoonAnnotations(record.getAnnotations()),
			convertSpoonPosition(record.getPosition()),
			createTypeReferences(record.getSuperInterfaces()),
			convertCtFormalTypeParameters(record),
			convertCtFields(record),
			convertCtMethods(record),
			createTypeReference(record.getDeclaringType()),
			convertCtConstructors(record)
		);
	}

	private FieldDecl convertCtField(CtField<?> field) {
		return new FieldDecl(
			makeQualifiedName(field),
			convertSpoonVisibility(field.getVisibility()),
			convertSpoonNonAccessModifiers(field.getModifiers()),
			convertSpoonAnnotations(field.getAnnotations()),
			convertSpoonPosition(field.getPosition()),
			createTypeReference(field.getDeclaringType()),
			createITypeReference(field.getType())
		);
	}

	private MethodDecl convertCtMethod(CtMethod<?> method) {
		// Spoon does not store 'default' information as modifier, but we do
		List<Modifier> modifiers = Stream.concat(
			convertSpoonNonAccessModifiers(method.getModifiers()).stream(),
			method.isDefaultMethod() ? Stream.of(Modifier.DEFAULT) : Stream.empty()
		).toList();

		return new MethodDecl(
			makeQualifiedName(method),
			convertSpoonVisibility(method.getVisibility()),
			modifiers,
			convertSpoonAnnotations(method.getAnnotations()),
			convertSpoonPosition(method.getPosition()),
			createTypeReference(method.getDeclaringType()),
			createITypeReference(method.getType()),
			convertCtParameters(method),
			convertCtFormalTypeParameters(method),
			createTypeReferences(new ArrayList<>(method.getThrownTypes()))
		);
	}

	private ConstructorDecl convertCtConstructor(CtConstructor<?> cons) {
		return new ConstructorDecl(
			makeQualifiedName(cons),
			convertSpoonVisibility(cons.getVisibility()),
			convertSpoonNonAccessModifiers(cons.getModifiers()),
			convertSpoonAnnotations(cons.getAnnotations()),
			convertSpoonPosition(cons.getPosition()),
			createTypeReference(cons.getDeclaringType()),
			createITypeReference(cons.getType()),
			convertCtParameters(cons),
			convertCtFormalTypeParameters(cons),
			createTypeReferences(new ArrayList<>(cons.getThrownTypes()))
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
		// We need to keep track of default constructors in the API model.
		// In such case, Spoon indeed returns an (implicit) constructor, but its visibility is null,
		// so we need to handle it separately.
		return cls.getConstructors().stream()
			.filter(cons -> isExported(cons) || cons.isImplicit())
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
			convertCtTypeParameterBounds(parameter.getSuperclass())
		);
	}

	private List<ITypeReference> convertCtTypeParameterBounds(CtTypeReference<?> ref) {
		return switch (ref) {
			case CtIntersectionTypeReference<?> intersection -> intersection.getBounds().stream().map(this::createITypeReference).toList();
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
		return new ParameterDecl(parameter.getSimpleName(), createITypeReference(parameter.getType()), parameter.isVarArgs());
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

	private List<Modifier> convertSpoonNonAccessModifiers(Collection<ModifierKind> modifiers) {
		return modifiers.stream()
			.filter(mod ->
				     ModifierKind.PUBLIC != mod
					&& ModifierKind.PROTECTED != mod
					&& ModifierKind.PRIVATE != mod)
			.map(this::convertSpoonModifier)
			.toList();
	}

	private List<Annotation> convertSpoonAnnotations(List<CtAnnotation<?>> annotations) {
		return annotations.stream()
			.map(this::convertSpoonAnnotation)
			.toList();
	}

	private Annotation convertSpoonAnnotation(CtAnnotation<?> annotation) {
		return new Annotation(createTypeReference(annotation.getAnnotationType()));
	}

	private SourceLocation convertSpoonPosition(SourcePosition position) {
		return position.isValidPosition()
			? new SourceLocation(
				position.getFile() != null ? position.getFile().toPath() : null,
				position.getLine())
			: SourceLocation.NO_LOCATION;
	}

	private boolean isExported(CtType<?> type) {
		return
			(type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type)))
				&& isParentExported(type);
	}

	private boolean isExported(CtTypeMember member) {
		return (member.isPublic() || (member.isProtected() && !isEffectivelyFinal(member.getDeclaringType())))
			&& isParentExported(member);
	}

	private boolean isParentExported(CtTypeMember member) {
		return member.getDeclaringType() == null || isExported(member.getDeclaringType());
	}

	private boolean isEffectivelyFinal(CtType<?> type) {
		if (type instanceof CtClass<?> cls)
			if (!cls.getConstructors().isEmpty()
				&& cls.getConstructors().stream().noneMatch(cons -> cons.isPublic() || cons.isProtected()))
				return true;

		return type.isFinal() || type.hasModifier(ModifierKind.SEALED);
	}

	private String makeQualifiedName(CtTypeMember member) {
		return String.format("%s.%s", member.getDeclaringType().getQualifiedName(), member.getSimpleName());
	}
}
