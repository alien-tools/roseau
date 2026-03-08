package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.extractors.ExtractorSink;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import java.lang.annotation.ElementType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;

public final class AsmClassVisitor extends ClassVisitor {
	private final ExtractorSink sink;
	private final ApiFactory factory;
	private String className;
	private Path sourceFile;
	private int classAccess;
	private TypeReference<TypeDecl> enclosingType;
	private TypeReference<ClassDecl> superClass;
	private boolean hasAccessibleConstructor;
	private boolean hasEnumConstantBody;
	private boolean shouldSkip;
	private final Set<TypeReference<InterfaceDecl>> implementedInterfaces = new LinkedHashSet<>();
	private final Set<FieldDecl> fields = new LinkedHashSet<>();
	private final Set<MethodDecl> methods = new LinkedHashSet<>();
	private final Set<AnnotationMethodDecl> annotationMethods = new LinkedHashSet<>();
	private final Set<ElementType> targets = new LinkedHashSet<>();
	private final Set<ConstructorDecl> constructors = new LinkedHashSet<>();
	private final List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	private final Set<TypeReference<TypeDecl>> permittedTypes = new LinkedHashSet<>();
	private final Set<AsmAnnotationVisitor.Data> annotations = new LinkedHashSet<>();

	public AsmClassVisitor(int api, ExtractorSink sink, ApiFactory factory) {
		super(api);
		this.sink = sink;
		this.factory = factory;
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
	public ModuleVisitor visitModule(String name, int access, String version) {
		return new ModuleVisitor(api) {
			private Set<String> exports = new LinkedHashSet<>();

			@Override
			public void visitExport(String pkg, int pkgAccess, String... modules) {
				// modules correspond to qualified exports
				if (modules == null || modules.length == 0) {
					exports.add(bytecodeToFqn(pkg));
				}
			}

			@Override
			public void visitEnd() {
				sink.accept(factory.createModule(name, exports));
			}
		};
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = bytecodeToFqn(name);
		classAccess = access;

		if (isSynthetic(classAccess) || className.endsWith("package-info") || className.endsWith("module-info")) {
			shouldSkip = true;
			return;
		}

		if (signature != null) {
			SignatureReader reader = new SignatureReader(signature);
			AsmSignatureVisitor signatureVisitor = new AsmSignatureVisitor(api, factory);
			reader.accept(signatureVisitor);
			superClass = signatureVisitor.getSuperclass();
			implementedInterfaces.addAll(signatureVisitor.getSuperInterfaces());
			formalTypeParameters.addAll(signatureVisitor.getFormalTypeParameters());
		} else {
			if (superName != null) {
				superClass = factory.references().createTypeReference(bytecodeToFqn(superName));
			}
			implementedInterfaces.addAll(Arrays.stream(interfaces)
				.map(AsmClassVisitor::bytecodeToFqn)
				.map(factory.references()::<InterfaceDecl>createTypeReference)
				.collect(toSet()));
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (shouldSkip || isSynthetic(access) || !isTypeMemberExported(access)) {
			return null;
		}

		return new FieldVisitor(api) {
			private final Set<AsmAnnotationVisitor.Data> annotations = new LinkedHashSet<>();

			@Override
			public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
				AsmAnnotationVisitor.Data data = new AsmAnnotationVisitor.Data(annotationDescriptor);
				annotations.add(data);
				return new AsmAnnotationVisitor(api, annotationDescriptor, data);
			}

			@Override
			public void visitEnd() {
				fields.add(convertField(access, name, descriptor, signature, annotations));
			}
		};
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (isAccessibleConstructor(name, access)) {
			hasAccessibleConstructor = true;
		}

		if (shouldSkip || isSynthetic(access) || isBridge(access) || !isTypeMemberExported(access)) {
			return null;
		}

		return new MethodVisitor(api) {
			private final Set<AsmAnnotationVisitor.Data> annotations = new LinkedHashSet<>();
			private boolean hasDefault;
			private int firstLine = -1;

			@Override
			public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
				AsmAnnotationVisitor.Data data = new AsmAnnotationVisitor.Data(annotationDescriptor);
				annotations.add(data);
				return new AsmAnnotationVisitor(api, annotationDescriptor, data);
			}

			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				hasDefault = true;
				return null;
			}

			@Override
			public void visitLineNumber(int line, Label start) {
				firstLine = firstLine > 0 ? Math.min(firstLine, line) : line;
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
			// No Opcodes.ACC_NON_SEALED in ASM yet
			permittedTypes.add(factory.references().createTypeReference(bytecodeToFqn(permittedSubclass)));
		}
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// FIXME: Constant bodies are inner classes
		hasEnumConstantBody = isEnum(classAccess);

		if (shouldSkip || !bytecodeToFqn(name).equals(className)) {
			return;
		}

		if (outerName != null && innerName != null) {
			// Nested/inner types
			// Merge the kind bits (class/interface/enum/annotation/record) from the class header with
			// the visibility/modifier bits from the InnerClasses entry. Some compilers omit ACC_RECORD
			// in the InnerClasses attributes for nested records, which would make us misclassify them.
			int kindBits = Opcodes.ACC_INTERFACE | Opcodes.ACC_ENUM | Opcodes.ACC_ANNOTATION | Opcodes.ACC_RECORD;
			classAccess = (access & ~kindBits) | (classAccess & kindBits);
			enclosingType = factory.references().createTypeReference(bytecodeToFqn(outerName));
		} else {
			// Anonymous/local types
			shouldSkip = true;
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (shouldSkip) {
			return null;
		}

		AsmAnnotationVisitor.Data data = new AsmAnnotationVisitor.Data(descriptor);
		annotations.add(data);
		return new AsmAnnotationVisitor(api, descriptor, data, targets);
	}

	@Override
	public void visitEnd() {
		if (shouldSkip) {
			return;
		}

		AccessModifier visibility = convertVisibility(classAccess);
		Set<Modifier> modifiers = convertClassModifiers(classAccess);
		Set<Annotation> anns = convertAnnotations(annotations);
		SourceLocation location = factory.location(sourceFile, -1);

		if (isEffectivelyFinal(classAccess)) {
			// We initially included all PUBLIC/PROTECTED type members
			// Now that we finally know whether the enclosing type is effectively final, we can filter
			fields.removeIf(TypeMemberDecl::isProtected);
			methods.removeIf(TypeMemberDecl::isProtected);
			constructors.removeIf(TypeMemberDecl::isProtected);
		}

		// §8.9: an enum class E is implicitly sealed if its declaration contains at least one
		// enum constant that has a class body. Otherwise, final.
		if (isEnum(classAccess)) {
			if (hasEnumConstantBody && !isFinal(classAccess)) {
				modifiers.add(Modifier.SEALED);
			} else {
				modifiers.add(Modifier.FINAL);
			}
		}

		if (isAnnotation(classAccess)) {
			sink.accept(factory.createAnnotation(className, visibility, modifiers, anns, location, fields, annotationMethods,
				enclosingType, targets));
		} else if (isInterface(classAccess)) {
			sink.accept(factory.createInterface(className, visibility, modifiers, anns, location, implementedInterfaces,
				formalTypeParameters, fields, methods, enclosingType, permittedTypes));
		} else if (isEnum(classAccess)) {
			sink.accept(factory.createEnum(className, visibility, modifiers, anns, location, implementedInterfaces,
				fields, methods, enclosingType, constructors));
		} else if (isRecord(classAccess)) {
			sink.accept(factory.createRecord(className, visibility, modifiers, anns, location, implementedInterfaces,
				formalTypeParameters, fields, methods, enclosingType, constructors));
		} else {
			sink.accept(factory.createClass(className, visibility, modifiers, anns, location, implementedInterfaces,
				formalTypeParameters, fields, methods, enclosingType, superClass, constructors, permittedTypes));
		}
	}

	private FieldDecl convertField(int access, String name, String descriptor, String signature,
	                               Set<AsmAnnotationVisitor.Data> annotations) {
		ITypeReference fieldType;
		if (signature != null) {
			AsmTypeSignatureVisitor<ITypeReference> visitor = new AsmTypeSignatureVisitor<>(api, factory.references());
			new SignatureReader(signature).accept(visitor);
			fieldType = visitor.getType();
		} else {
			fieldType = convertType(descriptor);
		}
		SourceLocation location = factory.location(sourceFile, -1);
		return factory.createField(className + "." + name, convertVisibility(access), convertFieldModifiers(access),
			convertAnnotations(annotations), location, factory.references().createTypeReference(className), fieldType);
	}

	private ConstructorDecl convertConstructor(int access, String descriptor, String signature, String[] exceptions,
	                                           Set<AsmAnnotationVisitor.Data> annotations, int line) {
		List<ParameterDecl> parameters;
		Set<ITypeReference> thrownExceptions;
		List<FormalTypeParameter> typeParameters;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, factory);
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
			typeParameters = List.of();
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		// Last parameter is a T[], but the API representation is T...
		if (isVarargs(access)) {
			parameters = convertVarargParameter(parameters);
		}

		return factory.createConstructor(String.format("%s.<init>", className), convertVisibility(access),
			convertMethodModifiers(access), convertAnnotations(annotations), factory.location(sourceFile, line),
			factory.references().createTypeReference(className), factory.references().createTypeReference(className),
			parameters, typeParameters, thrownExceptions);
	}

	private MethodDecl convertMethod(int access, String name, String descriptor, String signature, String[] exceptions,
	                                 Set<AsmAnnotationVisitor.Data> annotations, int line) {
		ITypeReference returnType;
		List<ParameterDecl> parameters;
		List<FormalTypeParameter> typeParameters;
		Set<ITypeReference> thrownExceptions;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, factory);
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
			typeParameters = List.of();
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		// Last parameter is a T[], but the API representation is T...
		if (isVarargs(access)) {
			parameters = convertVarargParameter(parameters);
		}

		return factory.createMethod(String.format("%s.%s", className, name), convertVisibility(access),
			convertMethodModifiers(access), convertAnnotations(annotations), factory.location(sourceFile, line),
			factory.references().createTypeReference(className), returnType, parameters,
			typeParameters, thrownExceptions);
	}

	private AnnotationMethodDecl convertAnnotationMethod(String name, String descriptor, String signature,
	                                                     Set<AsmAnnotationVisitor.Data> annotations, int line,
	                                                     boolean hasDefault) {
		ITypeReference returnType;

		if (signature != null) {
			AsmSignatureVisitor visitor = new AsmSignatureVisitor(api, factory);
			new SignatureReader(signature).accept(visitor);
			returnType = visitor.getReturnType();
		} else {
			returnType = convertType(Type.getReturnType(descriptor).getDescriptor());
		}

		return factory.createAnnotationMethod(String.format("%s.%s", className, name), convertAnnotations(annotations),
			factory.location(sourceFile, line), factory.references().createTypeReference(className), returnType, hasDefault);
	}

	private List<ParameterDecl> convertVarargParameter(List<ParameterDecl> parameters) {
		if (parameters.isEmpty()) {
			return List.of();
		}

		List<ParameterDecl> params = new ArrayList<>(parameters);
		ParameterDecl last = params.getLast();
		if (last.type() instanceof ArrayTypeReference(ITypeReference componentType, int dimension)) {
			// If this is a multidimensional array, remove one dimension, otherwise make it a regular reference
			if (dimension > 1) {
				params.set(params.size() - 1, factory.createParameter(last.name(),
					factory.references().createArrayTypeReference(componentType, dimension - 1), true));
			} else {
				params.set(params.size() - 1, factory.createParameter(last.name(), componentType, true));
			}
		}
		return params;
	}

	private Set<Annotation> convertAnnotations(Set<AsmAnnotationVisitor.Data> dataList) {
		return dataList.stream()
			.map(data -> factory.createAnnotation(
				factory.references().createTypeReference(descriptorToFqn(data.descriptor())),
				data.values()))
			.collect(toSet());
	}

	private Set<ITypeReference> convertThrownExceptions(String[] exceptions) {
		if (exceptions == null) {
			return Set.of();
		}

		return Arrays.stream(exceptions)
			.map(e -> factory.references().createTypeReference(bytecodeToFqn(e)))
			.collect(toSet());
	}

	private ITypeReference convertType(String descriptor) {
		Type type = Type.getType(descriptor);
		if (type.getSort() == Type.ARRAY) {
			ITypeReference component = convertType(type.getElementType().getDescriptor());
			return factory.references().createArrayTypeReference(component, type.getDimensions());
		} else if (type.getSort() == Type.OBJECT) {
			return factory.references().createTypeReference(type.getClassName());
		} else {
			return factory.references().createPrimitiveTypeReference(type.getClassName());
		}
	}

	private List<ParameterDecl> convertParameters(Type[] paramTypes) {
		return IntStream.range(0, paramTypes.length)
			.mapToObj(i -> factory.createParameter("p" + i, convertType(paramTypes[i].getDescriptor()), false))
			.toList();
	}

	private static boolean isTypeMemberExported(int access) {
		AccessModifier visibility = convertVisibility(access);
		return visibility == AccessModifier.PUBLIC || visibility == AccessModifier.PROTECTED;
	}

	private boolean isEffectivelyFinal(int access) {
		if (isEnum(access) || isRecord(access)) {
			return true;
		}
		return isFinal(access) || !permittedTypes.isEmpty() || (isClass(classAccess) && !hasAccessibleConstructor);
	}

	private static String bytecodeToFqn(String bytecodeName) {
		return bytecodeName.replace('/', '.');
	}

	private static String descriptorToFqn(String descriptor) {
		return Type.getType(descriptor).getClassName();
	}

	private boolean isAccessibleConstructor(String name, int access) {
		return "<init>".equals(name) && !isSynthetic(access) && convertVisibility(access) != AccessModifier.PRIVATE;
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

	private static AccessModifier convertVisibility(int access) {
		return switch (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) {
			case Opcodes.ACC_PUBLIC -> AccessModifier.PUBLIC;
			case Opcodes.ACC_PROTECTED -> AccessModifier.PROTECTED;
			case Opcodes.ACC_PRIVATE -> AccessModifier.PRIVATE;
			default -> AccessModifier.PACKAGE_PRIVATE;
		};
	}

	private static Set<Modifier> convertClassModifiers(int access) {
		Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if (isFinal(access)) {
			modifiers.add(Modifier.FINAL);
		}
		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			modifiers.add(Modifier.ABSTRACT);
		}
		if ((access & Opcodes.ACC_STATIC) != 0) {
			modifiers.add(Modifier.STATIC);
		}
		return modifiers;
	}

	private static Set<Modifier> convertFieldModifiers(int access) {
		Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
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

	private Set<Modifier> convertMethodModifiers(int access) {
		Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
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

	// No Opcodes.ACC_DEFAULT, so that's how we infer it
	private boolean isDefault(int access) {
		return (classAccess & Opcodes.ACC_INTERFACE) != 0 &&
			(access & Opcodes.ACC_ABSTRACT) == 0 &&
			(access & Opcodes.ACC_STATIC) == 0 &&
			(access & Opcodes.ACC_PRIVATE) == 0;
	}
}
