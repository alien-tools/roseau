package io.github.alien.roseau.extractors.asm;

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

import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

final class AsmClassVisitor extends ClassVisitor {
	private final TypeReferenceFactory typeRefFactory;
	private String className;
	private Path sourceFile;
	private int classAccess;
	private TypeDecl typeDecl;
	private TypeReference<TypeDecl> enclosingType;
	private TypeReference<ClassDecl> superClass;
	private List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
	private final List<FieldDecl> fields = new ArrayList<>();
	private final List<MethodDecl> methods = new ArrayList<>();
	private final List<AnnotationMethodDecl> annotationMethods = new ArrayList<>();
	private final Set<ElementType> targets = new HashSet<>();
	private final List<ConstructorDecl> constructors = new ArrayList<>();
	private List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	private final List<TypeReference<TypeDecl>> permittedTypes = new ArrayList<>();
	private final List<AnnotationData> annotations = new ArrayList<>();
	private boolean hasNonPrivateConstructor;
	private boolean hasEnumConstantBody;
	private boolean shouldSkip;
	private static final Pattern anonymousMatcher = Pattern.compile("\\$\\d+");

	record AnnotationData(String descriptor, Map<String, String> values) {
		AnnotationData(String descriptor) {
			this(descriptor, new HashMap<>());
		}
	}

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
				if (className != null) {
					sourceFile = Path.of(className.replace('.', '/')).resolveSibling(source);
				} else {
					sourceFile = Path.of(source);
				}
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
			private final List<AnnotationData> annotations = new ArrayList<>();

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				AnnotationData annotationData = new AnnotationData(descriptor);
				annotations.add(annotationData);
				return new AnnotationVisitor(api) {
					@Override
					public void visit(String name, Object value) {
						annotationData.values().put(name, formatAnnotationValue(value));
					}

					@Override
					public void visitEnum(String name, String descriptor, String value) {
						annotationData.values().put(name, descriptorToFqn(descriptor) + "." + value);
					}
				};
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
			private boolean hasDefault;
			private final List<AnnotationData> annotations = new ArrayList<>();
			private int firstLine = -1;

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				AnnotationData annotationData = new AnnotationData(descriptor);
				annotations.add(annotationData);
				return new AnnotationVisitor(api) {
					@Override
					public void visit(String name, Object value) {
						annotationData.values().put(name, formatAnnotationValue(value));
					}

					@Override
					public void visitEnum(String name, String descriptor, String value) {
						annotationData.values().put(name, descriptorToFqn(descriptor) + "." + value);
					}
				};
			}

			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				hasDefault = true;
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
				} else if (isAnnotation(classAccess)) {
					annotationMethods.add(convertAnnotationMethod(name, descriptor, signature, annotations, firstLine, hasDefault));
				} else {
					methods.add(convertMethod(access, name, descriptor, signature, exceptions, annotations, firstLine));
				}
			}
		};
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		if (!shouldSkip) {
			// FIXME: no support for NON_SEALED in ASM yet
			permittedTypes.add(typeRefFactory.createTypeReference(bytecodeToFqn(permittedSubclass)));
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// Constant bodies are inner classes? Though this is not 100% accurate
		hasEnumConstantBody = true;

		if (shouldSkip || !bytecodeToFqn(name).equals(className)) {
			return;
		}

		if (outerName != null && innerName != null && !anonymousMatcher.matcher(outerName).find()) {
			// Nested/inner types
			// Merge the kind bits (class/interface/enum/annotation/record) from the class header with
			// the visibility/modifier bits from the InnerClasses entry. Some compilers omit ACC_RECORD
			// in the InnerClasses attributes for nested records, which would make us misclassify them.
			int kindBits = Opcodes.ACC_INTERFACE | Opcodes.ACC_ENUM | Opcodes.ACC_ANNOTATION | Opcodes.ACC_RECORD;
			classAccess = (access & ~kindBits) | (classAccess & kindBits);
			enclosingType = typeRefFactory.createTypeReference(bytecodeToFqn(outerName));
		} else {
			// Anonymous/local types
			shouldSkip = true;
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (!shouldSkip) {
			AnnotationData annotationData = new AnnotationData(descriptor);
			annotations.add(annotationData);

			return new AnnotationVisitor(api) {
				@Override
				public void visit(String name, Object value) {
					annotationData.values().put(name, formatAnnotationValue(value));
				}

				@Override
				public void visitEnum(String name, String descriptor, String value) {
					annotationData.values().put(name, descriptorToFqn(descriptor) + "." + value);
					if ("Ljava/lang/annotation/ElementType;".equals(descriptor)) {
						targets.add(ElementType.valueOf(value));
					}
				}

				@Override
				public AnnotationVisitor visitArray(String name) {
					if ("value".equals(name) && "Ljava/lang/annotation/Target;".equals(descriptor)) {
						return new AnnotationVisitor(super.api) {
							@Override
							public void visitEnum(String name, String descriptor, String value) {
								if ("Ljava/lang/annotation/ElementType;".equals(descriptor)) {
									targets.add(ElementType.valueOf(value));
								}
							}
						};
					}
					return super.visitArray(name);
				}
			};
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
		SourceLocation location = new SourceLocation(sourceFile, -1);

		if (!permittedTypes.isEmpty()) {
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
				fields, annotationMethods, enclosingType, targets);
		} else if (isInterface(classAccess)) {
			typeDecl = new InterfaceDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, permittedTypes);
		} else if (isEnum(classAccess)) {
			typeDecl = new EnumDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, fields, methods, enclosingType, constructors, List.of());
		} else if (isRecord(classAccess)) {
			typeDecl = new RecordDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, constructors, List.of());
		} else {
			typeDecl = new ClassDecl(className, visibility, modifiers, anns, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType,
				superClass, constructors, permittedTypes);
		}
	}

	private FieldDecl convertField(int access, String name, String descriptor, String signature,
	                               List<AnnotationData> annotations) {
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
		SourceLocation location = new SourceLocation(sourceFile, -1);
		return new FieldDecl(String.format("%s.%s", className, name), convertVisibility(access),
			convertFieldModifiers(access), convertAnnotations(annotations), location,
			typeRefFactory.createTypeReference(className), fieldType);
	}

	private ConstructorDecl convertConstructor(int access, String descriptor, String signature, String[] exceptions,
	                                           List<AnnotationData> annotations, int line) {
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
			convertMethodModifiers(access), convertAnnotations(annotations), new SourceLocation(sourceFile, line),
			typeRefFactory.createTypeReference(className),
			typeRefFactory.createTypeReference(className),
			parameters, typeParameters, thrownExceptions);
	}

	private MethodDecl convertMethod(int access, String name, String descriptor, String signature, String[] exceptions,
	                                 List<AnnotationData> annotations, int line) {
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
			convertMethodModifiers(access), convertAnnotations(annotations), new SourceLocation(sourceFile, line),
			typeRefFactory.createTypeReference(className), returnType, parameters,
			typeParameters, thrownExceptions);
	}

	private AnnotationMethodDecl convertAnnotationMethod(String name, String descriptor, String signature,
	                                                     List<AnnotationData> annotations, int line, boolean hasDefault) {
		ITypeReference returnType;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, typeRefFactory);
			new SignatureReader(signature).accept(visitor);
			returnType = visitor.getReturnType();
		} else {
			returnType = convertType(Type.getReturnType(descriptor).getDescriptor());
		}

		return new AnnotationMethodDecl(String.format("%s.%s", className, name), convertAnnotations(annotations),
			new SourceLocation(sourceFile, line), typeRefFactory.createTypeReference(className), returnType, hasDefault);
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

	private List<Annotation> convertAnnotations(List<AnnotationData> annotationDataList) {
		return annotationDataList.stream()
			.map(data -> new Annotation(
				typeRefFactory.createTypeReference(descriptorToFqn(data.descriptor())),
				data.values()))
			.toList();
	}

	private static String formatAnnotationValue(Object value) {
		return value.toString();
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
		return isFinal(access) || !permittedTypes.isEmpty() || (isClass(classAccess) && !hasNonPrivateConstructor);
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
