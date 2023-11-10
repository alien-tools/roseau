package com.github.maracas.roseau;

import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.AccessModifier;
import com.github.maracas.roseau.model.AnnotationDecl;
import com.github.maracas.roseau.model.ClassDecl;
import com.github.maracas.roseau.model.ConstructorDecl;
import com.github.maracas.roseau.model.EnumDecl;
import com.github.maracas.roseau.model.FieldDecl;
import com.github.maracas.roseau.model.FormalTypeParameter;
import com.github.maracas.roseau.model.InterfaceDecl;
import com.github.maracas.roseau.model.MethodDecl;
import com.github.maracas.roseau.model.Modifier;
import com.github.maracas.roseau.model.ParameterDecl;
import com.github.maracas.roseau.model.RecordDecl;
import com.github.maracas.roseau.model.TypeDecl;
import com.github.maracas.roseau.model.TypeReference;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class represents roseau's API extraction tool.
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
		List<CtPackage> allPackages = model.getAllPackages().stream().toList();

		Map<CtTypeReference<?>, TypeDecl> allTypes = allPackages.stream()
			.map(this::getExportedTypes)
			.flatMap(Collection::stream)
			.collect(Collectors.toMap(
				t -> t,
				t -> convertSpoonTypeToTypeDeclaration(t.getTypeDeclaration())
			));

		return new API(allTypes.values().stream().toList());
	}

	private boolean isExported(CtType<?> type) {
		return type.isPublic() || (type.isProtected() && !isEffectivelyFinal(type));
	}

	private boolean isExported(CtModifiable member) {
		return member.isPublic() || member.isProtected();
	}

	private boolean isEffectivelyFinal(CtType<?> type) {
		return type.isFinal() || type.hasModifier(ModifierKind.SEALED);
	}

	// Returns all exported types within a package
	private List<CtTypeReference<?>> getExportedTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.filter(this::isExported)
			.flatMap(type -> Stream.concat(Stream.of(type.getReference()), getExportedTypes(type).stream()))
			.toList();
	}

	// Returns (recursively) the exported nested types within a type
	private List<CtTypeReference<?>> getExportedTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.filter(this::isExported)
			.flatMap(nestedType -> Stream.concat(Stream.of(nestedType.getReference()), getExportedTypes(nestedType).stream()))
			.toList();
	}

	// Returns the exported fields of a type
	private List<CtField<?>> getExportedFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns the exported methods of a type
	private List<CtMethod<?>> getExportedMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(this::isExported)
			.toList();
	}

	// Returns the exported constructors of a type
	private List<CtConstructor<?>> getExportedConstructors(CtType<?> type) {
		if (type instanceof CtClass<?> cls) {
			return new ArrayList<>(cls.getConstructors().stream()
				.filter(this::isExported)
				.toList());
		}
		return Collections.emptyList();
	}

	// Converts Spoon's ModifierKind to Roseau's AccessModifier
	private AccessModifier convertVisibility(ModifierKind visibility) {
		if (visibility == ModifierKind.PUBLIC) {
			return AccessModifier.PUBLIC;
		} else if (visibility == ModifierKind.PRIVATE) {
			return AccessModifier.PRIVATE;
		} else if (visibility == ModifierKind.PROTECTED) {
			return AccessModifier.PROTECTED;
		} else {
			return AccessModifier.DEFAULT;
		}
	}

	// Converts Spoon's non-access ModifierKind to Roseau's Modifier
	private Modifier convertNonAccessModifier(ModifierKind modifier) {
		if (modifier == ModifierKind.STATIC) {
			return Modifier.STATIC;
		} else if (modifier == ModifierKind.FINAL) {
			return Modifier.FINAL;
		} else if (modifier == ModifierKind.ABSTRACT) {
			return Modifier.ABSTRACT;
		} else if (modifier == ModifierKind.SYNCHRONIZED) {
			return Modifier.SYNCHRONIZED;
		} else if (modifier == ModifierKind.VOLATILE) {
			return Modifier.VOLATILE;
		} else if (modifier == ModifierKind.TRANSIENT) {
			return Modifier.TRANSIENT;
		} else if (modifier == ModifierKind.SEALED) {
			return Modifier.SEALED;
		} else if (modifier == ModifierKind.NON_SEALED) {
			return Modifier.NON_SEALED;
		} else if (modifier == ModifierKind.NATIVE) {
			return Modifier.NATIVE;
		} else {
			return Modifier.STRICTFP;
		}
	}

	// Filtering access modifiers because the convertVisibility() handles them already
	private List<Modifier> convertNonAccessModifiers(Set<ModifierKind> modifiers) {
		return modifiers.stream()
			.filter(mod ->
				   mod != ModifierKind.PUBLIC
				&& mod != ModifierKind.PROTECTED
				&& mod != ModifierKind.PRIVATE)
			.map(this::convertNonAccessModifier)
			.toList();
	}

	private TypeReference makeTypeReference(CtTypeReference<?> spoonType) {
		return new TypeReference(spoonType.getQualifiedName());
	}

	// Converts a CtType to a Type declaration
	private TypeDecl convertSpoonTypeToTypeDeclaration(CtType<?> spoonType) {
		String qualifiedName = spoonType.getQualifiedName();
		AccessModifier visibility = convertVisibility(spoonType.getVisibility());
		List<Modifier> modifiers = convertNonAccessModifiers(spoonType.getModifiers());
		List<FormalTypeParameter> formalTypeParameters = spoonType.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();
		TypeReference containingType = spoonType.getDeclaringType() != null
			? makeTypeReference(spoonType.getDeclaringType().getReference())
			: TypeReference.NULL;
		String position = spoonType.getPosition().toString();
		List<TypeReference> superInterfaces = spoonType.getSuperInterfaces().stream()
			.map(this::makeTypeReference)
			.toList();

		List<FieldDecl> convertedFields =
			getExportedFields(spoonType).stream()
				.map(this::convertSpoonFieldToFieldDeclaration)
				.toList();

		List<MethodDecl> convertedMethods =
			getExportedMethods(spoonType).stream()
				.map(this::convertSpoonMethodToMethodDeclaration)
				.toList();

		if (spoonType.isAnnotationType()) {
			return new AnnotationDecl(qualifiedName, visibility, modifiers, position, containingType, convertedFields, convertedMethods);
		}

		if (spoonType.isInterface()) {
			return new InterfaceDecl(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods);
		}

		if (spoonType.isClass() || spoonType.isEnum()) {
			TypeReference superClass = spoonType.getSuperclass() != null
				? makeTypeReference(spoonType.getSuperclass())
				: TypeReference.NULL;
			List<CtTypeReference<?>> allSuperClasses = getAllSuperclasses(spoonType.getReference());
			List<ConstructorDecl> convertedConstructors =
				getExportedConstructors(spoonType).stream()
					.map(this::convertSpoonConstructorToConstructorDeclaration)
					.toList();

			if (spoonType.isEnum()) {
				return new EnumDecl(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, convertedFields, convertedMethods, convertedConstructors);
			}

			if (spoonType instanceof CtRecord) {
				return new RecordDecl(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, convertedConstructors);
			}

			if (spoonType.isClass()) {
				return new ClassDecl(qualifiedName, visibility, modifiers, position, containingType, superInterfaces, formalTypeParameters, convertedFields, convertedMethods, superClass, convertedConstructors);
			}
		}

		throw new IllegalStateException("Unknown type " + spoonType.getQualifiedName());
	}

	// Converts a CtField to a Field declaration
	private FieldDecl convertSpoonFieldToFieldDeclaration(CtField<?> spoonField) {
		String qualifiedName = spoonField.getDeclaringType().getQualifiedName() + "." + spoonField.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonField.getVisibility());
		List<Modifier> modifiers = convertNonAccessModifiers(spoonField.getModifiers());
		String position = spoonField.getPosition().toString();
		TypeReference containingType = makeTypeReference(spoonField.getDeclaringType().getReference());
		TypeReference type = makeTypeReference(spoonField.getType());

		return new FieldDecl(qualifiedName, visibility, modifiers, position, containingType, type);
	}

	// Converts a CtMethod to a Method declaration
	private MethodDecl convertSpoonMethodToMethodDeclaration(CtMethod<?> spoonMethod) {
		String qualifiedName = spoonMethod.getDeclaringType().getQualifiedName() + "." + spoonMethod.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonMethod.getVisibility());
		List<Modifier> modifiers = convertNonAccessModifiers(spoonMethod.getModifiers());
		String position = spoonMethod.getPosition().toString();
		TypeReference containingType = makeTypeReference(spoonMethod.getDeclaringType().getReference());
		TypeReference returnType = makeTypeReference(spoonMethod.getType());
		List<ParameterDecl> parameters = spoonMethod.getParameters().stream()
			.map(this::convertSpoonParameterToParameter)
			.toList();

		List<TypeReference> exceptions = spoonMethod.getThrownTypes()
			.stream()
			.map(this::makeTypeReference)
			.toList();

		List<FormalTypeParameter> formalTypeParameters = spoonMethod.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();
		boolean isDefault = spoonMethod.isDefaultMethod();

		return new MethodDecl(qualifiedName, visibility, modifiers, position, containingType, returnType, parameters, formalTypeParameters, exceptions, isDefault);
	}

	// Converts a CtConstructor to a Constructor declaration
	private ConstructorDecl convertSpoonConstructorToConstructorDeclaration(CtConstructor<?> spoonConstructor) {
		String qualifiedName = spoonConstructor.getDeclaringType().getQualifiedName() + "." + spoonConstructor.getSimpleName();
		AccessModifier visibility = convertVisibility(spoonConstructor.getVisibility());
		List<Modifier> modifiers = convertNonAccessModifiers(spoonConstructor.getModifiers());
		String position = spoonConstructor.getPosition().toString();
		TypeReference containingType = makeTypeReference(spoonConstructor.getDeclaringType().getReference());
		TypeReference returnType = makeTypeReference(spoonConstructor.getType());
		List<ParameterDecl> parameters = spoonConstructor.getParameters().stream()
			.map(this::convertSpoonParameterToParameter)
			.toList();

		List<TypeReference> exceptions = spoonConstructor.getThrownTypes()
			.stream()
			.map(this::makeTypeReference)
			.toList();

		List<FormalTypeParameter> formalTypeParameters = spoonConstructor.getFormalCtTypeParameters().stream()
			.map(this::convertCtTypeParameter)
			.toList();

		return new ConstructorDecl(qualifiedName, visibility, modifiers, position, containingType, returnType, parameters, formalTypeParameters, exceptions);
	}

	private ParameterDecl convertSpoonParameterToParameter(CtParameter<?> parameter) {
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

	private List<CtTypeReference<?>> getAllSuperclasses(CtTypeReference<?> type) {
		if (type.getSuperclass() != null) {
			return Stream.concat(Stream.of(type.getSuperclass()), getAllSuperclasses(type.getSuperclass()).stream()).toList();
		} else return Collections.emptyList();
	}
}
