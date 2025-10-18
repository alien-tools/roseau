package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.RoseauException;
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
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExportsDirective;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class JdtAPIVisitor extends ASTVisitor {
	private final List<TypeDecl> collectedTypeDecls = new ArrayList<>(10);
	private final List<ModuleDecl> collectedModuleDecls = new ArrayList<>(1);
	private final CompilationUnit cu;
	private final String packageName;
	private final Path filePath;
	private final TypeReferenceFactory typeRefFactory;
	private final Map<String, Integer> lineNumbersMapping = HashMap.newHashMap(10);

	private static final Logger LOGGER = LogManager.getLogger(JdtAPIVisitor.class);

	JdtAPIVisitor(CompilationUnit cu, String filePath, TypeReferenceFactory factory, Path basePath) {
		this.cu = cu;
		this.packageName = Optional.ofNullable(cu.getPackage()).map(p -> p.getName().getFullyQualifiedName()).orElse("");
		this.filePath = Optional.ofNullable(filePath).map(file -> basePath.relativize(Path.of(file))).orElse(null);
		this.typeRefFactory = factory;
	}

	List<TypeDecl> getCollectedTypeDecls() {
		return Collections.unmodifiableList(collectedTypeDecls);
	}

	List<ModuleDecl> getCollectedModuleDecls() {
		return Collections.unmodifiableList(collectedModuleDecls);
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		processAbstractTypeDeclaration(node);
	}

	@Override
	public void endVisit(RecordDeclaration node) {
		processAbstractTypeDeclaration(node);
	}

	@Override
	public void endVisit(EnumDeclaration node) {
		processAbstractTypeDeclaration(node);
	}

	@Override
	public void endVisit(AnnotationTypeDeclaration node) {
		processAbstractTypeDeclaration(node);
	}

	@Override
	public void endVisit(ModuleDeclaration node) {
		Set<String> exports = new HashSet<>();
		for (var stmt : node.moduleStatements()) {
			if (stmt instanceof ExportsDirective export && export.modules().isEmpty()) {
				exports.add(export.getName().getFullyQualifiedName());
			}
		}
		collectedModuleDecls.add(new ModuleDecl(node.getName().getFullyQualifiedName(), exports));
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		IMethodBinding binding = node.resolveBinding();
		if (binding != null) {
			// node.getStartPosition() includes leading comments/javadoc/annotations
			// so we (arbitrarily) decide that the method's name is the relevant one
			lineNumbersMapping.put(getFullyQualifiedName(binding), cu.getLineNumber(node.getName().getStartPosition()));
		}
		return false;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		node.fragments().forEach(fragment -> {
			if (fragment instanceof VariableDeclarationFragment vdf) {
				IVariableBinding binding = vdf.resolveBinding();
				if (binding != null) {
					lineNumbersMapping.put(getFullyQualifiedName(binding), cu.getLineNumber(vdf.getName().getStartPosition()));
				}
			}
		});
		return false;
	}

	private void processAbstractTypeDeclaration(AbstractTypeDeclaration type) {
		ITypeBinding binding = type.resolveBinding();
		if (binding == null) {
			LOGGER.warn("No binding for {}; skipping", type.getName().getFullyQualifiedName());
			return;
		}

		if (binding.isAnonymous() || binding.isLocal()) {
			return;
		}

		String qualifiedName = toRoseauFqn(binding);
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> modifiers = convertModifiers(binding.getModifiers());
		List<Annotation> annotations = convertAnnotations(binding.getAnnotations());
		SourceLocation location = new SourceLocation(filePath, cu.getLineNumber(type.getName().getStartPosition()));
		List<FormalTypeParameter> typeParams = convertTypeParameters(binding.getTypeParameters());

		// Not present in bindings: https://github.com/eclipse-jdt/eclipse.jdt.core/pull/3252
		if (org.eclipse.jdt.core.dom.Modifier.isSealed(type.getModifiers())) {
			modifiers.add(Modifier.SEALED);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isNonSealed(type.getModifiers())) {
			modifiers.add(Modifier.NON_SEALED);
		}

		// §8.9
		if (type instanceof EnumDeclaration enm &&
			((List<?>) enm.enumConstants()).stream().anyMatch(cons ->
				((EnumConstantDeclaration) cons).getAnonymousClassDeclaration() != null)) {
			modifiers.add(Modifier.SEALED);
		}

		List<TypeReference<InterfaceDecl>> implementedInterfaces = Arrays.stream(binding.getInterfaces())
			.map(intf -> (TypeReference<InterfaceDecl>) makeTypeReference(intf)).toList();

		TypeReference<ClassDecl> superClassRef = binding.isClass() && binding.getSuperclass() != null
			? (TypeReference<ClassDecl>) makeTypeReference(binding.getSuperclass())
			: null;

		List<FieldDecl> fields = Arrays.stream(binding.getDeclaredFields())
			.filter(field -> isExported(field, type))
			.map(field -> convertField(field, binding))
			.toList();

		List<MethodDecl> methods = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> !method.isConstructor() && isExported(method, type) &&
				!isEnumMethod(method) && !isSyntheticRecordMethod(method))
			.map(method -> convertMethod(method, binding))
			.toList();

		List<ConstructorDecl> constructors = Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> method.isConstructor() && isExported(method, type))
			.map(cons -> convertConstructor(cons, binding))
			.toList();

		var enclosingType = binding.getDeclaringClass() != null
			? (TypeReference<TypeDecl>) makeTypeReference(binding.getDeclaringClass())
			: null;

		TypeDecl typeDecl = switch (type) {
			case TypeDeclaration c when !c.isInterface() ->
				new ClassDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
					typeParams, fields, methods, enclosingType, superClassRef, constructors, convertPermittedTypes(c));
			case TypeDeclaration i when i.isInterface() -> {
				modifiers.add(Modifier.ABSTRACT);
				yield new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
					typeParams, fields, methods, enclosingType, convertPermittedTypes(i));
			}
			case EnumDeclaration e ->
				new EnumDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
					fields, methods, enclosingType, constructors, List.of());
			case RecordDeclaration r ->
				new RecordDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
					typeParams, fields, methods, enclosingType, constructors, List.of());
			case AnnotationTypeDeclaration a -> {
				modifiers.add(Modifier.ABSTRACT);
				List<AnnotationMethodDecl> annotationMethodDecls = Arrays.stream(binding.getDeclaredMethods())
					.map(method -> convertAnnotationMethod(method, binding))
					.toList();
				Set<ElementType> targets = convertAnnotationTargets(binding);
				yield new AnnotationDecl(qualifiedName, visibility, modifiers, annotations, location,
					fields, annotationMethodDecls, enclosingType, targets);
			}
			default -> throw new RoseauException("Unexpected type kind: " + type.getClass());
		};

		collectedTypeDecls.add(typeDecl);
	}

	private FieldDecl convertField(IVariableBinding binding, ITypeBinding enclosingType) {
		ITypeReference fieldType = makeTypeReference(binding.getType());
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> mods = convertModifiers(binding.getModifiers());
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(getFullyQualifiedName(binding), -1);
		SourceLocation location = new SourceLocation(filePath, line);
		TypeReference<TypeDecl> enclosingTypeRef = typeRefFactory.createTypeReference(toRoseauFqn(enclosingType));

		return new FieldDecl(toRoseauFqn(enclosingType) + "." + binding.getName(), visibility, mods,
			anns, location, enclosingTypeRef, fieldType);
	}

	private ConstructorDecl convertConstructor(IMethodBinding binding, ITypeBinding enclosingType) {
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> mods = convertModifiers(binding.getModifiers());
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(getFullyQualifiedName(binding), -1);
		SourceLocation location = new SourceLocation(filePath, line);
		List<FormalTypeParameter> typeParams = convertTypeParameters(binding.getTypeParameters());
		List<ITypeReference> thrownExceptions = convertThrownExceptions(binding.getExceptionTypes());
		TypeReference<TypeDecl> enclosingTypeRef = typeRefFactory.createTypeReference(toRoseauFqn(enclosingType));
		List<ParameterDecl> params = convertParameters(binding.getParameterNames(), binding.getParameterTypes(),
			binding.isVarargs());

		if (binding.isCompactConstructor() && enclosingType instanceof RecordDeclaration rec) {
			params.addAll(
				rec.recordComponents().stream()
					.filter(SingleVariableDeclaration.class::isInstance)
					.map(o -> {
						SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
						return new ParameterDecl(svd.getName().getIdentifier(),
							makeTypeReference(svd.getType().resolveBinding()), false);
					})
					.toList()
			);
		}

		return new ConstructorDecl(toRoseauFqn(enclosingType) + ".<init>", visibility, mods, anns, location,
			enclosingTypeRef, enclosingTypeRef, params, typeParams, thrownExceptions);
	}

	private MethodDecl convertMethod(IMethodBinding binding, ITypeBinding enclosingType) {
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> mods = convertModifiers(binding.getModifiers());
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(getFullyQualifiedName(binding), -1);
		SourceLocation location = new SourceLocation(filePath, line);
		List<FormalTypeParameter> typeParams = convertTypeParameters(binding.getTypeParameters());
		List<ITypeReference> thrownExceptions = convertThrownExceptions(binding.getExceptionTypes());
		TypeReference<TypeDecl> enclosingTypeRef = typeRefFactory.createTypeReference(toRoseauFqn(enclosingType));
		ITypeReference returnType = makeTypeReference(binding.getReturnType());
		List<ParameterDecl> params = convertParameters(binding.getParameterNames(), binding.getParameterTypes(),
			binding.isVarargs());

		return new MethodDecl(toRoseauFqn(enclosingType) + "." + binding.getName(), visibility, mods, anns, location,
			enclosingTypeRef, returnType, params, typeParams, thrownExceptions);
	}

	private AnnotationMethodDecl convertAnnotationMethod(IMethodBinding binding, ITypeBinding enclosingType) {
		List<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(getFullyQualifiedName(binding), -1);
		SourceLocation location = new SourceLocation(filePath, line);
		TypeReference<TypeDecl> enclosingTypeRef = typeRefFactory.createTypeReference(toRoseauFqn(enclosingType));
		ITypeReference returnType = makeTypeReference(binding.getReturnType());
		boolean hasDefault = binding.getDefaultValue() != null;

		return new AnnotationMethodDecl(toRoseauFqn(enclosingType) + "." + binding.getName(), anns, location,
			enclosingTypeRef, returnType, hasDefault);
	}

	private List<ParameterDecl> convertParameters(String[] names, ITypeBinding[] types, boolean isVarargs) {
		var params = new ArrayList<ParameterDecl>();
		for (int i = 0; i < names.length; i++) {
			if (isVarargs && i == names.length - 1) {
				params.add(new ParameterDecl(names[i], makeTypeReference(types[i].getComponentType()), true));
			} else {
				params.add(new ParameterDecl(names[i], makeTypeReference(types[i]), false));
			}
		}
		return params;
	}

	private List<ITypeReference> convertThrownExceptions(ITypeBinding[] exceptions) {
		return Arrays.stream(exceptions)
			.map(this::makeTypeReference)
			.toList();
	}

	private List<Annotation> convertAnnotations(IAnnotationBinding[] annotations) {
		return Arrays.stream(annotations)
			.map(ann -> {
				Map<String, String> values = new HashMap<>();
				for (IMemberValuePairBinding pair : ann.getAllMemberValuePairs()) {
					String key = pair.getName();
					Object value = pair.getValue();
					if (value != null) {
						values.put(key, formatAnnotationValue(value));
					}
				}
				return new Annotation((TypeReference<AnnotationDecl>) makeTypeReference(ann.getAnnotationType()), values);
			})
			.toList();
	}

	private String formatAnnotationValue(Object value) {
		return switch (value) {
			case Object[] array -> Arrays.stream(array)
				.map(this::formatAnnotationValue)
				.toList()
				.toString();
			case IVariableBinding varBinding ->
				// Enum constant
				varBinding.getDeclaringClass().getQualifiedName() + "." + varBinding.getName();
			case ITypeBinding typeBinding ->
				// Class literal
				typeBinding.getQualifiedName();
			default -> value.toString();
		};
	}

	private List<FormalTypeParameter> convertTypeParameters(ITypeBinding[] typeParameters) {
		return Arrays.stream(typeParameters)
			.map(tp -> new FormalTypeParameter(tp.getName(),
				Arrays.stream(tp.getTypeBounds()).map(this::makeTypeReference).toList()))
			.toList();
	}

	private List<TypeReference<TypeDecl>> convertPermittedTypes(TypeDeclaration type) {
		return ((List<org.eclipse.jdt.core.dom.Type>) type.permittedTypes()).stream()
			.map(Type::resolveBinding)
			.filter(Objects::nonNull)
			.map(t -> typeRefFactory.createTypeReference(t.getQualifiedName()))
			.toList();
	}

	// Convert JDT-style Outer.Inner to Roseau-style Outer$Inner
	private String toRoseauFqn(ITypeBinding type) {
		if (type.getDeclaringClass() == null) {
			return type.getBinaryName();
		}
		// FIXME: doubly-nested types' getName() can contain generics...
		int last = type.getName().indexOf('<');
		var simpleName = last > 0 ? type.getName().substring(0, last) : type.getName();
		return toRoseauFqn(type.getDeclaringClass()) + "$" + simpleName;
	}

	// FIXME: we need to carry the AbstractTypeDeclaration in the utilities below to account for
	// https://github.com/eclipse-jdt/eclipse.jdt.core/pull/3252

	private boolean isExported(IVariableBinding field, AbstractTypeDeclaration containingType) {
		return org.eclipse.jdt.core.dom.Modifier.isPublic(field.getModifiers()) ||
			(org.eclipse.jdt.core.dom.Modifier.isProtected(field.getModifiers()) &&
				!isEffectivelyFinal(field.getDeclaringClass(), containingType));
	}

	private boolean isExported(IMethodBinding method, AbstractTypeDeclaration containingType) {
		return org.eclipse.jdt.core.dom.Modifier.isPublic(method.getModifiers()) ||
			(org.eclipse.jdt.core.dom.Modifier.isProtected(method.getModifiers()) &&
				!isEffectivelyFinal(method.getDeclaringClass(), containingType));
	}

	private boolean isEffectivelyFinal(ITypeBinding binding, AbstractTypeDeclaration type) {
		if (binding.isEnum() || binding.isRecord()) {
			return true;
		}

		if (binding.isClass()) {
			var cons = Arrays.stream(binding.getDeclaredMethods())
				.filter(IMethodBinding::isConstructor)
				.toList();

			if (!cons.isEmpty() &&
				cons.stream().allMatch(c -> org.eclipse.jdt.core.dom.Modifier.isPrivate(c.getModifiers()))) {
				return true;
			}
		}

		var isFinal = org.eclipse.jdt.core.dom.Modifier.isFinal(type.getModifiers());
		var isSealed = org.eclipse.jdt.core.dom.Modifier.isSealed(type.getModifiers());
		var isNonSealed = org.eclipse.jdt.core.dom.Modifier.isNonSealed(type.getModifiers());

		return (isFinal || isSealed) && !isNonSealed;
	}

	private boolean isEnumMethod(IMethodBinding binding) {
		if (binding.getDeclaringClass().isEnum()) {
			if ("valueOf".equals(binding.getName()) && binding.getParameterTypes().length == 1 &&
				"java.lang.String".equals(binding.getParameterTypes()[0].getQualifiedName())) {
				return true;
			}
			return "values".equals(binding.getName()) && binding.getParameterTypes().length == 0;
		}
		return false;
	}

	// We want the accessors, not the equals/hashCode/toString unless explicitly overridden...
	private boolean isSyntheticRecordMethod(IMethodBinding binding) {
		return binding.isSyntheticRecordMethod() &&
			Set.of("toString", "equals", "hashCode").contains(binding.getName());
	}

	// Available on TypeDeclaration, but not Enum/Record/Annotation, even though they can contain inner types
	// The original implementation only returns inner Class/Interface, not Enum/Record/Annotation
	// ITypeBinding properly implements inner types, but we need to visit ASTs (currently...)
	// private List<AbstractTypeDeclaration> getInnerTypes(AbstractTypeDeclaration type) {
	//   return type.bodyDeclarations().stream()
	//   .filter(AbstractTypeDeclaration.class::isInstance)
	//   .toList();
	// }

	private Set<ElementType> convertAnnotationTargets(ITypeBinding binding) {
		Set<ElementType> targets = new HashSet<>();
		for (IAnnotationBinding ab : binding.getAnnotations()) {
			ITypeBinding annType = ab.getAnnotationType();
			if (annType != null && Target.class.getCanonicalName().equals(annType.getQualifiedName())) {
				IMemberValuePairBinding[] pairs = ab.getAllMemberValuePairs();
				for (IMemberValuePairBinding pair : pairs) {
					if ("value".equals(pair.getName())) {
						Object[] vals = (Object[]) pair.getValue();
						for (Object v : vals) {
							if (v instanceof IVariableBinding enumConst) {
								String elemTypeName = enumConst.getName(); // e.g. "METHOD", "TYPE"
								targets.add(ElementType.valueOf(elemTypeName));
							}
						}
					}
				}
			}
		}
		return targets;
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
		if (org.eclipse.jdt.core.dom.Modifier.isStatic(modifiers)) {
			result.add(Modifier.STATIC);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isFinal(modifiers)) {
			result.add(Modifier.FINAL);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isAbstract(modifiers)) {
			result.add(Modifier.ABSTRACT);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isSynchronized(modifiers)) {
			result.add(Modifier.SYNCHRONIZED);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isVolatile(modifiers)) {
			result.add(Modifier.VOLATILE);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isTransient(modifiers)) {
			result.add(Modifier.TRANSIENT);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isNative(modifiers)) {
			result.add(Modifier.NATIVE);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isStrictfp(modifiers)) {
			result.add(Modifier.STRICTFP);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isSealed(modifiers)) {
			result.add(Modifier.SEALED);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isNonSealed(modifiers)) {
			result.add(Modifier.NON_SEALED);
		}
		if (org.eclipse.jdt.core.dom.Modifier.isDefault(modifiers)) {
			result.add(Modifier.DEFAULT);
		}
		return result;
	}

	private String lookupUnresolvedName(String simpleName) {
		List<?> imports = cu.imports();
		for (Object imp : imports) {
			// Only consider single type imports.
			if (imp instanceof ImportDeclaration id && !id.isOnDemand()) {
				String fqn = id.getName().getFullyQualifiedName();
				// If the fully qualified name ends with '.' + simpleName, we assume a match.
				if (fqn.endsWith("." + simpleName)) {
					return fqn;
				}
			}
		}
		// Otherwise, assume it's a same-package type
		return packageName.isEmpty() ? simpleName : (packageName + "." + simpleName);
	}

	private ITypeReference makeTypeReference(ITypeBinding binding) {
		if (binding.isPrimitive()) {
			return typeRefFactory.createPrimitiveTypeReference(binding.getName());
		}
		if (binding.isArray()) {
			return typeRefFactory.createArrayTypeReference(
				makeTypeReference(binding.getElementType()), binding.getDimensions());
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
			if (binding.getBound() != null) {
				return typeRefFactory.createWildcardTypeReference(
					List.of(makeTypeReference(binding.getBound())), binding.isUpperbound());
			}
			return typeRefFactory.createWildcardTypeReference(List.of(TypeReference.OBJECT), true);
		}
		// JDT attempts to recover the FQN of missing bindings; if it fails, the unresolved type
		// is assumed to be in the current package. Try to fall back to imported types, or the current package.
		if (binding.isRecovered() && binding.getQualifiedName().startsWith(packageName)) {
			return typeRefFactory.createTypeReference(lookupUnresolvedName(binding.getName()));
		}
		return typeRefFactory.createTypeReference(toRoseauFqn(binding));
	}

	private String getFullyQualifiedName(IMethodBinding method) {
		return "%s#%s(%s)".formatted(
			method.getDeclaringClass().getQualifiedName(),
			method.getName(),
			Arrays.stream(method.getParameterTypes()).map(Object::toString).toList()
		);
	}

	private String getFullyQualifiedName(IVariableBinding field) {
		return field.getDeclaringClass().getQualifiedName() + '#' + field.getName();
	}
}
