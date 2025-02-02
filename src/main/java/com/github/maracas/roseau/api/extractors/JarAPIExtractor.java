package com.github.maracas.roseau.api.extractors;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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
						TypeDecl typeDecl = visitor.getTypeDecl();
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
		private List<FormalTypeParameter> typeParameterDecls = new ArrayList<>();
		private TypeDecl typeDecl;

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
				SignatureReader reader = new SignatureReader(signature);
				reader.accept(new TypeParamSignatureVisitor());
			}

			super.visit(version, access, name, signature, superName, interfaces);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (isTypeMemberExported(access)) {
				fieldDecls.add(new FieldDecl(
					String.format("%s.%s", className, name),
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					typeRefFactory.createTypeReference(className),
					convertType(descriptor, signature)
				));
			}
			return super.visitField(access, name, descriptor, signature, value);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if (isTypeMemberExported(access)) {
				if (name.equals("<init>")) {
					constructorDecls.add(convertConstructor(access, descriptor, signature));
				} else if (isTypeMemberExported(access)) {
					methodDecls.add(convertMethod(access, name, descriptor, signature));
				}
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}

		private ConstructorDecl convertConstructor(int access, String descriptor, String signature) {
			// Constructors should return the type of the type they construct to match with sources extraction
			return new ConstructorDecl(
				String.format("%s.<init>", className),
				convertAccess(access),
				convertModifiers(access),
				Collections.emptyList(),
				SourceLocation.NO_LOCATION,
				typeRefFactory.createTypeReference(className),
				typeRefFactory.createTypeReference(className),
				convertParameters(Type.getArgumentTypes(descriptor)),
				convertTypeParameters(signature),
				Collections.emptyList()
			);
		}

		private MethodDecl convertMethod(int access, String name, String descriptor, String signature) {
			return new MethodDecl(
				String.format("%s.%s", className, name),
				convertAccess(access),
				convertModifiers(access),
				Collections.emptyList(),
				SourceLocation.NO_LOCATION,
				typeRefFactory.createTypeReference(className),
				convertType(Type.getReturnType(descriptor).getDescriptor(), signature),
				convertParameters(Type.getArgumentTypes(descriptor)),
				convertTypeParameters(signature),
				Collections.emptyList()
			);
		}

		private class TypeParamSignatureVisitor extends SignatureVisitor {
			public TypeParamSignatureVisitor() {
				super(ASM_VERSION);
			}

			@Override
			public void visitFormalTypeParameter(String name) {
				typeParameterDecls.add(new FormalTypeParameter(name, Collections.emptyList()));
			}
		}

		private List<ParameterDecl> convertParameters(Type[] paramTypes) {
			return Arrays.stream(paramTypes)
				.map(t -> new ParameterDecl("param", convertType(t.getDescriptor(), null), false))
				.toList();
		}

		private List<FormalTypeParameter> convertTypeParameters(String signature) {
			if (signature == null)
				return Collections.emptyList();
			List<FormalTypeParameter> params = new ArrayList<>();
			SignatureReader reader = new SignatureReader(signature);
			//reader.accept(new TypeParamSignatureVisitor());
			var visitor = new APISignatureVisitor();
			reader.accept(visitor);
			System.out.println("Got " + visitor.toString());
			return params;
		}

		private ITypeReference convertType(String descriptor, String signature) {
			Type type = Type.getType(descriptor);
			if (type.getSort() == Type.ARRAY) {
				ITypeReference component = convertType(type.getElementType().getDescriptor(), signature);
				return typeRefFactory.createArrayTypeReference(component, type.getDimensions());
			} else if (type.getSort() == Type.OBJECT) {
				if (signature != null) { // Type-parameterized type
					SignatureReader reader = new SignatureReader(signature);
					APISignatureVisitor visitor = new APISignatureVisitor();
					reader.accept(visitor);
					List<ITypeReference> typeArguments = visitor.getTypeArguments().stream()
						.map(this::internalToFqn)
						.map(arg -> (ITypeReference) typeRefFactory.createTypeReference(arg))
						.toList();
					return typeRefFactory.createTypeReference(
						type.getClassName(), typeArguments);
				}
				return typeRefFactory.createTypeReference(type.getClassName());
			} else {
				return typeRefFactory.createPrimitiveTypeReference(type.getClassName());
			}
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
			return modifiers;
		}

		private boolean isTypeExported(int access) {
			// FIXME
			return (access & Opcodes.ACC_PUBLIC) != 0;
		}

		private boolean isTypeMemberExported(int access) {
			// FIXME
			return (access & Opcodes.ACC_PUBLIC) != 0;
		}

		@Override
		public void visitEnd() {
			TypeDecl type;
			String[] parts = className.split("\\$");
			TypeReference<TypeDecl> enclosingType = parts.length > 1 ?
				typeRefFactory.createTypeReference(String.join("$", Arrays.copyOf(parts, parts.length - 1))) : null;

			if ((access & Opcodes.ACC_ANNOTATION) != 0) {
				type = new AnnotationDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					fieldDecls,
					methodDecls,
					enclosingType
				);
			} else if ((access & Opcodes.ACC_INTERFACE) != 0) {
				type = new InterfaceDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					interfaceDecls,
					typeParameterDecls,
					fieldDecls,
					methodDecls,
					enclosingType
				);
			} else if ((access & Opcodes.ACC_ENUM) != 0) {
				type = new EnumDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					interfaceDecls,
					fieldDecls,
					methodDecls,
					enclosingType,
					constructorDecls
				);
			} else if ((access & Opcodes.ACC_RECORD) != 0) {
				type = new RecordDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
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
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					interfaceDecls,
					typeParameterDecls,
					fieldDecls,
					methodDecls,
					enclosingType,
					superClassDecl,
					constructorDecls
				);
			}

			if (isTypeExported(access))
				typeDecl = type;

			super.visitEnd();
		}
	}

	public static void main(String[] args) {
		var jarApi = new JarAPIExtractor().extractAPI(Path.of("/home/dig/repositories/maracas/test-data/comp-changes/old/target/comp-changes-old-0.0.1.jar"));
		var sourcesApi = new SpoonAPIExtractor().extractAPI(Path.of("/home/dig/repositories/maracas/test-data/comp-changes/old/src"));

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
