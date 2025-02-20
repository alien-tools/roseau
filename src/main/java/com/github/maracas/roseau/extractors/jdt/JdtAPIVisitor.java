package com.github.maracas.roseau.extractors.jdt;

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
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class JdtAPIVisitor extends ASTVisitor {
	private final List<TypeDecl> collectedTypeDecls = new ArrayList<>();
	private final CompilationUnit cu;
	private final String filePath;
	private final TypeReferenceFactory typeRefFactory;

	public JdtAPIVisitor(CompilationUnit cu, String filePath, TypeReferenceFactory factory) {
		this.cu = cu;
		this.filePath = filePath;
		this.typeRefFactory = factory;
	}

	public List<TypeDecl> getCollectedTypeDecls() {
		return collectedTypeDecls;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		// Nothing to do; package name is used when computing qualified names.
		return super.visit(node);
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if (isAnonymousOrLocal(node)) {
			System.out.println("Anonymous or local " + node);
			return false;
		}
		processTypeDeclaration(node);
		// Do not visit children automatically—processing nested types is handled in processTypeDeclaration.
		return false;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		if (isAnonymousOrLocal(node)) {
			return false;
		}
		processEnumDeclaration(node);
		return false;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		if (isAnonymousOrLocal(node)) {
			return false;
		}
		processAnnotationDeclaration(node);
		return false;
	}

	@Override
	public boolean visit(RecordDeclaration node) {
		if (isAnonymousOrLocal(node)) {
			return false;
		}
		processRecordDeclaration(node);
		return false;
	}

	private void processAbstractTypeDeclaration(AbstractTypeDeclaration type) {
		switch (type) {
			case TypeDeclaration td -> processTypeDeclaration(td);
			case EnumDeclaration ed -> processEnumDeclaration(ed);
			case RecordDeclaration rd -> processRecordDeclaration(rd);
			case AnnotationTypeDeclaration atd -> processAnnotationDeclaration(atd);
			default -> throw new IllegalStateException("Unexpected value: " + type);
		}
	}

	private void processTypeDeclaration(TypeDeclaration type) {
		ITypeBinding binding = type.resolveBinding();
		if (binding == null)
			throw new RuntimeException("No binding for " + type);

		Arrays.stream(getInnerTypes(type)).forEach(inner -> processAbstractTypeDeclaration(inner));

		String qualifiedName = roseauFqn(binding);
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> modifiers = convertModifiers(binding.getModifiers());
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		SourceLocation location = new SourceLocation(Paths.get(filePath), cu.getLineNumber(type.getStartPosition()));
		List<FormalTypeParameter> typeParams = convertTypeParameters(binding.getTypeParameters());

		// https://github.com/eclipse-jdt/eclipse.jdt.core/pull/3252
		if (convertModifiers(type.getModifiers()).contains(Modifier.SEALED))
			modifiers.add(Modifier.SEALED);
		if (convertModifiers(type.getModifiers()).contains(Modifier.NON_SEALED))
			modifiers.add(Modifier.NON_SEALED);

		// Implemented interfaces: For classes and interfaces.
		List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
		implementedInterfaces.addAll(Arrays.stream(binding.getInterfaces())
			.map(intf -> (TypeReference<InterfaceDecl>) makeTypeReference(intf)).toList());

		// For classes: determine superclass
		TypeReference<ClassDecl> superClassRef = binding.getSuperclass() != null
			? (TypeReference<ClassDecl>) makeTypeReference(binding.getSuperclass())
			: null;

		// Process fields, methods and constructors from the body declarations
		List<FieldDecl> fields = Arrays.stream(binding.getDeclaredFields())
			.filter(field -> isExported(field))
			.map(field -> convertFieldDeclaration(field, binding))
			.toList();

		List<MethodDecl> methods = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> !method.isConstructor() && isExported(method))
			.map(method -> convertMethod(method, binding))
			.toList();

		List<ConstructorDecl> constructors = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> method.isConstructor() && isExported(method))
			.map(cons -> convertConstructor(cons, binding))
			.toList();

		var enclosingType = (TypeReference<TypeDecl>) (binding.getDeclaringClass() != null
			? makeTypeReference(binding.getDeclaringClass())
			: null);

		// Build the API model for a class or interface
		TypeDecl typeDecl;
		if (binding.isInterface()) {
			modifiers.add(Modifier.ABSTRACT);
			typeDecl = new InterfaceDecl(
				qualifiedName,
				visibility,
				modifiers,
				anns,
				location,
				implementedInterfaces,
				typeParams,
				fields,
				methods,
				enclosingType
			);
		} else {
			typeDecl = new ClassDecl(
				qualifiedName,
				visibility,
				modifiers,
				anns,
				location,
				implementedInterfaces,
				typeParams,
				fields,
				methods,
				enclosingType,
				superClassRef,
				constructors
			);
		}
		collectedTypeDecls.add(typeDecl);
	}

	public String roseauFqn(ITypeBinding type) {
		if (type.getDeclaringClass() == null)
			return type.getQualifiedName();
		else return roseauFqn(type.getDeclaringClass()) + "$" + type.getName();
	}

	private boolean isExported(ITypeBinding type) {
		var visibility = convertVisibility(type.getModifiers());

		return (visibility == AccessModifier.PUBLIC || (visibility == AccessModifier.PROTECTED && !isEffectivelyFinal(type)))
			&& isParentExported(type.getDeclaringClass());
	}

	private boolean isExported(IVariableBinding field) {
		var visibility = convertVisibility(field.getModifiers());
		var parent = field.getDeclaringClass();

		return visibility == AccessModifier.PUBLIC || (visibility == AccessModifier.PROTECTED && !isEffectivelyFinal(parent));
	}

	private boolean isExported(IMethodBinding method) {
		var visibility = convertVisibility(method.getModifiers());
		var parent = method.getDeclaringClass();

		return visibility == AccessModifier.PUBLIC || (visibility == AccessModifier.PROTECTED && !isEffectivelyFinal(parent));
	}

	private boolean isParentExported(ITypeBinding type) {
		if (type.getDeclaringClass() == null)
			return true;
		return isExported(type.getDeclaringClass());
	}

	// FIXME: fails cause of https://github.com/eclipse-jdt/eclipse.jdt.core/pull/3252
	private boolean isEffectivelyFinal(ITypeBinding type) {
		if (type.isEnum() || type.isRecord())
			return true;

		var modifiers = convertModifiers(type.getModifiers());
		var isFinal = modifiers.contains(Modifier.FINAL);
		var isSealed = modifiers.contains(Modifier.SEALED);
		var isNonSealed = modifiers.contains(Modifier.NON_SEALED);

		if (type.isClass()) {
			var cons = Arrays.stream(type.getDeclaredMethods())
				.filter(m -> m.isConstructor())
				.toList();
			if (!cons.isEmpty() && cons.stream().allMatch(c -> convertVisibility(c.getModifiers()) == AccessModifier.PRIVATE))
				return true;
		}

		return (isFinal || isSealed) && !isNonSealed;
	}

	// Available on TypeDeclaration, but not Enum/Record/Annotation, even though they happen
	// The original implementation only returns inner Class/Interface, not Enum/Record/Annotation
	// ITypeBinding properly implements inner types, but we need to visit ASTs (currently...)
	public AbstractTypeDeclaration[] getInnerTypes(AbstractTypeDeclaration type) {
		List bd = type.bodyDeclarations();
		int typeCount = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			Object o = it.next();
			if (o instanceof AbstractTypeDeclaration) {
				typeCount++;
			}
		}
		AbstractTypeDeclaration[] memberTypes = new AbstractTypeDeclaration[typeCount];
		int next = 0;
		for (Iterator it = bd.listIterator(); it.hasNext(); ) {
			Object decl = it.next();
			if (decl instanceof AbstractTypeDeclaration atd) {
				memberTypes[next++] = atd;
			}
		}
		return memberTypes;
	}

	/**
	 * Process an EnumDeclaration.
	 */
	private void processEnumDeclaration(EnumDeclaration enumDecl) {
		ITypeBinding binding = enumDecl.resolveBinding();
		if (binding == null)
			throw new RuntimeException("No binding for " + enumDecl);

		Arrays.stream(getInnerTypes(enumDecl)).forEach(nested -> processAbstractTypeDeclaration(nested));

		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> modifiers = convertModifiers(binding.getModifiers());
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		SourceLocation location = new SourceLocation(Paths.get(filePath), cu.getLineNumber(enumDecl.getStartPosition()));
		// Enums may implement interfaces.
		List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
		implementedInterfaces.addAll(Arrays.stream(binding.getInterfaces())
			.map(intf -> (TypeReference<InterfaceDecl>) makeTypeReference(intf)).toList());
		// Enums do not have formal type parameters in Java.
		List<FormalTypeParameter> typeParams = Collections.emptyList();

		List<FieldDecl> fields = Arrays.stream(binding.getDeclaredFields())
			.filter(field -> isExported(field))
			.map(field -> convertFieldDeclaration(field, binding))
			.toList();

		List<MethodDecl> methods = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> !method.isConstructor() && isExported(method))
			.map(method -> convertMethod(method, binding))
			.toList();

		List<ConstructorDecl> constructors = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> method.isConstructor() && isExported(method))
			.map(cons -> convertConstructor(cons, binding))
			.toList();

		var enclosingType = (TypeReference<TypeDecl>) (binding.getDeclaringClass() != null
			? makeTypeReference(binding.getDeclaringClass())
			: null);

		TypeDecl typeDecl = new EnumDecl(
			roseauFqn(binding),
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
		collectedTypeDecls.add(typeDecl);
	}

	/**
	 * Process an AnnotationTypeDeclaration.
	 */
	private void processAnnotationDeclaration(AnnotationTypeDeclaration annotationDecl) {
		ITypeBinding binding = annotationDecl.resolveBinding();
		if (binding == null)
			throw new RuntimeException("No binding for " + annotationDecl);

		Arrays.stream(getInnerTypes(annotationDecl)).forEach(nested -> processAbstractTypeDeclaration(nested));

		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> modifiers = convertModifiers(binding.getModifiers());
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		SourceLocation location = new SourceLocation(Paths.get(filePath), cu.getLineNumber(annotationDecl.getStartPosition()));
		// Annotations do not have super types or formal type parameters.$

		List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
		implementedInterfaces.addAll(Arrays.stream(binding.getInterfaces())
			.map(intf -> (TypeReference<InterfaceDecl>) makeTypeReference(intf)).toList());

		List<FieldDecl> fields = Arrays.stream(binding.getDeclaredFields())
			.filter(field -> isExported(field))
			.map(field -> convertFieldDeclaration(field, binding))
			.toList();

		List<MethodDecl> methods = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> !method.isConstructor() && isExported(method))
			.map(method -> convertMethod(method, binding))
			.toList();

		var enclosingType = (TypeReference<TypeDecl>) (binding.getDeclaringClass() != null
			? makeTypeReference(binding.getDeclaringClass())
			: null);

		TypeDecl typeDecl = new AnnotationDecl(
			roseauFqn(binding),
			visibility,
			modifiers,
			anns,
			location,
			fields,
			methods,
			enclosingType
		);
		collectedTypeDecls.add(typeDecl);
	}

	/**
	 * Process a RecordDeclaration.
	 */
	private void processRecordDeclaration(RecordDeclaration recordDecl) {
		ITypeBinding binding = recordDecl.resolveBinding();
		if (binding == null)
			throw new RuntimeException("No binding for " + recordDecl);

		Arrays.stream(getInnerTypes(recordDecl)).forEach(nested -> processAbstractTypeDeclaration(nested));

		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> modifiers = convertModifiers(binding.getModifiers());
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		SourceLocation location = new SourceLocation(Paths.get(filePath), cu.getLineNumber(recordDecl.getStartPosition()));
		// Records may implement interfaces.
		List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
		implementedInterfaces.addAll(Arrays.stream(binding.getInterfaces())
			.map(intf -> (TypeReference<InterfaceDecl>) makeTypeReference(intf)).toList());
		// Records do not have formal type parameters.
		List<FormalTypeParameter> typeParams = Collections.emptyList();

		// https://github.com/eclipse-jdt/eclipse.jdt.core/pull/3252
		if (convertModifiers(recordDecl.getModifiers()).contains(Modifier.SEALED))
			modifiers.add(Modifier.SEALED);
		if (convertModifiers(recordDecl.getModifiers()).contains(Modifier.NON_SEALED))
			modifiers.add(Modifier.NON_SEALED);

		// Process fields, methods and constructors from the body declarations
		List<FieldDecl> fields = Arrays.stream(binding.getDeclaredFields())
			.filter(field -> isExported(field))
			.map(field -> convertFieldDeclaration(field, binding))
			.toList();

		List<MethodDecl> methods = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> !method.isConstructor() && isExported(method))
			.map(method -> convertMethod(method, binding))
			.toList();

		List<ConstructorDecl> constructors = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> method.isConstructor() && isExported(method))
			.map(cons -> convertConstructor(cons, binding))
			.toList();

		var enclosingType = (TypeReference<TypeDecl>) (binding.getDeclaringClass() != null
			? makeTypeReference(binding.getDeclaringClass())
			: null);

		//modifiers.add(Modifier.FINAL);
		TypeDecl typeDecl = new RecordDecl(
			roseauFqn(binding),
			visibility,
			modifiers,
			anns,
			location,
			implementedInterfaces,
			typeParams,
			fields,
			methods,
			enclosingType,
			constructors
		);
		collectedTypeDecls.add(typeDecl);
	}

	private AccessModifier convertVisibility(int modifiers) {
		if (org.eclipse.jdt.core.dom.Modifier.isPublic(modifiers))
			return AccessModifier.PUBLIC;
		if (org.eclipse.jdt.core.dom.Modifier.isProtected(modifiers))
			return AccessModifier.PROTECTED;
		if (org.eclipse.jdt.core.dom.Modifier.isPrivate(modifiers))
			return AccessModifier.PRIVATE;
		return AccessModifier.PACKAGE_PRIVATE;
	}

	private Set<Modifier> convertModifiers(int modifiers) {
		Set<Modifier> result = EnumSet.noneOf(Modifier.class);
		if (org.eclipse.jdt.core.dom.Modifier.isStatic(modifiers)) result.add(Modifier.STATIC);
		if (org.eclipse.jdt.core.dom.Modifier.isFinal(modifiers)) result.add(Modifier.FINAL);
		if (org.eclipse.jdt.core.dom.Modifier.isAbstract(modifiers)) result.add(Modifier.ABSTRACT);
		if (org.eclipse.jdt.core.dom.Modifier.isSynchronized(modifiers)) result.add(Modifier.SYNCHRONIZED);
		if (org.eclipse.jdt.core.dom.Modifier.isVolatile(modifiers)) result.add(Modifier.VOLATILE);
		if (org.eclipse.jdt.core.dom.Modifier.isTransient(modifiers)) result.add(Modifier.TRANSIENT);
		if (org.eclipse.jdt.core.dom.Modifier.isNative(modifiers)) result.add(Modifier.NATIVE);
		if (org.eclipse.jdt.core.dom.Modifier.isStrictfp(modifiers)) result.add(Modifier.STRICTFP);
		if (org.eclipse.jdt.core.dom.Modifier.isSealed(modifiers)) result.add(Modifier.SEALED);
		if (org.eclipse.jdt.core.dom.Modifier.isNonSealed(modifiers)) result.add(Modifier.NON_SEALED);
		if (org.eclipse.jdt.core.dom.Modifier.isDefault(modifiers)) result.add(Modifier.DEFAULT);
		return result;
	}

	private List<Annotation> convertAnnotations(IAnnotationBinding[] annotations) {
		return Arrays.stream(annotations)
			.map(ann -> new Annotation((TypeReference<AnnotationDecl>) makeTypeReference(ann.getAnnotationType())))
			.toList();
	}

	private List<FormalTypeParameter> convertTypeParameters(ITypeBinding[] typeParameters) {
		var params = new ArrayList<FormalTypeParameter>();
		for (ITypeBinding typeParam : typeParameters) {
			params.add(new FormalTypeParameter(typeParam.getName(),
				Arrays.stream(typeParam.getTypeBounds()).map(b -> makeTypeReference(b)).toList()));
		}
		return params;
	}

	private FieldDecl convertFieldDeclaration(IVariableBinding fd, ITypeBinding enclosingType) {
		ITypeReference fieldType = makeTypeReference(fd.getType());
		AccessModifier visibility = convertVisibility(fd.getModifiers());
		Set<Modifier> mods = convertModifiers(fd.getModifiers());
		List<Annotation> anns = convertAnnotations(fd.getAnnotations());
		SourceLocation location = new SourceLocation(Paths.get(filePath), -1); // FIXME

		return new FieldDecl(
			roseauFqn(enclosingType) + "." + fd.getName(),
			visibility,
			mods,
			anns,
			location,
			typeRefFactory.createTypeReference(roseauFqn(enclosingType)),
			fieldType
		);
	}

	private ConstructorDecl convertConstructor(IMethodBinding md, ITypeBinding enclosingType) {
		AccessModifier visibility = convertVisibility(md.getModifiers());
		Set<Modifier> mods = convertModifiers(md.getModifiers());
		List<Annotation> anns = convertAnnotations(md.getAnnotations());
		SourceLocation location = new SourceLocation(Paths.get(filePath), -1); // FIXME
		List<ParameterDecl> params = convertParameters(md.getParameterNames(), md.getParameterTypes(), md.isVarargs());
		List<FormalTypeParameter> typeParams = convertTypeParameters(md.getTypeParameters());
		List<ITypeReference> thrownExceptions = convertThrownExceptions(md.getExceptionTypes());

		if (md.isCompactConstructor() && enclosingType instanceof RecordDeclaration rec) {
			rec.recordComponents().forEach(field -> {
				if (field instanceof SingleVariableDeclaration vdf) {
					String fieldName = vdf.getName().getIdentifier();
					params.add(new ParameterDecl(fieldName, makeTypeReference(vdf.getType().resolveBinding()), false));
				}
			});
		}

		return new ConstructorDecl(
			roseauFqn(enclosingType) + ".<init>",
			visibility,
			mods,
			anns,
			location,
			typeRefFactory.createTypeReference(roseauFqn(enclosingType)),
			typeRefFactory.createTypeReference(roseauFqn(enclosingType)),
			params,
			typeParams,
			thrownExceptions
		);
	}

	private MethodDecl convertMethod(IMethodBinding md, ITypeBinding enclosingType) {
		AccessModifier visibility = convertVisibility(md.getModifiers());
		Set<Modifier> mods = convertModifiers(md.getModifiers());
		List<Annotation> anns = convertAnnotations(md.getAnnotations());
		SourceLocation location = new SourceLocation(Paths.get(filePath), -1); // FIXME
		ITypeReference returnType = makeTypeReference(md.getReturnType());
		List<ParameterDecl> params = convertParameters(md.getParameterNames(), md.getParameterTypes(), md.isVarargs());
		List<FormalTypeParameter> typeParams = convertTypeParameters(md.getTypeParameters());
		List<ITypeReference> thrownExceptions = convertThrownExceptions(md.getExceptionTypes());

		return new MethodDecl(
			roseauFqn(enclosingType) + "." + md.getName(),
			visibility,
			mods,
			anns,
			location,
			typeRefFactory.createTypeReference(roseauFqn(enclosingType)),
			returnType,
			params,
			typeParams,
			thrownExceptions
		);
	}

	private List<ParameterDecl> convertParameters(String[] names, ITypeBinding[] types, boolean isVarargs) {
		var params = new ArrayList<ParameterDecl>();
		for (int i = 0; i < names.length; i++) {
			if (isVarargs && i == names.length - 1)
				params.add(new ParameterDecl(names[i], makeTypeReference(types[i].getComponentType()), true));
			else
				params.add(new ParameterDecl(names[i], makeTypeReference(types[i]), false));
		}
		return params;
	}

	private List<ITypeReference> convertThrownExceptions(ITypeBinding[] bindings) {
		return Arrays.stream(bindings).map(b -> makeTypeReference(b)).toList();
	}

	private ITypeReference makeTypeReference(ITypeBinding binding) {
		if (binding.isPrimitive()) {
			return typeRefFactory.createPrimitiveTypeReference(binding.getName());
		}
		if (binding.isArray()) {
			return typeRefFactory.createArrayTypeReference(makeTypeReference(binding.getElementType()),
				binding.getDimensions());
		}
		if (binding.isParameterizedType()) {
			var tas = Arrays.stream(binding.getTypeArguments())
				.map(this::makeTypeReference)
				.toList();
			return typeRefFactory.createTypeReference(binding.getBinaryName(), tas);
		}
		if (binding.isTypeVariable()) {
			return typeRefFactory.createTypeParameterReference(binding.getName());
		}
		if (binding.isWildcardType()) {
			// FIXME
			return TypeReference.OBJECT;
		}
		return typeRefFactory.createTypeReference(roseauFqn(binding));
	}

	private boolean isAnonymousOrLocal(ASTNode node) {
		// If the parent is not a CompilationUnit or another TypeDeclaration, we consider it local.
		ASTNode parent = node.getParent();
		return !(parent instanceof CompilationUnit || parent instanceof AbstractTypeDeclaration);
	}
}
