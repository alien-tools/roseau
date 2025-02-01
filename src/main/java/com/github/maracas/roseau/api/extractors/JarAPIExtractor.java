package com.github.maracas.roseau.api.extractors;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.Annotation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class JarAPIExtractor implements APIExtractor {
	private static final int ASM_VERSION = Opcodes.ASM9;

	@Override
	public API extractAPI(Path sources) {
		Map<String, TypeDecl> typeDecls = new HashMap<>();
		SpoonAPIFactory apiFactory = new SpoonAPIFactory();
		TypeReferenceFactory typeRefFactory = new SpoonTypeReferenceFactory(apiFactory);

		try (JarFile jar = new JarFile(sources.toFile())) {
			jar.stream().forEach(entry -> {
				if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
					try (InputStream is = jar.getInputStream(entry)) {
						ClassReader reader = new ClassReader(is);
						reader.accept(new ApiClassVisitor(typeDecls, typeRefFactory),
							ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
					} catch (IOException e) {
						throw new RuntimeException("Error processing JAR entry", e);
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Error processing JAR file", e);
		}

		List<TypeDecl> exportedTypes = new ArrayList<>();
		for (TypeDecl type : typeDecls.values()) {
			if (isExported(type, typeDecls)) {
				exportedTypes.add(type);
			}
		}

		return new API(exportedTypes, new SpoonAPIFactory());
	}

	private boolean isExported(TypeDecl type, Map<String, TypeDecl> typeDecls) {
		// FIXME
		return true;//type.getAccessModifier() == AccessModifier.PUBLIC;
	}

	private static class ApiClassVisitor extends ClassVisitor {
		private final Map<String, TypeDecl> typeDecls;
		private final TypeReferenceFactory typeRefFactory;
		private String className;
		private int access;
		private TypeReference<ClassDecl> superClass;
		private List<TypeReference<InterfaceDecl>> interfaces = new ArrayList<>();
		private List<FieldDecl> fields = new ArrayList<>();
		private List<MethodDecl> methods = new ArrayList<>();
		private List<ConstructorDecl> constructors = new ArrayList<>();
		private List<FormalTypeParameter> typeParams = new ArrayList<>();

		public ApiClassVisitor(Map<String, TypeDecl> typeDecls, TypeReferenceFactory typeRefFactory) {
			super(ASM_VERSION);
			this.typeDecls = typeDecls;
			this.typeRefFactory = typeRefFactory;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.className = name.replace('/', '.');
			this.access = access;

			if (superName != null)
				this.superClass = typeRefFactory.createTypeReference(superName.replace('/', '.'));

			for (String iface : interfaces)
				this.interfaces.add(typeRefFactory.createTypeReference(iface.replace('/', '.')));

			if (signature != null) {
				SignatureReader reader = new SignatureReader(signature);
				reader.accept(new TypeParamSignatureVisitor());
			}

			super.visit(version, access, name, signature, superName, interfaces);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (isExported(access)) {
				ITypeReference type = parseType(descriptor, signature);
				fields.add(new FieldDecl(
					String.format("%s.%s", className, name),
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					typeRefFactory.createTypeReference(className),
					type
				));
			}
			return super.visitField(access, name, descriptor, signature, value);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if (name.equals("<init>")) {
				constructors.add(convertConstructor(access, descriptor, signature));
			} else if (isExported(access)) {
				methods.add(convertMethod(access, name, descriptor, signature));
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}

		private ConstructorDecl convertConstructor(int access, String descriptor, String signature) {
			return new ConstructorDecl(
				String.format("%s.<init>", className),
				convertAccess(access),
				convertModifiers(access),
				Collections.emptyList(),
				SourceLocation.NO_LOCATION,
				typeRefFactory.createTypeReference(className),
				parseType(Type.getReturnType(descriptor).getDescriptor(), null),
				parseParameters(Type.getArgumentTypes(descriptor)),
				parseTypeParams(signature),
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
				parseType(Type.getReturnType(descriptor).getDescriptor(), null),
				parseParameters(Type.getArgumentTypes(descriptor)),
				parseTypeParams(signature),
				Collections.emptyList()
			);
		}

		private List<ParameterDecl> parseParameters(Type[] paramTypes) {
			List<ParameterDecl> params = new ArrayList<>();
			for (Type param : paramTypes) {
				params.add(new ParameterDecl(
					"param",
					parseType(param.getDescriptor(), null),
					false
				));
			}
			return params;
		}

		private List<FormalTypeParameter> parseTypeParams(String signature) {
			if (signature == null) return Collections.emptyList();
			List<FormalTypeParameter> params = new ArrayList<>();
			SignatureReader reader = new SignatureReader(signature);
			reader.accept(new TypeParamSignatureVisitor());
			return params;
		}

		private ITypeReference parseType(String descriptor, String signature) {
			Type type = Type.getType(descriptor);
			if (type.getSort() == Type.ARRAY) {
				ITypeReference component = parseType(type.getElementType().getDescriptor(), null);
				return typeRefFactory.createArrayTypeReference(component, type.getDimensions());
			} else if (type.getSort() == Type.OBJECT) {
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

		private boolean isExported(int access) {
			return (access & Opcodes.ACC_PUBLIC) != 0;
		}

		@Override
		public void visitEnd() {
			TypeDecl type;
			String[] parts = className.split("\\$");
			TypeReference<TypeDecl> enclosingType = parts.length > 1 ?
				typeRefFactory.createTypeReference(String.join("$", Arrays.copyOf(parts, parts.length - 1))) : null;

			if ((access & Opcodes.ACC_INTERFACE) != 0) {
				type = new InterfaceDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					interfaces,
					typeParams,
					fields,
					methods,
					enclosingType
				);
			} else if ((access & Opcodes.ACC_ENUM) != 0) {
				type = new EnumDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					interfaces,
					fields,
					methods,
					enclosingType,
					constructors
				);
			} else if ((access & Opcodes.ACC_RECORD) != 0) {
				type = new RecordDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					interfaces,
					Collections.emptyList(),
					fields,
					methods,
					enclosingType,
					constructors
				);
			} else {
				type = new ClassDecl(
					className,
					convertAccess(access),
					convertModifiers(access),
					Collections.emptyList(),
					SourceLocation.NO_LOCATION,
					interfaces,
					typeParams,
					fields,
					methods,
					enclosingType,
					null,//superClass,
					constructors
				);
			}

			typeDecls.put(className, type);
			super.visitEnd();
		}

		private class TypeParamSignatureVisitor extends SignatureVisitor {
			public TypeParamSignatureVisitor() {
				super(ASM_VERSION);
			}

			@Override
			public void visitFormalTypeParameter(String name) {
				typeParams.add(new FormalTypeParameter(name, Collections.emptyList()));
			}
		}
	}

	private static class ASMAPIFactory {
		private final TypeReferenceFactory typeReferenceFactory = new SpoonTypeReferenceFactory(new SpoonAPIFactory());

		public TypeDecl createTypeDecl(String className, int access, String signature, String superClass, List<String> interfaces,
		                               List<FieldDecl> fields, List<MethodDecl> methods, List<ConstructorDecl> constructors,
		                               List<Annotation> annotations) {
			List<FormalTypeParameter> typeParameters = parseFormalTypeParameters(signature);
			TypeReference<ClassDecl> superClassRef = superClass != null ?
				typeReferenceFactory.createTypeReference(superClass.replace('/', '.')) : null;
			List<TypeReference<InterfaceDecl>> interfaceRefs = interfaces.stream()
				.map(i -> typeReferenceFactory.<InterfaceDecl>createTypeReference(i.replace('/', '.')))
				.toList();
			String[] parts = className.split("\\$");
			TypeReference<TypeDecl> enclosingType = parts.length > 1 ?
				typeReferenceFactory.createTypeReference(String.join("$", Arrays.copyOf(parts, parts.length - 1))) : null;

			AccessModifier accessModifier = convertAccess(access);
			EnumSet<Modifier> modifiers = convertModifiers(access);

			if ((access & Opcodes.ACC_INTERFACE) != 0) {
				return new InterfaceDecl(
					className,
					accessModifier,
					modifiers,
					annotations,
					SourceLocation.NO_LOCATION,
					interfaceRefs,
					typeParameters,
					fields,
					methods,
					enclosingType
				);
			} else if ((access & Opcodes.ACC_ENUM) != 0) {
				return new EnumDecl(
					className,
					accessModifier,
					modifiers,
					annotations,
					SourceLocation.NO_LOCATION,
					interfaceRefs,
					fields,
					methods,
					enclosingType,
					constructors
				);
			} else {
				return new ClassDecl(
					className,
					accessModifier,
					modifiers,
					annotations,
					SourceLocation.NO_LOCATION,
					interfaceRefs,
					typeParameters,
					fields,
					methods,
					enclosingType,
					superClassRef,
					constructors
				);
			}
		}

		public FieldDecl createFieldDecl(String className, int access, String name, String descriptor, String signature) {
			return new FieldDecl(
				String.format("%s.%s", className, name),
				convertAccess(access),
				convertModifiers(access),
				Collections.emptyList(), // Annotations would require additional processing
				SourceLocation.NO_LOCATION,
				typeReferenceFactory.createTypeReference(className),
				parseTypeReference(descriptor, signature)
			);
		}

		public MethodDecl createMethodDecl(String className, int access, String name, String descriptor, String signature) {
			return new MethodDecl(
				String.format("%s.%s", className, name),
				convertAccess(access),
				convertModifiers(access),
				Collections.emptyList(), // Annotations
				SourceLocation.NO_LOCATION,
				typeReferenceFactory.createTypeReference(className),
				parseTypeReference(Type.getReturnType(descriptor).getDescriptor(), null),
				parseParameters(Type.getArgumentTypes(descriptor)),
				parseFormalTypeParameters(signature),
				Collections.emptyList() // Thrown exceptions
			);
		}

		public ConstructorDecl createConstructorDecl(String className, int access, String descriptor, String signature) {
			return new ConstructorDecl(
				String.format("%s.<init>", className),
				convertAccess(access),
				convertModifiers(access),
				Collections.emptyList(), // Annotations
				SourceLocation.NO_LOCATION,
				typeReferenceFactory.createTypeReference(className),
				parseTypeReference(Type.getReturnType(descriptor).getDescriptor(), null),
				parseParameters(Type.getArgumentTypes(descriptor)),
				parseFormalTypeParameters(signature),
				Collections.emptyList() // Thrown exceptions
			);
		}

		private List<FormalTypeParameter> parseFormalTypeParameters(String signature) {
			if (signature == null) return Collections.emptyList();
			List<FormalTypeParameter> params = new ArrayList<>();
			new SignatureReader(signature).accept(new SignatureVisitor(Opcodes.ASM9) {
				@Override
				public void visitFormalTypeParameter(String name) {
					params.add(new FormalTypeParameter(name, Collections.emptyList()));
				}
			});
			return params;
		}

		private ITypeReference parseTypeReference(String descriptor, String signature) {
			Type type = Type.getType(descriptor);
			if (type.getSort() == Type.ARRAY) {
				ITypeReference component = parseTypeReference(type.getElementType().getDescriptor(), null);
				return typeReferenceFactory.createArrayTypeReference(component, type.getDimensions());
			} else if (type.getSort() == Type.OBJECT) {
				return typeReferenceFactory.createTypeReference(type.getClassName());
			} else {
				return typeReferenceFactory.createPrimitiveTypeReference(type.getClassName());
			}
		}

		private List<ParameterDecl> parseParameters(Type[] paramTypes) {
			return Arrays.stream(paramTypes)
				.map(t -> new ParameterDecl("param", parseTypeReference(t.getDescriptor(), null), false))
				.collect(Collectors.toList());
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
			if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) modifiers.add(Modifier.SYNCHRONIZED);
			if ((access & Opcodes.ACC_VOLATILE) != 0) modifiers.add(Modifier.VOLATILE);
			if ((access & Opcodes.ACC_TRANSIENT) != 0) modifiers.add(Modifier.TRANSIENT);
			if ((access & Opcodes.ACC_NATIVE) != 0) modifiers.add(Modifier.NATIVE);
			if ((access & Opcodes.ACC_STRICT) != 0) modifiers.add(Modifier.STRICTFP);
			//if ((access & Opcodes.ACC_DEFAULT) != 0) modifiers.add(Modifier.DEFAULT);
			return modifiers;
		}
	}

	public static void main(String[] args) {
		var x = new JarAPIExtractor().extractAPI(Path.of("/home/dig/.m2/repository/com/github/maracas/roseau/0.0.2-SNAPSHOT/roseau-0.0.2-SNAPSHOT.jar"));
		System.out.println(x);
	}
}
