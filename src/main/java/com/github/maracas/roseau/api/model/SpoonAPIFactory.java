package com.github.maracas.roseau.api.model;

import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
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
import java.util.stream.Stream;

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
			makeTypeReferences(cls.getSuperInterfaces()),
			convertCtFormalTypeParameters(cls),
			convertCtFields(cls),
			convertCtMethods(cls),
			makeTypeReference(cls.getDeclaringType()),
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
			makeTypeReferences(intf.getSuperInterfaces()),
			convertCtFormalTypeParameters(intf),
			convertCtFields(intf),
			convertCtMethods(intf),
			makeTypeReference(intf.getDeclaringType())
			);
	}

	private AnnotationDecl convertCtAnnotationType(CtAnnotationType<?> annotation) {
		return new AnnotationDecl(
			annotation.getQualifiedName(),
			convertSpoonVisibility(annotation.getVisibility()),
			convertSpoonNonAccessModifiers(annotation.getModifiers()),
			convertSpoonPosition(annotation.getPosition()),
			convertCtFields(annotation),
			convertCtMethods(annotation),
			makeTypeReference(annotation.getDeclaringType())
			);
	}

	private EnumDecl convertCtEnum(CtEnum<?> enm) {
		return new EnumDecl(
			enm.getQualifiedName(),
			convertSpoonVisibility(enm.getVisibility()),
			convertSpoonNonAccessModifiers(enm.getModifiers()),
			convertSpoonPosition(enm.getPosition()),
			makeTypeReferences(enm.getSuperInterfaces()),
			convertCtFields(enm),
			convertCtMethods(enm),
			makeTypeReference(enm.getDeclaringType()),
			convertCtConstructors(enm)
		);
	}

	private RecordDecl convertCtRecord(CtRecord record) {
		return new RecordDecl(
			record.getQualifiedName(),
			convertSpoonVisibility(record.getVisibility()),
			convertSpoonNonAccessModifiers(record.getModifiers()),
			convertSpoonPosition(record.getPosition()),
			makeTypeReferences(record.getSuperInterfaces()),
			convertCtFormalTypeParameters(record),
			convertCtFields(record),
			convertCtMethods(record),
			makeTypeReference(record.getDeclaringType()),
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
			makeITypeReference(field.getType())
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
			convertSpoonPosition(method.getPosition()),
			makeTypeReference(method.getDeclaringType()),
			makeITypeReference(method.getType()),
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
			makeITypeReference(cons.getType()),
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
		return new ParameterDecl(parameter.getSimpleName(), makeITypeReference(parameter.getType()), parameter.isVarArgs());
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

	private ITypeReference makeITypeReference(CtTypeReference<?> typeRef) {
		return switch (typeRef) {
			case CtArrayTypeReference<?> arrayRef -> new ArrayTypeReference(makeITypeReference(arrayRef.getComponentType()));
			case CtTypeParameterReference tpRef -> {
				if (tpRef.getBoundingType() instanceof CtIntersectionTypeReference<?> intersection)
					yield new TypeParameterReference(tpRef.getQualifiedName(), makeITypeReferences(intersection.getBounds()));
				else
					yield new TypeParameterReference(tpRef.getQualifiedName(), List.of(makeITypeReference(tpRef.getBoundingType())));
			}
			case CtTypeReference<?> ref when ref.isPrimitive() -> new PrimitiveTypeReference(ref.getQualifiedName());
			case null -> null;
			default -> new TypeReference<>(typeRef.getQualifiedName(), typeFactory);
		};
	}

	public <T extends TypeDecl> TypeReference<T> makeTypeReference(String qualifiedName) {
		return qualifiedName != null ? new TypeReference<>(qualifiedName, typeFactory) : null;
	}

	public ITypeReference makePrimitiveTypeReference(String name) {
		return name != null ? new PrimitiveTypeReference(name) : null;
	}

	private <T extends TypeDecl> TypeReference<T> makeTypeReference(CtTypeReference<?> typeRef) {
		return typeRef != null ? makeTypeReference(typeRef.getQualifiedName()) : null;
	}

	private <T extends TypeDecl> TypeReference<T> makeTypeReference(CtType<?> type) {
		return type != null ? makeTypeReference(type.getReference()) : null;
	}

	private List<ITypeReference> makeITypeReferences(Collection<? extends CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::makeITypeReference)
			.filter(java.util.Objects::nonNull)
			.toList();
	}

	private <T extends TypeDecl> List<TypeReference<T>> makeTypeReferences(Collection<? extends CtTypeReference<?>> typeRefs) {
		return typeRefs.stream()
			.map(this::<T>makeTypeReference)
			.filter(java.util.Objects::nonNull)
			.toList();
	}

	private String makeQualifiedName(CtTypeMember member) {
		return String.format("%s.%s", member.getDeclaringType().getQualifiedName(), member.getSimpleName());
	}
}
