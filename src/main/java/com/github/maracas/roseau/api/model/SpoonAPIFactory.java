package com.github.maracas.roseau.api.model;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SpoonAPIFactory {
	private final TypeFactory typeFactory;

	public SpoonAPIFactory(TypeFactory typeFactory) {
		this.typeFactory = typeFactory;
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

	private ClassDecl convertCtClass(CtClass<?> cls) {
		return new ClassDecl(
			cls.getQualifiedName(),
			convertSpoonVisibility(cls.getVisibility()),
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
			convertSpoonNonAccessModifiers(method.getModifiers()),
			convertSpoonPosition(method.getPosition()),
			makeTypeReference(method.getDeclaringType()),
			makeTypeReference(method.getType()),
			convertCtParameters(method),
			convertCtFormalTypeParameters(method),
			makeTypeReferences(new ArrayList<>(method.getThrownTypes()))
		);
	}

	private ConstructorDecl convertCtConstructor(CtConstructor<?> cons) {
		return new ConstructorDecl(
			makeQualifiedName(cons),
			convertSpoonVisibility(cons.getVisibility()),
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
		// We need to keep track of default constructors in the API model.
		// In such case, Spoon indeed returns an (implicit) constructor, but its visibility is null
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

	private List<TypeReference<TypeDecl>> convertCtTypeParameterBounds(CtTypeReference<?> ref) {
		return switch (ref) {
			case CtIntersectionTypeReference<?> intersection -> intersection.getBounds().stream().map(this::makeTypeReference).toList();
			case CtTypeReference<?> reference -> List.of(makeTypeReference(reference));
			case null -> Collections.emptyList();
		};
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
			position.getFile() != null ? position.getFile().toPath() : null,
			position.getLine()
		);
	}

	private boolean isExported(CtType<?> type) {
		return
			(type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type)))
				&& isParentExported(type);
	}

	private boolean isExported(CtTypeMember member) {
		return (member.isPublic() || member.isProtected()) && isParentExported(member);
	}

	private boolean isParentExported(CtTypeMember member) {
		return member.getDeclaringType() == null || isExported(member.getDeclaringType());
	}

	private boolean isEffectivelyFinal(CtType<?> type) {
		// FIXME A class is also effectively final if it does not have a public (possibly default) constructor
		return type.isFinal() || type.hasModifier(ModifierKind.SEALED);
	}

	private <U extends TypeDecl> TypeReference<U> makeTypeReference(CtTypeReference<?> typeRef) {
		return switch (typeRef) {
			case CtArrayTypeReference<?> arrayRef -> new ArrayTypeReference<>(arrayRef.getComponentType().getQualifiedName(), typeFactory);
			case CtTypeParameterReference tpRef -> {
				if (tpRef.getBoundingType() instanceof CtIntersectionTypeReference<?> intersection)
					yield new TypeParameterReference<>(tpRef.getQualifiedName(), makeTypeReferences(intersection.getBounds()), typeFactory);
				else
					yield new TypeParameterReference<>(tpRef.getQualifiedName(), List.of(makeTypeReference(tpRef.getBoundingType())), typeFactory);
			}
			case CtTypeReference<?> ref when ref.isPrimitive() -> new PrimitiveTypeReference<>(ref.getQualifiedName(), typeFactory);
			case null -> null;
			default -> new TypeReference<>(typeRef.getQualifiedName(), typeFactory);
		};
	}

	private <U extends TypeDecl> TypeReference<U> makeTypeReference(CtType<?> type) {
		return type != null ? makeTypeReference(type.getReference()) : null;
	}

	private <U extends TypeDecl> List<TypeReference<U>> makeTypeReferences(Collection<CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::<U>makeTypeReference)
			.filter(java.util.Objects::nonNull)
			.toList();
	}

	private String makeQualifiedName(CtTypeMember member) {
		return String.format("%s.%s", member.getDeclaringType().getQualifiedName(), member.getSimpleName());
	}
}
