package com.github.maracas.roseau.api.extractors.jar;

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
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.IType;
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
import java.util.regex.Pattern;

class APIClassVisitor extends ClassVisitor {
	private final TypeReferenceFactory typeRefFactory;
	private String className;
	private int classAccess;
	private TypeDecl typeDecl;
	private TypeReference<ClassDecl> superClass;
	private List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
	private List<FieldDecl> fields = new ArrayList<>();
	private List<MethodDecl> methods = new ArrayList<>();
	private List<ConstructorDecl> constructors = new ArrayList<>();
	private List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	private List<String> annotations = new ArrayList<>();
	private boolean isSealed = false;
	private String filename;

	private static final Pattern ANONYMOUS_PATTERN = Pattern.compile(".*\\$\\d+.*");

	private static final Logger LOGGER = LogManager.getLogger();

	APIClassVisitor(int api, TypeReferenceFactory typeRefFactory) {
		super(api);
		this.typeRefFactory = typeRefFactory;
	}

	TypeDecl getTypeDecl() {
		return typeDecl;
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

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = bytecodeToFqn(name);
		classAccess = access;

		if (isSynthetic(classAccess)) {
			LOGGER.info("Skipping synthetic class %s", className);
			return;
		}

		if (signature != null) {
			SignatureReader reader = new SignatureReader(signature);
			APISignatureVisitor signatureVisitor = new APISignatureVisitor(api, typeRefFactory, false, "");
			reader.accept(signatureVisitor);
			formalTypeParameters = signatureVisitor.getFormalTypeParameters();
			// We don't want Object as explicit superclass
			if (!signatureVisitor.getSuperclass().getQualifiedName().equals("java.lang.Object"))
				superClass = signatureVisitor.getSuperclass();
			implementedInterfaces = signatureVisitor.getSuperInterfaces();
		} else {
			// We don't want Object as explicit superclass
			if (superName != null && !superName.equals("java/lang/Object"))
				superClass = typeRefFactory.createTypeReference(bytecodeToFqn(superName));

			implementedInterfaces = Arrays.stream(interfaces)
				.map(this::bytecodeToFqn)
				.map(typeRefFactory::<InterfaceDecl>createTypeReference)
				.toList();
		}

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return new FieldVisitor(api) {
			List<String> annotations = new ArrayList<>();

			@Override
			public AnnotationVisitor visitAnnotation(String fieldDescriptor, boolean visible) {
				annotations.add(descriptorToFqn(fieldDescriptor));
				return super.visitAnnotation(fieldDescriptor, visible);
			}

			@Override
			public void visitEnd() {
				if (isSynthetic(access)) {
					LOGGER.info("Skipping synthetic field %s", name);
					return;
				}

				if (isTypeMemberExported(access)) {
					List<Annotation> anns = annotations.stream()
						.map(ann -> new Annotation(
							typeRefFactory.createTypeReference(ann)))
						.toList();

					ITypeReference fieldType = null;
					if (signature != null) {
						APISignatureVisitor.TypeVisitor visitor = new APISignatureVisitor.TypeVisitor(
							api, typeRefFactory, false, "");
						new SignatureReader(signature).accept(visitor);
						fieldType = visitor.getType();
					} else {
						fieldType = convertType(descriptor);
					}

					fields.add(new FieldDecl(String.format("%s.%s", className, name), convertAccess(access),
						convertFieldModifiers(access), anns, SourceLocation.NO_LOCATION,
						typeRefFactory.createTypeReference(className), fieldType));
				}
			}
		};
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodVisitor(api) {
			List<String> annotations = new ArrayList<>();
			List<String> parameterNames = new ArrayList<>();

			@Override
			public void visitParameter(String name, int access) {
				parameterNames.add(name != null ? name : "p");
				super.visitParameter(name, access);
			}

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				annotations.add(descriptor);
				return super.visitAnnotation(descriptor, visible);
			}

			@Override
			public void visitLineNumber(int line, Label start) {
				System.out.println("Found " + name + " @ " + line + " (" + start + ")");
				super.visitLineNumber(line, start);
			}

			@Override
			public void visitEnd() {
				if ((access & Opcodes.ACC_BRIDGE) != 0 || (access & Opcodes.ACC_SYNTHETIC) != 0) {
					System.out.println("Bridge or synthetic method: " + name);
					return;
				}

				if (isTypeMemberExported(access)) {
					if (name.equals("<init>")) {
						constructors.add(convertConstructor(access, descriptor, signature, exceptions, parameterNames, annotations));
					} else {
						if ((classAccess & Opcodes.ACC_ENUM) != 0 && (name.equals("values") || name.equals("valueOf")))
							return;
						methods.add(convertMethod(access, name, descriptor, signature, exceptions, parameterNames, annotations));
					}
				}
			}
		};
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		// Roseau's current API model does not care about the list of permitted subclasses
		// but we need to know whether the class is sealed or not, and there is no ACC_SEALED in ASM
		isSealed = true;
		super.visitPermittedSubclass(permittedSubclass);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (bytecodeToFqn(name).equals(className)) {
			this.classAccess = access;
		}
		super.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		annotations.add(descriptor);
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public void visitSource(String source, String debug) {
		filename = source;
		super.visitSource(source, debug);
	}

	private ConstructorDecl convertConstructor(int access, String descriptor, String signature, String[] exceptions,
	                                           List<String> parameterNames, List<String> annotations) {
		boolean isVarargs = (access & Opcodes.ACC_VARARGS) != 0;

		ITypeReference returnType = null;
		List<ParameterDecl> parameters = Collections.emptyList();
		List<FormalTypeParameter> formalTypeParameters = Collections.emptyList();
		List<ITypeReference> thrownExceptions = Collections.emptyList();

		if (signature != null) {
			APISignatureVisitor visitor = new APISignatureVisitor(api, typeRefFactory, isVarargs, "");
			new SignatureReader(signature).accept(visitor);
			returnType = visitor.getReturnType();
			parameters = visitor.getParameters();
			formalTypeParameters = visitor.getFormalTypeParameters();
			thrownExceptions = visitor.getThrownExceptions();
		} else {
			returnType = convertType(Type.getReturnType(descriptor).getDescriptor());
			Type[] originalParams = Type.getArgumentTypes(descriptor);
			parameters = className.contains("$") && originalParams.length >= 1
				? convertParameters(Arrays.copyOfRange(originalParams, 1, originalParams.length), parameterNames, isVarargs)
				: convertParameters(originalParams, parameterNames, isVarargs);
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		// Constructors should return the type of the type they construct to match with sources extraction
		// Constructors of inner classes take their outer class as argument?
		return new ConstructorDecl(String.format("%s.<init>", className), convertAccess(access),
			convertMethodModifiers(access), convertAnnotations(annotations), SourceLocation.NO_LOCATION,
			typeRefFactory.createTypeReference(className),
			typeRefFactory.createTypeReference(className),
			parameters, formalTypeParameters, thrownExceptions);
	}

	private MethodDecl convertMethod(int access, String name, String descriptor, String signature, String[] exceptions,
	                                 List<String> parameterNames, List<String> annotations) {
		boolean isVarargs = (access & Opcodes.ACC_VARARGS) != 0;
		boolean isDefault = (classAccess & Opcodes.ACC_INTERFACE) != 0
			&& (access & Opcodes.ACC_ABSTRACT) == 0
			&& (access & Opcodes.ACC_STATIC) == 0
			&& (access & Opcodes.ACC_PRIVATE) == 0;
		EnumSet<Modifier> mods = convertMethodModifiers(access);
		if (isDefault)
			mods.add(Modifier.DEFAULT);
		System.out.println("######### Visiting " + className + "." + name + "[" + signature + "](" + isVarargs + ")");

		ITypeReference returnType = null;
		List<ParameterDecl> parameters = Collections.emptyList();
		List<FormalTypeParameter> formalTypeParameters = Collections.emptyList();
		List<ITypeReference> thrownExceptions = Collections.emptyList();

		if (signature != null) {
			APISignatureVisitor visitor = new APISignatureVisitor(api, typeRefFactory, isVarargs, "");
			new SignatureReader(signature).accept(visitor);
			returnType = visitor.getReturnType();
			parameters = visitor.getParameters();
			formalTypeParameters = visitor.getFormalTypeParameters();
			thrownExceptions = visitor.getThrownExceptions().isEmpty()
				? convertThrownExceptions(exceptions)
				: visitor.getThrownExceptions();
		} else {
			returnType = convertType(Type.getReturnType(descriptor).getDescriptor());
			parameters = convertParameters(Type.getArgumentTypes(descriptor), parameterNames, isVarargs);
			thrownExceptions = convertThrownExceptions(exceptions);
		}

		return new MethodDecl(String.format("%s.%s", className, name), convertAccess(access),
			mods, convertAnnotations(annotations), SourceLocation.NO_LOCATION,
			typeRefFactory.createTypeReference(className), returnType, parameters,
			formalTypeParameters, thrownExceptions);
	}

	private List<Annotation> convertAnnotations(List<String> annotations) {
		return annotations.stream()
			.map(ann -> new Annotation(
				typeRefFactory.createTypeReference(descriptorToFqn(ann))))
			.toList();
	}

	private List<ITypeReference> convertThrownExceptions(String[] exceptions) {
		if (exceptions != null)
			return Arrays.stream(exceptions)
				.map(e -> (ITypeReference) typeRefFactory.createTypeReference(bytecodeToFqn(e)))
				.toList();
		return Collections.emptyList();
	}

	private List<FormalTypeParameter> convertFormalTypeParameters(APISignatureVisitor visitor) {
		if (visitor == null)
			return Collections.emptyList();
		return visitor.getFormalTypeParameters();
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

	private List<ParameterDecl> convertParameters(Type[] paramTypes, List<String> parameterNames, boolean isVarargs) {
		List<ParameterDecl> params = new ArrayList<>();

		for (int i = 0; i < paramTypes.length; i++) {
			String name = parameterNames.size() > i ? parameterNames.get(i) : "param";
			if (i == paramTypes.length - 1) {
				ITypeReference type = convertType(paramTypes[i].getDescriptor());
				if (isVarargs && type instanceof ArrayTypeReference atr)
					params.add(new ParameterDecl(name, atr.componentType(), true));
				else
					params.add(new ParameterDecl(name, type, false));
			}
			else
				params.add(new ParameterDecl(name, convertType(paramTypes[i].getDescriptor()), false));
		}

		return params;
	}

	private AccessModifier convertAccess(int access) {
		if ((access & Opcodes.ACC_PUBLIC) != 0) return AccessModifier.PUBLIC;
		if ((access & Opcodes.ACC_PROTECTED) != 0) return AccessModifier.PROTECTED;
		if ((access & Opcodes.ACC_PRIVATE) != 0) return AccessModifier.PRIVATE;
		return AccessModifier.PACKAGE_PRIVATE;
	}

	private EnumSet<Modifier> convertClassModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_FINAL) != 0) modifiers.add(Modifier.FINAL);
		if ((access & Opcodes.ACC_ABSTRACT) != 0) modifiers.add(Modifier.ABSTRACT);
		if ((access & Opcodes.ACC_STATIC) != 0) modifiers.add(Modifier.STATIC); // shouldn't work on classes
		// FIXME: sealed, non-sealed
		return modifiers;
	}

	private EnumSet<Modifier> convertFieldModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_STATIC) != 0) modifiers.add(Modifier.STATIC);
		if ((access & Opcodes.ACC_FINAL) != 0) modifiers.add(Modifier.FINAL);
		if ((access & Opcodes.ACC_VOLATILE) != 0) modifiers.add(Modifier.VOLATILE);
		if ((access & Opcodes.ACC_TRANSIENT) != 0) modifiers.add(Modifier.TRANSIENT);
		return modifiers;
	}

	private EnumSet<Modifier> convertMethodModifiers(int access) {
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		if ((access & Opcodes.ACC_STATIC) != 0) modifiers.add(Modifier.STATIC);
		if ((access & Opcodes.ACC_FINAL) != 0) modifiers.add(Modifier.FINAL);
		if ((access & Opcodes.ACC_ABSTRACT) != 0) modifiers.add(Modifier.ABSTRACT);
		if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) modifiers.add(Modifier.SYNCHRONIZED);
		if ((access & Opcodes.ACC_NATIVE) != 0) modifiers.add(Modifier.NATIVE);
		if ((access & Opcodes.ACC_STRICT) != 0) modifiers.add(Modifier.STRICTFP);
		// FIXME: default
		return modifiers;
	}

	private boolean isTypeExported(int access) {
		// FIXME
		return (access & Opcodes.ACC_PUBLIC) != 0;
	}

	private boolean isTypeMemberExported(int access) {
		// FIXME
		return (access & Opcodes.ACC_PUBLIC) != 0
			|| (access & Opcodes.ACC_PROTECTED) != 0;
	}

	@Override
	public void visitEnd() {
		if (ANONYMOUS_PATTERN.matcher(className).matches())
			return;
		// if ((classAccess & Opcodes.ACC_MODULE) != 0) doesn't work?
		if (className.endsWith("package-info") || className.endsWith("module-info"))
			return;

		TypeDecl type;
		String[] parts = className.split("\\$");
		TypeReference<TypeDecl> enclosingType = parts.length > 1 ?
			typeRefFactory.createTypeReference(String.join("$", Arrays.copyOf(parts, parts.length - 1))) : null;
		AccessModifier visibility = convertAccess(classAccess);
		EnumSet<Modifier> modifiers = convertClassModifiers(classAccess);
		List<Annotation> anns = annotations.stream()
			.map(ann -> new Annotation(typeRefFactory.createTypeReference(descriptorToFqn(ann))))
			.toList();
		if (isSealed)
			modifiers.add(Modifier.SEALED);
		SourceLocation location = new SourceLocation(
			Path.of(filename != null ? filename : "<unknown>"),
			-1);

		if ((classAccess & Opcodes.ACC_ANNOTATION) != 0) {
			type = new AnnotationDecl(
				className,
				visibility,
				modifiers,
				anns,
				location,
				fields,
				methods,
				enclosingType
			);
		} else if ((classAccess & Opcodes.ACC_INTERFACE) != 0) {
			type = new InterfaceDecl(
				className,
				visibility,
				modifiers,
				anns,
				location,
				implementedInterfaces,
				formalTypeParameters,
				fields,
				methods,
				enclosingType
			);
		} else if ((classAccess & Opcodes.ACC_ENUM) != 0) {
			// Enums should have a default constructor
//				constructorDecls.add(new ConstructorDecl(
//					String.format("%s.<init>", className),
//					AccessModifier.PUBLIC,
//					EnumSet.noneOf(Modifier.class),
//					anns,
//					location,
//					typeRefFactory.createTypeReference(className),
//					typeRefFactory.createTypeReference(className),
//					Collections.emptyList(),
//					Collections.emptyList(),
//					Collections.emptyList()
//				));
			// FIXME: for some reason, Enums are abstract
			modifiers.remove(Modifier.ABSTRACT);
			type = new EnumDecl(
				className,
				visibility,
				modifiers,
				anns,
				location,
				implementedInterfaces,
				fields,
				methods,
				enclosingType,
				constructors
			);
		} else if ((classAccess & Opcodes.ACC_RECORD) != 0) {
			type = new RecordDecl(
				className,
				visibility,
				modifiers,
				anns,
				location,
				implementedInterfaces,
				Collections.emptyList(),
				fields,
				methods,
				enclosingType,
				constructors
			);
		} else {
			type = new ClassDecl(
				className,
				visibility,
				modifiers,
				anns,
				location,
				implementedInterfaces,
				formalTypeParameters,
				fields,
				methods,
				enclosingType,
				superClass,
				constructors
			);
		}

		// if (isTypeExported(access))
		// need to keep unexported types for type resolution
		typeDecl = type;

		super.visitEnd();
	}
}
