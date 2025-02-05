package com.github.maracas.roseau.api.extractors;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.API;
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
import com.github.maracas.roseau.diff.APIDiff;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.xmlet.htmlapifaster.A;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class JarAPIExtractor implements APIExtractor {
	private static final int ASM_VERSION = Opcodes.ASM9;
	private static final int PARSING_OPTIONS = ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG;

	@Override
	public API extractAPI(Path sources) {
		List<TypeDecl> typeDecls = new ArrayList<>();
		SpoonAPIFactory apiFactory = new SpoonAPIFactory();
		TypeReferenceFactory typeRefFactory = new SpoonTypeReferenceFactory(apiFactory);

		try (JarFile jar = new JarFile(sources.toFile())) {
			jar.stream().forEach(entry -> {
				if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
					try (InputStream is = jar.getInputStream(entry)) {
						ClassReader reader = new ClassReader(is);
						ApiClassVisitor visitor = new ApiClassVisitor(typeRefFactory);
						reader.accept(visitor, PARSING_OPTIONS);
						var typeDecl = visitor.getTypeDecl();
						if (typeDecl != null)
							typeDecls.add(typeDecl);
					} catch (IOException e) {
						throw new RuntimeException("Error processing JAR entry", e);
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Error processing JAR file", e);
		}

		return new API(typeDecls, new SpoonAPIFactory());
	}

	private static class ApiClassVisitor extends ClassVisitor {
		private final TypeReferenceFactory typeRefFactory;
		private String className;
		private int access;
		private TypeReference<ClassDecl> superClassDecl;
		private List<TypeReference<InterfaceDecl>> interfaceDecls = new ArrayList<>();
		private List<FieldDecl> fieldDecls = new ArrayList<>();
		private List<MethodDecl> methodDecls = new ArrayList<>();
		private List<ConstructorDecl> constructorDecls = new ArrayList<>();
		private List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
		private List<String> annotations = new ArrayList<>();
		private boolean isSealed = false;
		private TypeDecl typeDecl;
		private String filename;

		public ApiClassVisitor(TypeReferenceFactory typeRefFactory) {
			super(ASM_VERSION);
			this.typeRefFactory = typeRefFactory;
		}

		public TypeDecl getTypeDecl() {
			return typeDecl;
		}

		private String internalToFqn(String internalName) {
			return internalName.replace('/', '.');
		}
		private String descriptorToFqn(String descriptor) {
			return Type.getType(descriptor).getClassName();
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = internalToFqn(name);
			this.access = access;

			// We don't want Object as explicit superclass
			if (superName != null && !superName.equals("java/lang/Object"))
				superClassDecl = typeRefFactory.createTypeReference(internalToFqn(superName));

			interfaceDecls = Arrays.stream(interfaces)
				.map(this::internalToFqn)
				.map(typeRefFactory::<InterfaceDecl>createTypeReference)
				.toList();

			if (signature != null) {
				System.out.println("cls sign = " + signature);
				SignatureReader reader = new SignatureReader(signature);
				APISignatureVisitor signatureVisitor = new APISignatureVisitor(ASM_VERSION, typeRefFactory, "");
				reader.accept(signatureVisitor);
				formalTypeParameters = signatureVisitor.getFormalTypeParameters();
				superClassDecl = signatureVisitor.getSuperclass();
				interfaceDecls = signatureVisitor.getSuperInterfaces();
			}

			super.visit(version, access, name, signature, superName, interfaces);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			return new FieldVisitor(ASM_VERSION) {
				List<String> annotations = new ArrayList<>();

				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					annotations.add(descriptor);
					return super.visitAnnotation(descriptor, visible);
				}

				@Override
				public void visitEnd() {
					if (isTypeMemberExported(access)) {
						System.out.println("Visiting field " + descriptor + " " + signature);
						TypeVisitor visitor = null;
						if (signature != null) {
							visitor = new TypeVisitor(ASM_VERSION, typeRefFactory, "");
							new SignatureReader(signature).accept(visitor);
						}
						fieldDecls.add(new FieldDecl(
							String.format("%s.%s", className, name),
							convertAccess(access),
							convertModifiers(access),
							annotations.stream().map(ann -> new Annotation(typeRefFactory.createTypeReference(descriptorToFqn(ann)))).toList(),
							SourceLocation.NO_LOCATION,
							typeRefFactory.createTypeReference(className),
							signature != null ? visitor.getType() : convertType(descriptor, null)
						));
					}
				}
			};
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			return new MethodVisitor(ASM_VERSION) {
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
					if (isTypeMemberExported(access)) {
						if (name.equals("<init>")) {
							constructorDecls.add(convertConstructor(access, descriptor, signature, exceptions, parameterNames, annotations));
						} else if (isTypeMemberExported(access)) {
							if (!name.equals("values") && !name.equals("valueOf")) // FIXME: annoying Enum synthetic methods
								methodDecls.add(convertMethod(access, name, descriptor, signature, exceptions, parameterNames, annotations));
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
		}

		@Override
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			if (internalToFqn(name).equals(className)) {
				this.access = access;
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			annotations.add(descriptor);
			return super.visitAnnotation(descriptor, visible);
		}

		@Override
		public void visitSource(String source, String debug) {
			filename = source;
		}

		private ConstructorDecl convertConstructor(int access, String descriptor, String signature, String[] exceptions,
		                                           List<String> parameterNames, List<String> annotations) {
			APISignatureVisitor visitor = null;
			if (signature != null) {
				visitor = new APISignatureVisitor(ASM_VERSION, typeRefFactory, "");
				new SignatureReader(signature).accept(visitor);
			}

			// Constructors should return the type of the type they construct to match with sources extraction
			// Constructors of inner classes take their outer class as argument?
			Type[] originalParams = Type.getArgumentTypes(descriptor);
			List<ParameterDecl> params =
				className.contains("$") && originalParams.length >= 1
					? convertParameters(Arrays.copyOfRange(originalParams, 1, originalParams.length), visitor, parameterNames)
					: convertParameters(originalParams, visitor, parameterNames);
			return new ConstructorDecl(
				String.format("%s.<init>", className),
				convertAccess(access),
				convertModifiers(access),
				annotations.stream().map(ann -> new Annotation(typeRefFactory.createTypeReference(descriptorToFqn(ann)))).toList(),
				SourceLocation.NO_LOCATION,
				typeRefFactory.createTypeReference(className),
				typeRefFactory.createTypeReference(className),
				params,
				convertFormalTypeParameters(visitor),
				exceptions != null ?
						Arrays.stream(exceptions)
							.map(e -> typeRefFactory.<ClassDecl>createTypeReference(internalToFqn(e)))
							.toList() :
					Collections.emptyList()
			);
		}

		private MethodDecl convertMethod(int access, String name, String descriptor, String signature, String[] exceptions,
		                                 List<String> parameterNames, List<String> annotations) {
			System.out.println("######### Visiting " + className + "." + name + "[" + signature + "]");

			APISignatureVisitor visitor = null;
			if (signature != null) {
				visitor = new APISignatureVisitor(ASM_VERSION, typeRefFactory, "");
				new SignatureReader(signature).accept(visitor);
			}

			return new MethodDecl(
				String.format("%s.%s", className, name),
				convertAccess(access),
				convertModifiers(access),
				annotations.stream().map(ann -> new Annotation(typeRefFactory.createTypeReference(descriptorToFqn(ann)))).toList(),
				SourceLocation.NO_LOCATION,
				typeRefFactory.createTypeReference(className),
				convertType(Type.getReturnType(descriptor).getDescriptor(), visitor),
				convertParameters(Type.getArgumentTypes(descriptor), visitor, parameterNames),
				convertFormalTypeParameters(visitor),
				exceptions != null ?
					Arrays.stream(exceptions)
						.map(e -> typeRefFactory.<ClassDecl>createTypeReference(internalToFqn(e)))
						.toList() :
					Collections.emptyList()
			);
		}

		private List<FormalTypeParameter> convertFormalTypeParameters(APISignatureVisitor visitor) {
			if (visitor == null)
				return Collections.emptyList();
			return visitor.getFormalTypeParameters();
		}

		private ITypeReference convertType(String descriptor, APISignatureVisitor visitor) {
			Type type = Type.getType(descriptor);
			if (type.getSort() == Type.ARRAY) {
				ITypeReference component = convertType(type.getElementType().getDescriptor(), visitor);
				return typeRefFactory.createArrayTypeReference(component, type.getDimensions());
			} else if (type.getSort() == Type.OBJECT) {
				if (visitor != null) { // Type-parameterized type
					return visitor.getReturnType();
				}
				return typeRefFactory.createTypeReference(type.getClassName());
			} else {
				return typeRefFactory.createPrimitiveTypeReference(type.getClassName());
			}
		}

		private List<ParameterDecl> convertParameters(Type[] paramTypes, APISignatureVisitor visitor, List<String> parameterNames) {
			if (visitor != null)
				return visitor.getParameters();

			List<ParameterDecl> params = new ArrayList<>();

			for (int i = 0; i < paramTypes.length; i++) {
				String name = parameterNames.size() > i ? parameterNames.get(i) : "param";
				params.add(new ParameterDecl(name, convertType(paramTypes[i].getDescriptor(), visitor), false));
			}

			return params;
		}

		private AccessModifier convertAccess(int access) {
			if ((access & Opcodes.ACC_PUBLIC) != 0) return AccessModifier.PUBLIC;
			if ((access & Opcodes.ACC_PROTECTED) != 0) return AccessModifier.PROTECTED;
			if ((access & Opcodes.ACC_PRIVATE) != 0) return AccessModifier.PRIVATE;
			return AccessModifier.PACKAGE_PRIVATE;
		}

		private EnumSet<Modifier> convertModifiers(int access) {
			EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
			if ((access & Opcodes.ACC_STATIC) != 0) modifiers.add(Modifier.STATIC);
			if ((access & Opcodes.ACC_FINAL) != 0) modifiers.add(Modifier.FINAL);
			if ((access & Opcodes.ACC_ABSTRACT) != 0) modifiers.add(Modifier.ABSTRACT);
			//if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) modifiers.add(Modifier.SYNCHRONIZED);
			if ((access & Opcodes.ACC_VOLATILE) != 0) modifiers.add(Modifier.VOLATILE);
			if ((access & Opcodes.ACC_TRANSIENT) != 0) modifiers.add(Modifier.TRANSIENT);
			if ((access & Opcodes.ACC_NATIVE) != 0) modifiers.add(Modifier.NATIVE);
			if ((access & Opcodes.ACC_STRICT) != 0) modifiers.add(Modifier.STRICTFP);
			// FIXME: sealed, non-sealed, default
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
			TypeDecl type;
			String[] parts = className.split("\\$");
			TypeReference<TypeDecl> enclosingType = parts.length > 1 ?
				typeRefFactory.createTypeReference(String.join("$", Arrays.copyOf(parts, parts.length - 1))) : null;
			AccessModifier visibility = convertAccess(access);
			EnumSet<Modifier> modifiers = convertModifiers(access);
			List<Annotation> anns = annotations.stream()
				.map(ann -> new Annotation(typeRefFactory.createTypeReference(descriptorToFqn(ann))))
				.toList();
			if (isSealed)
				modifiers.add(Modifier.SEALED);
			SourceLocation location = new SourceLocation(
				Path.of(filename != null ? filename : "<unknown>"),
				-1);

			if ((access & Opcodes.ACC_ANNOTATION) != 0) {
				type = new AnnotationDecl(
					className,
					visibility,
					modifiers,
					anns,
					location,
					fieldDecls,
					methodDecls,
					enclosingType
				);
			} else if ((access & Opcodes.ACC_INTERFACE) != 0) {
				type = new InterfaceDecl(
					className,
					visibility,
					modifiers,
					anns,
					location,
					interfaceDecls,
					formalTypeParameters,
					fieldDecls,
					methodDecls,
					enclosingType
				);
			} else if ((access & Opcodes.ACC_ENUM) != 0) {
				// Enums should have a default constructor
				constructorDecls.add(new ConstructorDecl(
					String.format("%s.<init>", className),
					AccessModifier.PUBLIC,
					EnumSet.noneOf(Modifier.class),
					anns,
					location,
					typeRefFactory.createTypeReference(className),
					typeRefFactory.createTypeReference(className),
					Collections.emptyList(),
					Collections.emptyList(),
					Collections.emptyList()
				));
				// FIXME: for some reason, Enums are abstract when extracted from sources?
				modifiers.add(Modifier.ABSTRACT);
				type = new EnumDecl(
					className,
					visibility,
					modifiers,
					anns,
					location,
					interfaceDecls,
					fieldDecls,
					methodDecls,
					enclosingType,
					constructorDecls
				);
			} else if ((access & Opcodes.ACC_RECORD) != 0) {
				type = new RecordDecl(
					className,
					visibility,
					modifiers,
					anns,
					location,
					interfaceDecls,
					Collections.emptyList(),
					fieldDecls,
					methodDecls,
					enclosingType,
					constructorDecls
				);
			} else {
				type = new ClassDecl(
					className,
					visibility,
					modifiers,
					anns,
					location,
					interfaceDecls,
					formalTypeParameters,
					fieldDecls,
					methodDecls,
					enclosingType,
					superClassDecl,
					constructorDecls
				);
			}

			// if (isTypeExported(access))
			// need to keep unexported types for type resolution
			typeDecl = type;

			super.visitEnd();
		}
	}

	public static void main(String[] args) throws Exception {
		// FIXME: SYNCHRONIZED on everything for whatever reason

//		var jarApi = new JarAPIExtractor().extractAPI(Path.of("/home/dig/repositories/maracas/test-data/comp-changes/old/target/comp-changes-old-0.0.1.jar"));
//		var sourcesApi = new SpoonAPIExtractor().extractAPI(Path.of("/home/dig/repositories/maracas/test-data/comp-changes/old/src"));
		var jarApi = new JarAPIExtractor().extractAPI(Path.of("/home/dig/repositories/guava-31.1/guava/target/guava-31.1-jre.jar"));
		var sourcesApi = new SpoonAPIExtractor().extractAPI(Path.of("/home/dig/repositories/guava-31.1/guava/src"));
//		var jarApi = new JarAPIExtractor().extractAPI(Path.of("/home/dig/repositories/asmtest/target/asmtest-1.0-SNAPSHOT.jar"));
//		var sourcesApi = new SpoonAPIExtractor().extractAPI(Path.of("/home/dig/repositories/asmtest/src"));

		jarApi.writeJson(Path.of("jar.json"));
		sourcesApi.writeJson(Path.of("sources.json"));

		var diff = new APIDiff(jarApi, sourcesApi);
		var bcs = diff.diff();
		System.out.println("JAR to sources: " + bcs.size());
		System.out.println(bcs.stream().map(Object::toString).collect(Collectors.joining("\n")));

		var diff2 = new APIDiff(sourcesApi, jarApi);
		var bcs2 = diff2.diff();
		System.out.println("Sources to JAR: " + bcs2.size());
		System.out.println(bcs2.stream().map(Object::toString).collect(Collectors.joining("\n")));

		/*sourcesApi.getAllTypes().forEach(sourceType -> {
			var fqn = sourceType.getQualifiedName();
			var jarTypeOpt = jarApi.findType(fqn);

			if (jarTypeOpt.isEmpty())
				System.out.printf("No %s in JAR%n", fqn);
			else {
				var jarType = jarTypeOpt.get();
				if (!jarType.toString().equals(sourceType.toString())) {
					System.out.printf("%s differs:%n", fqn);
					System.out.println("JAR:");
					System.out.println(jarType);
					System.out.println("Sources:");
					System.out.println(sourceType);
				} else {
					System.out.printf("%s matches%n", fqn);
				}
			}
		});*/
	}
}
