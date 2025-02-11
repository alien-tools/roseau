package com.github.maracas.roseau.extractors.jar;

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
import com.github.maracas.roseau.api.model.TypeMemberDecl;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;

class AsmClassVisitor extends ClassVisitor {
	private final TypeReferenceFactory typeRefFactory;
	private String className = null;
	private int classAccess = 0;
	private TypeDecl typeDecl = null;
	private TypeReference<TypeDecl> enclosingType = null;
	private TypeReference<ClassDecl> superClass = null;
	private List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
	private List<FieldDecl> fields = new ArrayList<>();
	private List<MethodDecl> methods = new ArrayList<>();
	private List<ConstructorDecl> constructors = new ArrayList<>();
	private List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	private List<String> annotations = new ArrayList<>();
	private boolean isSealed = false;
	private boolean hasNonPrivateConstructor = false;
	private boolean hasEnumConstantBody = false;
	private boolean shouldSkip = false;
	private int recordComponents = 0;

	private static final Logger LOGGER = LogManager.getLogger();

	AsmClassVisitor(int api, TypeReferenceFactory typeRefFactory) {
		super(api);
		this.typeRefFactory = typeRefFactory;
	}

	TypeDecl getTypeDecl() {
		return typeDecl;
	}

	@Override
	public void visitSource(String source, String debug) {
		// Skipping our non-Java JVM friends
		if (!shouldSkip && source != null && !source.endsWith(".java"))
			shouldSkip = true;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = bytecodeToFqn(name);
		classAccess = access;

		if (className.endsWith("package-info") || className.endsWith("module-info")) {
			LOGGER.trace("Skipping package/module-info {}", className);
			shouldSkip = true;
			return;
		}

		if (isSynthetic(classAccess)) {
			LOGGER.trace("Skipping synthetic class {}", className);
			shouldSkip = true;
			return;
		}

		if (signature != null) {
			SignatureReader reader = new SignatureReader(signature);
			AsmSignatureVisitor signatureVisitor = new AsmSignatureVisitor(api, typeRefFactory);
			reader.accept(signatureVisitor);
			formalTypeParameters = signatureVisitor.getFormalTypeParameters();
			superClass = signatureVisitor.getSuperclass();
			implementedInterfaces = signatureVisitor.getSuperInterfaces();
		} else {
			if (superName != null)
				superClass = typeRefFactory.createTypeReference(bytecodeToFqn(superName));
			implementedInterfaces = Arrays.stream(interfaces)
				.map(this::bytecodeToFqn)
				.map(typeRefFactory::<InterfaceDecl>createTypeReference)
				.toList();
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (shouldSkip)
			return null;

		if (isSynthetic(access)) {
			LOGGER.trace("Skipping synthetic field {}", name);
			return null;
		}

		if (!isTypeMemberExported(access)) {
			LOGGER.trace("Skipping unexported field {}", name);
			return null;
		}

		return new FieldVisitor(api) {
			List<String> annotations = new ArrayList<>();

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				annotations.add(descriptor);
				return null;
			}

			@Override
			public void visitEnd() {
				fields.add(convertField(access, name, descriptor, signature, annotations));
			}
		};
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		// These are private final fields, we don't want them
		// Just keeping track of them to know which default constructor to exclude below
		if (!shouldSkip)
			++recordComponents;
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (shouldSkip)
			return null;

		if (isSynthetic(access) || isBridge(access)) {
			LOGGER.trace("Skipping synthetic/bridge method {}", name);
			return null;
		}

		if (name.equals("<init>") && convertVisibility(access) != AccessModifier.PRIVATE) {
			hasNonPrivateConstructor = true;
		}

		if (!isTypeMemberExported(access)) {
			LOGGER.trace("Skipping unexported method {}", name);
			return null;
		}

		// FIXME: stupid heuristics below to mark non-source but non-synthetic stuff
		if (isEnum(classAccess) &&
			((name.equals("values") && descriptor.startsWith("()[L")) ||
				(name.equals("valueOf") && descriptor.startsWith("(Ljava/lang/String;)L")))) {
			LOGGER.trace("Skipping {}'s values()/valueOf()", className);
			return null;
		}

		if (isRecord(classAccess) &&
			((name.equals("toString") && descriptor.equals("()Ljava/lang/String;")) ||
				(name.equals("equals") && descriptor.equals("(Ljava/lang/Object;)Z")) ||
				(name.equals("hashCode") && descriptor.equals("()I")))) {
			LOGGER.trace("Skipping {}'s toString()/hashCode()/equals()", className);
			return null;
		}

		if (isRecord(classAccess) && (name.equals("<init>") && Type.getArgumentCount(descriptor) == recordComponents)) {
			LOGGER.trace("Skipping {}'s default constructor", className);
			return null;
		}

		return new MethodVisitor(api) {
			List<String> annotations = new ArrayList<>();

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				annotations.add(descriptor);
				return null;
			}

			@Override
			public void visitEnd() {
				if (name.equals("<init>"))
					constructors.add(convertConstructor(access, descriptor, signature, exceptions, annotations));
				else
					methods.add(convertMethod(access, name, descriptor, signature, exceptions, annotations));
			}
		};
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		if (!shouldSkip) {
			// Roseau's current API model does not care about the list of permitted subclasses
			// but we need to know whether the class is sealed or not, and there is no ACC_SEALED in ASM
			isSealed = true;
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// Constant bodies are inner classes? Though this is not 100% accurate
		hasEnumConstantBody = true;

		if (shouldSkip || !bytecodeToFqn(name).equals(className))
			return;

		if (outerName != null && innerName != null) {
			// Nested/inner types
			classAccess = access;
			enclosingType = typeRefFactory.createTypeReference(bytecodeToFqn(outerName));
		} else {
			// Anonymous/local types
			shouldSkip = true;
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (!shouldSkip)
			annotations.add(descriptor);
		return null;
	}

	@Override
	public void visitEnd() {
		if (shouldSkip)
			return;

		AccessModifier visibility = convertVisibility(classAccess);
		EnumSet<Modifier> modifiers = convertClassModifiers(classAccess);
		List<Annotation> anns = convertAnnotations(annotations);
		SourceLocation location = SourceLocation.NO_LOCATION;

		if (isSealed)
			modifiers.add(Modifier.SEALED);

		if (isEffectivelyFinal(classAccess)) {
			// We initially included all PUBLIC/PROTECTED type members
			// Now that we finally know whether the enclosing type is effectively final, we can filter
			fields.removeIf(TypeMemberDecl::isProtected);
			methods.removeIf(TypeMemberDecl::isProtected);
			constructors.removeIf(TypeMemberDecl::isProtected);
		}

		// Roughly §8.9
		if (isEnum(classAccess) && hasEnumConstantBody && !isFinal(classAccess))
			modifiers.add(Modifier.SEALED);

		if (isAnnotation(classAccess)) {
			typeDecl = new AnnotationDecl(className, visibility, modifiers, anns, location,
				fields, methods, enclosingType);
		} else if (isInterface(classAccess)) {
			typeDecl = new InterfaceDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
		} else if (isEnum(classAccess)) {
			typeDecl = new EnumDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, fields, methods, enclosingType, constructors);
		} else if (isRecord(classAccess)) {
			typeDecl = new RecordDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, constructors);
		} else {
			typeDecl = new ClassDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, superClass, constructors);
		}
	}

	private FieldDecl convertField(int access, String name, String descriptor, String signature,
	                               List<String> annotations) {
		ITypeReference fieldType;
		if (signature != null) {
			AsmSignatureVisitor.TypeVisitor<ITypeReference> visitor =
				new AsmSignatureVisitor.TypeVisitor<>(api, typeRefFactory);
			new SignatureReader(signature).accept(visitor);
			fieldType = visitor.getType();
		} else {
			fieldType = convertType(descriptor);
		}

		return new FieldDecl(String.format("%s.%s", className, name), convertVisibility(access),
			convertFieldModifiers(access), convertAnnotations(annotations), SourceLocation.NO_LOCATION,
			typeRefFactory.createTypeReference(className), fieldType);
	}

	private ConstructorDecl convertConstructor(int access, String descriptor, String signature, String[] exceptions,
	                                           List<String> annotations) {
		List<ParameterDecl> parameters;
		List<ITypeReference> thrownExceptions;
		List<FormalTypeParameter> formalTypeParameters;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, typeRefFactory);
			new SignatureReader(signature).accept(visitor);
			parameters = visitor.getParameters();
			formalTypeParameters = visitor.getFormalTypeParameters();
			thrownExceptions = visitor.getThrownExceptions().isEmpty()
				? convertThrownExceptions(exceptions)
				: visitor.getThrownExceptions();
		} else {
			// Constructors of inner non-static classes take their outer class as implicit first parameter
			Type[] originalParams = Type.getArgumentTypes(descriptor);
			parameters = (enclosingType != null && !isStatic(classAccess) && originalParams.length >= 1)
				? convertParameters(Arrays.copyOfRange(originalParams, 1, originalParams.length))
				: convertParameters(originalParams);
			formalTypeParameters = Collections.emptyList();
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		// Last parameter is a T[], but the API representation is T...
		if (isVarargs(access))
			parameters = convertVarargParameter(parameters);

		return new ConstructorDecl(String.format("%s.<init>", className), convertVisibility(access),
			convertMethodModifiers(access), convertAnnotations(annotations), SourceLocation.NO_LOCATION,
			typeRefFactory.createTypeReference(className),
			typeRefFactory.createTypeReference(className),
			parameters, formalTypeParameters, thrownExceptions);
	}

	private MethodDecl convertMethod(int access, String name, String descriptor, String signature, String[] exceptions,
	                                 List<String> annotations) {
		ITypeReference returnType;
		List<ParameterDecl> parameters;
		List<FormalTypeParameter> formalTypeParameters;
		List<ITypeReference> thrownExceptions;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, typeRefFactory);
			new SignatureReader(signature).accept(visitor);
			returnType = visitor.getReturnType();
			parameters = visitor.getParameters();
			formalTypeParameters = visitor.getFormalTypeParameters();
			thrownExceptions = visitor.getThrownExceptions().isEmpty()
				? convertThrownExceptions(exceptions)
				: visitor.getThrownExceptions();
		} else {
			returnType = convertType(Type.getReturnType(descriptor).getDescriptor());
			parameters = convertParameters(Type.getArgumentTypes(descriptor));
			formalTypeParameters = Collections.emptyList();
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		// Last parameter is a T[], but the API representation is T...
		if (isVarargs(access))
			parameters = convertVarargParameter(parameters);

		return new MethodDecl(String.format("%s.%s", className, name), convertVisibility(access),
			convertMethodModifiers(access), convertAnnotations(annotations), SourceLocation.NO_LOCATION,
			typeRefFactory.createTypeReference(className), returnType, parameters,
			formalTypeParameters, thrownExceptions);
	}

	private List<ParameterDecl> convertVarargParameter(List<ParameterDecl> parameters) {
		if (parameters.isEmpty())
			return Collections.emptyList();

		List<ParameterDecl> params = new ArrayList<>(parameters);
		ParameterDecl last = params.getLast();
		if (last.type() instanceof ArrayTypeReference atr) {
			// If this is a multi-dimensional array, remove one dimension, otherwise make it a regular reference
			if (atr.dimension() > 1)
				params.set(params.size() - 1, new ParameterDecl(last.name(),
					typeRefFactory.createArrayTypeReference(atr.componentType(), atr.dimension() - 1), true));
			else
				params.set(params.size() - 1, new ParameterDecl(last.name(), atr.componentType(), true));
		}
		return params;
	}

	private List<Annotation> convertAnnotations(List<String> annotationDescriptors) {
		return annotationDescriptors.stream()
			.map(ann -> new Annotation(typeRefFactory.createTypeReference(descriptorToFqn(ann))))
			.toList();
	}

	private List<ITypeReference> convertThrownExceptions(String[] exceptions) {
		if (exceptions == null)
			return Collections.emptyList();

		return Arrays.stream(exceptions)
			.map(e -> typeRefFactory.createTypeReference(bytecodeToFqn(e)))
			.map(ITypeReference.class::cast)
			.toList();
	}

	private ITypeReference convertType(String descriptor) {
		Type type = Type.getType(descriptor);
		if (type.getSort() == Type.ARRAY) {
			ITypeReference component = convertType(type.getElementType().getDescriptor());
			return typeRefFactory.createArrayTypeReference(component, type.getDimensions());
		} else if (type.getSort() == Type.OBJECT) {
			return typeRefFactory.createTypeReference(type.getClassName());
		} else {
			return typeRefFactory.createPrimitiveTypeReference(type.getClassName());
		}
	}

	private List<ParameterDecl> convertParameters(Type[] paramTypes) {
		return IntStream.range(0, paramTypes.length)
			.mapToObj(i -> new ParameterDecl("p" +  i, convertType(paramTypes[i].getDescriptor()), false))
			.toList();
	}

	private AccessModifier convertVisibility(int access) {
		if ((access & Opcodes.ACC_PUBLIC) != 0)    return AccessModifier.PUBLIC;
		if ((access & Opcodes.ACC_PROTECTED) != 0) return AccessModifier.PROTECTED;
		if ((access & Opcodes.ACC_PRIVATE) != 0)   return AccessModifier.PRIVATE;
		return AccessModifier.PACKAGE_PRIVATE;
	}

	private boolean isTypeMemberExported(int access) {
		AccessModifier visibility = convertVisibility(access);
		return visibility == AccessModifier.PUBLIC || visibility == AccessModifier.PROTECTED;
	}

	private boolean isEffectivelyFinal(int access) {
		// FIXME: non-sealed
		return isFinal(access) || isSealed || (isClass(classAccess) && !hasNonPrivateConstructor);
	}

	// FIXME: non-sealed?
	private EnumSet<Modifier> convertClassModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_FINAL) != 0)    modifiers.add(Modifier.FINAL);
		if ((access & Opcodes.ACC_ABSTRACT) != 0) modifiers.add(Modifier.ABSTRACT);
		if ((access & Opcodes.ACC_STATIC) != 0)   modifiers.add(Modifier.STATIC); // shouldn't work on classes, but it does?
		return modifiers;
	}

	private EnumSet<Modifier> convertFieldModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_STATIC) != 0)    modifiers.add(Modifier.STATIC);
		if ((access & Opcodes.ACC_FINAL) != 0)     modifiers.add(Modifier.FINAL);
		if ((access & Opcodes.ACC_VOLATILE) != 0)  modifiers.add(Modifier.VOLATILE);
		if ((access & Opcodes.ACC_TRANSIENT) != 0) modifiers.add(Modifier.TRANSIENT);
		return modifiers;
	}

	private EnumSet<Modifier> convertMethodModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_STATIC) != 0)       modifiers.add(Modifier.STATIC);
		if ((access & Opcodes.ACC_FINAL) != 0)        modifiers.add(Modifier.FINAL);
		if ((access & Opcodes.ACC_ABSTRACT) != 0)     modifiers.add(Modifier.ABSTRACT);
		if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) modifiers.add(Modifier.SYNCHRONIZED);
		if ((access & Opcodes.ACC_NATIVE) != 0)       modifiers.add(Modifier.NATIVE);
		if ((access & Opcodes.ACC_STRICT) != 0)       modifiers.add(Modifier.STRICTFP);
		if (isDefault(access))                        modifiers.add(Modifier.DEFAULT);
		return modifiers;
	}

	private String bytecodeToFqn(String bytecodeName) {
		return bytecodeName.replace('/', '.');
	}

	private String descriptorToFqn(String descriptor) {
		return Type.getType(descriptor).getClassName();
	}

	private boolean isSynthetic(int access) {
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}

	private boolean isBridge(int access) {
		return (access & Opcodes.ACC_BRIDGE) != 0;
	}

	private boolean isEnum(int access) {
		return (access & Opcodes.ACC_ENUM) != 0;
	}

	private boolean isRecord(int access) {
		return (access & Opcodes.ACC_RECORD) != 0;
	}

	private boolean isClass(int access) {
		return !isEnum(access) && !isRecord(access) && !isInterface(access) && !isAnnotation(access);
	}

	private boolean isAnnotation(int access) {
		return (access & Opcodes.ACC_ANNOTATION) != 0;
	}

	private boolean isInterface(int access) {
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}

	private boolean isVarargs(int access) {
		return (access & Opcodes.ACC_VARARGS) != 0;
	}

	private boolean isStatic(int access) {
		return (access & Opcodes.ACC_STATIC) != 0;
	}

	private boolean isFinal(int access) {
		return (access & Opcodes.ACC_FINAL) != 0;
	}

	// No Opcodes.ACC_DEFAULT, so that's how we infer it
	private boolean isDefault(int access) {
		return (classAccess & Opcodes.ACC_INTERFACE) != 0 &&
			(access & Opcodes.ACC_ABSTRACT) == 0 &&
			(access & Opcodes.ACC_STATIC) == 0 &&
			(access & Opcodes.ACC_PRIVATE) == 0;
	}
}
