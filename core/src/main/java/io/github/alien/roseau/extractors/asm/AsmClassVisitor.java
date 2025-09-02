package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;

final class AsmClassVisitor extends ClassVisitor {
	private final TypeReferenceFactory typeRefFactory;
	private String className;
	private Path sourceFile = Path.of("<unknown>");
	private int classAccess;
	private TypeDecl typeDecl;
	private TypeReference<TypeDecl> enclosingType;
	private TypeReference<ClassDecl> superClass;
	private List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
	private final List<FieldDecl> fields = new ArrayList<>();
	private final List<MethodDecl> methods = new ArrayList<>();
	private final List<ConstructorDecl> constructors = new ArrayList<>();
	private List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	private final List<String> annotations = new ArrayList<>();
	private boolean isSealed;
	private boolean hasNonPrivateConstructor;
	private boolean hasEnumConstantBody;
	private boolean shouldSkip;

	private static final Logger LOGGER = LogManager.getLogger(AsmClassVisitor.class);

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
		if (source != null) {
			if (source.endsWith(".java")) {
				sourceFile = Path.of(source);
			} else {
				shouldSkip = true;
			}
		}
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
			if (superName != null) {
				superClass = typeRefFactory.createTypeReference(bytecodeToFqn(superName));
			}
			implementedInterfaces = Arrays.stream(interfaces)
				.map(AsmClassVisitor::bytecodeToFqn)
				.map(typeRefFactory::<InterfaceDecl>createTypeReference)
				.toList();
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (shouldSkip) {
			return null;
		}

		if (isSynthetic(access)) {
			LOGGER.trace("Skipping synthetic field {}", name);
			return null;
		}

		if (!isTypeMemberExported(access)) {
			LOGGER.trace("Skipping unexported field {}", name);
			return null;
		}

		return new FieldVisitor(api) {
			private final List<String> annotations = new ArrayList<>();

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
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (shouldSkip) {
			return null;
		}

		if (isSynthetic(access) || isBridge(access)) {
			LOGGER.trace("Skipping synthetic/bridge method {}", name);
			return null;
		}

		if ("<init>".equals(name) && convertVisibility(access) != AccessModifier.PRIVATE) {
			hasNonPrivateConstructor = true;
		}

		if (!isTypeMemberExported(access)) {
			LOGGER.trace("Skipping unexported method {}", name);
			return null;
		}

		// Stupid heuristics below to mark non-source but non-synthetic stuff
		boolean isEnumValues = "values".equals(name) && descriptor.startsWith("()[L");
		boolean isEnumValueOf = "valueOf".equals(name) && descriptor.startsWith("(Ljava/lang/String;)L");
		if (isEnum(classAccess) && (isEnumValues || isEnumValueOf)) {
			LOGGER.trace("Skipping {}'s values()/valueOf()", className);
			return null;
		}

		boolean isRecordToString = "toString".equals(name) && "()Ljava/lang/String;".equals(descriptor);
		boolean isRecordEquals = "equals".equals(name) && "(Ljava/lang/Object;)Z".equals(descriptor);
		boolean isRecordHashCode = "hashCode".equals(name) && "()I".equals(descriptor);
		if (isRecord(classAccess) && (isRecordToString || isRecordEquals || isRecordHashCode)) {
			LOGGER.trace("Skipping {}'s toString()/hashCode()/equals()", className);
			return null;
		}

		return new MethodVisitor(api) {
			private final List<String> annotations = new ArrayList<>();
			private int firstLine = -1;

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				annotations.add(descriptor);
				return null;
			}

			@Override
			public void visitLineNumber(int line, Label start) {
				firstLine = firstLine > 0 ? Math.min(firstLine, line) : line;
				super.visitLineNumber(line, start);
			}

			@Override
			public void visitEnd() {
				if ("<init>".equals(name)) {
					constructors.add(convertConstructor(access, descriptor, signature, exceptions, annotations, firstLine));
				} else {
					methods.add(convertMethod(access, name, descriptor, signature, exceptions, annotations, firstLine));
				}
			}
		};
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		if (!shouldSkip) {
			// Roseau's current API model does not care about the list of permitted subclasses,
			// but we need to know whether the class is sealed or not, and there is no ACC_SEALED in ASM
			isSealed = true;
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// Constant bodies are inner classes? Though this is not 100% accurate
		hasEnumConstantBody = true;

		if (shouldSkip || !bytecodeToFqn(name).equals(className)) {
			return;
		}

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
		if (!shouldSkip) {
			annotations.add(descriptor);
		}
		return null;
	}

	@Override
	public void visitEnd() {
		if (shouldSkip) {
			return;
		}

		AccessModifier visibility = convertVisibility(classAccess);
		EnumSet<Modifier> modifiers = convertClassModifiers(classAccess);
		List<Annotation> anns = convertAnnotations(annotations);
		// No bullet-proof line information for types
		SourceLocation location = new SourceLocation(sourceFile, -1, -1);

		if (isSealed) {
			modifiers.add(Modifier.SEALED);
		}

		if (isEffectivelyFinal(classAccess)) {
			// We initially included all PUBLIC/PROTECTED type members
			// Now that we finally know whether the enclosing type is effectively final, we can filter
			fields.removeIf(TypeMemberDecl::isProtected);
			methods.removeIf(TypeMemberDecl::isProtected);
			constructors.removeIf(TypeMemberDecl::isProtected);
		}

		// Roughly §8.9
		if (isEnum(classAccess) && hasEnumConstantBody && !isFinal(classAccess)) {
			modifiers.add(Modifier.SEALED);
		}

		if (isAnnotation(classAccess)) {
			typeDecl = new AnnotationDecl(className, visibility, modifiers, anns, location,
				fields, methods, enclosingType);
		} else if (isInterface(classAccess)) {
			typeDecl = new InterfaceDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, List.of());
		} else if (isEnum(classAccess)) {
			typeDecl = new EnumDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, fields, methods, enclosingType, constructors, List.of());
		} else if (isRecord(classAccess)) {
			typeDecl = new RecordDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, constructors, List.of());
		} else {
			typeDecl = new ClassDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, superClass, constructors, List.of());
		}
	}

	private FieldDecl convertField(int access, String name, String descriptor, String signature,
	                               List<String> annotations) {
		ITypeReference fieldType;
		if (signature != null) {
			AsmTypeSignatureVisitor<ITypeReference> visitor =
				new AsmTypeSignatureVisitor<>(api, typeRefFactory);
			new SignatureReader(signature).accept(visitor);
			fieldType = visitor.getType();
		} else {
			fieldType = convertType(descriptor);
		}

		// No bullet-proof line information for fields
		SourceLocation location = new SourceLocation(sourceFile, -1, -1);
		return new FieldDecl(String.format("%s.%s", className, name), convertVisibility(access),
			convertFieldModifiers(access), convertAnnotations(annotations), location,
			typeRefFactory.createTypeReference(className), fieldType);
	}

	private ConstructorDecl convertConstructor(int access, String descriptor, String signature, String[] exceptions,
	                                           List<String> annotations, int line) {
		List<ParameterDecl> parameters;
		List<ITypeReference> thrownExceptions;
		List<FormalTypeParameter> typeParameters;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, typeRefFactory);
			new SignatureReader(signature).accept(visitor);
			parameters = visitor.getParameters();
			typeParameters = visitor.getFormalTypeParameters();
			thrownExceptions = visitor.getThrownExceptions().isEmpty()
				? convertThrownExceptions(exceptions)
				: visitor.getThrownExceptions();
		} else {
			// Constructors of inner non-static classes take their outer class as implicit first parameter
			Type[] originalParams = Type.getArgumentTypes(descriptor);
			parameters = (enclosingType != null && !isStatic(classAccess) && originalParams.length >= 1)
				? convertParameters(Arrays.copyOfRange(originalParams, 1, originalParams.length))
				: convertParameters(originalParams);
			typeParameters = Collections.emptyList();
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		// Last parameter is a T[], but the API representation is T...
		if (isVarargs(access)) {
			parameters = convertVarargParameter(parameters);
		}

		return new ConstructorDecl(String.format("%s.<init>", className), convertVisibility(access),
			convertMethodModifiers(access), convertAnnotations(annotations), new SourceLocation(sourceFile, line, -1),
			typeRefFactory.createTypeReference(className),
			typeRefFactory.createTypeReference(className),
			parameters, typeParameters, thrownExceptions);
	}

	private MethodDecl convertMethod(int access, String name, String descriptor, String signature, String[] exceptions,
	                                 List<String> annotations, int line) {
		ITypeReference returnType;
		List<ParameterDecl> parameters;
		List<FormalTypeParameter> typeParameters;
		List<ITypeReference> thrownExceptions;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, typeRefFactory);
			new SignatureReader(signature).accept(visitor);
			returnType = visitor.getReturnType();
			parameters = visitor.getParameters();
			typeParameters = visitor.getFormalTypeParameters();
			thrownExceptions = visitor.getThrownExceptions().isEmpty()
				? convertThrownExceptions(exceptions)
				: visitor.getThrownExceptions();
		} else {
			returnType = convertType(Type.getReturnType(descriptor).getDescriptor());
			parameters = convertParameters(Type.getArgumentTypes(descriptor));
			typeParameters = Collections.emptyList();
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		// Last parameter is a T[], but the API representation is T...
		if (isVarargs(access)) {
			parameters = convertVarargParameter(parameters);
		}

		return new MethodDecl(String.format("%s.%s", className, name), convertVisibility(access),
			convertMethodModifiers(access), convertAnnotations(annotations), new SourceLocation(sourceFile, line, -1),
			typeRefFactory.createTypeReference(className), returnType, parameters,
			typeParameters, thrownExceptions);
	}

	private List<ParameterDecl> convertVarargParameter(List<ParameterDecl> parameters) {
		if (parameters.isEmpty()) {
			return Collections.emptyList();
		}

		List<ParameterDecl> params = new ArrayList<>(parameters);
		ParameterDecl last = params.getLast();
		if (last.type() instanceof ArrayTypeReference(ITypeReference componentType, int dimension)) {
			// If this is a multidimensional array, remove one dimension, otherwise make it a regular reference
			if (dimension > 1) {
				params.set(params.size() - 1, new ParameterDecl(last.name(),
					typeRefFactory.createArrayTypeReference(componentType, dimension - 1), true));
			} else {
				params.set(params.size() - 1, new ParameterDecl(last.name(), componentType, true));
			}
		}
		return params;
	}

	private List<Annotation> convertAnnotations(List<String> descriptors) {
		return descriptors.stream()
			.map(ann -> new Annotation(typeRefFactory.createTypeReference(descriptorToFqn(ann))))
			.toList();
	}

	private List<ITypeReference> convertThrownExceptions(String[] exceptions) {
		if (exceptions == null) {
			return Collections.emptyList();
		}

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
			.mapToObj(i -> new ParameterDecl("p" + i, convertType(paramTypes[i].getDescriptor()), false))
			.toList();
	}

	private static AccessModifier convertVisibility(int access) {
		if ((access & Opcodes.ACC_PUBLIC) != 0) {
			return AccessModifier.PUBLIC;
		}
		if ((access & Opcodes.ACC_PROTECTED) != 0) {
			return AccessModifier.PROTECTED;
		}
		if ((access & Opcodes.ACC_PRIVATE) != 0) {
			return AccessModifier.PRIVATE;
		}
		return AccessModifier.PACKAGE_PRIVATE;
	}

	private static boolean isTypeMemberExported(int access) {
		AccessModifier visibility = convertVisibility(access);
		return visibility == AccessModifier.PUBLIC || visibility == AccessModifier.PROTECTED;
	}

	private boolean isEffectivelyFinal(int access) {
		// FIXME: non-sealed
		return isFinal(access) || isSealed || (isClass(classAccess) && !hasNonPrivateConstructor);
	}

	private static EnumSet<Modifier> convertClassModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_FINAL) != 0) {
			modifiers.add(Modifier.FINAL);
		}
		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			modifiers.add(Modifier.ABSTRACT);
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			// shouldn't work on classes, but it does?
			modifiers.add(Modifier.STATIC);
		}
		return modifiers;
	}

	private static EnumSet<Modifier> convertFieldModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_STATIC) != 0) {
			modifiers.add(Modifier.STATIC);
		}
		if ((access & Opcodes.ACC_FINAL) != 0) {
			modifiers.add(Modifier.FINAL);
		}
		if ((access & Opcodes.ACC_VOLATILE) != 0) {
			modifiers.add(Modifier.VOLATILE);
		}
		if ((access & Opcodes.ACC_TRANSIENT) != 0) {
			modifiers.add(Modifier.TRANSIENT);
		}
		return modifiers;
	}

	private EnumSet<Modifier> convertMethodModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_STATIC) != 0) {
			modifiers.add(Modifier.STATIC);
		}
		if ((access & Opcodes.ACC_FINAL) != 0) {
			modifiers.add(Modifier.FINAL);
		}
		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			modifiers.add(Modifier.ABSTRACT);
		}
		if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
			modifiers.add(Modifier.SYNCHRONIZED);
		}
		if ((access & Opcodes.ACC_NATIVE) != 0) {
			modifiers.add(Modifier.NATIVE);
		}
		if ((access & Opcodes.ACC_STRICT) != 0) {
			modifiers.add(Modifier.STRICTFP);
		}
		if (isDefault(access)) {
			modifiers.add(Modifier.DEFAULT);
		}
		return modifiers;
	}

	private static String bytecodeToFqn(String bytecodeName) {
		return bytecodeName.replace('/', '.');
	}

	private static String descriptorToFqn(String descriptor) {
		return Type.getType(descriptor).getClassName();
	}

	private static boolean isSynthetic(int access) {
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}

	private static boolean isBridge(int access) {
		return (access & Opcodes.ACC_BRIDGE) != 0;
	}

	private static boolean isEnum(int access) {
		return (access & Opcodes.ACC_ENUM) != 0;
	}

	private static boolean isRecord(int access) {
		return (access & Opcodes.ACC_RECORD) != 0;
	}

	private static boolean isClass(int access) {
		return !isEnum(access) && !isRecord(access) && !isInterface(access) && !isAnnotation(access);
	}

	private static boolean isAnnotation(int access) {
		return (access & Opcodes.ACC_ANNOTATION) != 0;
	}

	private static boolean isInterface(int access) {
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}

	private static boolean isVarargs(int access) {
		return (access & Opcodes.ACC_VARARGS) != 0;
	}

	private static boolean isStatic(int access) {
		return (access & Opcodes.ACC_STATIC) != 0;
	}

	private static boolean isFinal(int access) {
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
