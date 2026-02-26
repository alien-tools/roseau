package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.RoseauException;
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
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.extractors.ExtractorSink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExportsDirective;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

final class JdtApiVisitor extends ASTVisitor {
	private final CompilationUnit cu;
	private final String packageName;
	private final Path filePath;
	private final ExtractorSink sink;
	private final ApiFactory factory;
	private final Map<String, Integer> lineNumbersMapping = HashMap.newHashMap(100);

	private static final Logger LOGGER = LogManager.getLogger(JdtApiVisitor.class);

	JdtApiVisitor(CompilationUnit cu, Path filePath, ExtractorSink sink, ApiFactory factory) {
		this.cu = cu;
		this.filePath = filePath;
		this.sink = sink;
		this.factory = factory;
		this.packageName = Optional.ofNullable(cu.getPackage()).map(p -> p.getName().getFullyQualifiedName()).orElse("");
	}

	/*
	 * Bindings only hold semantic information. We need to visit the sub-ASTs to collect location information.
	 * We (arbitrarily) decide to use the symbol's name as location and use JDT's binding keys (which are unique).
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		IBinding binding = node.resolveBinding();
		if (binding != null) {
			lineNumbersMapping.put(binding.getKey(), cu.getLineNumber(node.getName().getStartPosition()));
		}

		return false;
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		IBinding binding = node.resolveBinding();
		if (binding != null) {
			lineNumbersMapping.put(binding.getKey(), cu.getLineNumber(node.getName().getStartPosition()));
		}

		return false;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		// Fragments: int i, j, k;
		stream(node.fragments(), VariableDeclarationFragment.class)
			.forEach(fragment -> {
				IBinding binding = fragment.resolveBinding();
				if (binding != null) {
					lineNumbersMapping.put(binding.getKey(), cu.getLineNumber(fragment.getName().getStartPosition()));
				}
			});

		return false;
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		IBinding binding = node.resolveVariable();
		if (binding != null) {
			lineNumbersMapping.put(binding.getKey(), cu.getLineNumber(node.getStartPosition()));
		}

		return false;
	}

	/*
	 * Forward all type visits to processAbstractTypeDeclaration
	 */
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
		Set<String> exports = stream(node.moduleStatements(), ExportsDirective.class)
			// No qualified exports
			.filter(export -> export.modules().isEmpty())
			.map(export -> export.getName().getFullyQualifiedName())
			.collect(toSet());

		sink.accept(factory.createModule(node.getName().getFullyQualifiedName(), exports));
	}

	private void processAbstractTypeDeclaration(AbstractTypeDeclaration type) {
		ITypeBinding binding = type.resolveBinding();
		if (binding == null) {
			LOGGER.warn("No binding for {}; skipping", () -> type.getName().getFullyQualifiedName());
			return;
		}

		// Anonymous and local classes should not be recorded
		if (binding.isAnonymous() || binding.isLocal()) {
			return;
		}

		String qualifiedName = makeFqn(binding);
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> modifiers = convertModifiers(binding.getModifiers());
		Set<Annotation> annotations = convertAnnotations(binding.getAnnotations());
		SourceLocation location = factory.location(filePath, cu.getLineNumber(type.getName().getStartPosition()));
		List<FormalTypeParameter> typeParams = convertTypeParameters(binding.getTypeParameters());
		Set<TypeReference<InterfaceDecl>> implemented = convertImplementedInterfaces(binding);
		Set<FieldDecl> fields = convertFields(binding, type);
		Set<MethodDecl> methods = convertMethods(binding, type);
		Set<ConstructorDecl> constructors = convertConstructors(binding, type);
		TypeReference<TypeDecl> enclosingType = createTypeReference(binding.getDeclaringClass());

		TypeDecl typeDecl = switch (type) {
			case TypeDeclaration c when !c.isInterface() -> {
				TypeReference<ClassDecl> superClass = createTypeReference(binding.getSuperclass());
				Set<TypeReference<TypeDecl>> permittedTypes = convertPermittedTypes(c);
				yield factory.createClass(qualifiedName, visibility, modifiers, annotations, location, implemented,
					typeParams, fields, methods, enclosingType, superClass, constructors, permittedTypes);
			}
			case TypeDeclaration i when i.isInterface() -> {
				Set<TypeReference<TypeDecl>> permittedTypes = convertPermittedTypes(i);
				yield factory.createInterface(qualifiedName, visibility, modifiers, annotations, location, implemented,
					typeParams, fields, methods, enclosingType, permittedTypes);
			}
			case EnumDeclaration e -> {
				// §8.9: an enum class E is implicitly sealed if its declaration contains at least one
				// enum constant that has a class body. Otherwise, final.
				if (stream(e.enumConstants(), EnumConstantDeclaration.class)
					.anyMatch(constant -> constant.getAnonymousClassDeclaration() != null)) {
					modifiers.add(Modifier.SEALED);
				} else {
					modifiers.add(Modifier.FINAL);
				}
				yield factory.createEnum(qualifiedName, visibility, modifiers, annotations, location, implemented,
					fields, methods, enclosingType, constructors);
			}
			case RecordDeclaration _ ->
				factory.createRecord(qualifiedName, visibility, modifiers, annotations, location, implemented,
					typeParams, fields, methods, enclosingType, constructors);
			case AnnotationTypeDeclaration _ -> {
				Set<AnnotationMethodDecl> annotationMethods = convertAnnotationMethods(binding);
				Set<ElementType> targets = convertAnnotationTargets(binding);
				yield factory.createAnnotation(qualifiedName, visibility, modifiers, annotations, location,
					fields, annotationMethods, enclosingType, targets);
			}
			default -> throw new RoseauException("Unexpected type kind: " + type.getClass());
		};

		sink.accept(typeDecl);
	}

	private Set<TypeReference<InterfaceDecl>> convertImplementedInterfaces(ITypeBinding binding) {
		return Arrays.stream(binding.getInterfaces())
			.map(this::<InterfaceDecl>createTypeReference)
			.collect(toSet());
	}

	private Set<FieldDecl> convertFields(ITypeBinding binding, AbstractTypeDeclaration type) {
		return Arrays.stream(binding.getDeclaredFields())
			.filter(field -> isExported(field, type))
			.map(field -> convertField(field, binding))
			.collect(toSet());
	}

	private Set<MethodDecl> convertMethods(ITypeBinding binding, AbstractTypeDeclaration type) {
		return Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> !method.isConstructor() && isExported(method, type))
			.map(method -> convertMethod(method, binding))
			.collect(toSet());
	}

	private Set<ConstructorDecl> convertConstructors(ITypeBinding binding, AbstractTypeDeclaration type) {
		return Arrays.stream(binding.getDeclaredMethods())
			.filter(method -> method.isConstructor() && isExported(method, type))
			.map(cons -> convertConstructor(cons, binding))
			.collect(toSet());
	}

	private Set<AnnotationMethodDecl> convertAnnotationMethods(ITypeBinding binding) {
		return Arrays.stream(binding.getDeclaredMethods())
			.filter(IMethodBinding::isAnnotationMember)
			.map(method -> convertAnnotationMethod(method, binding))
			.collect(toSet());
	}

	private FieldDecl convertField(IVariableBinding binding, ITypeBinding enclosingType) {
		ITypeReference fieldType = createITypeReference(binding.getType());
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> mods = convertModifiers(binding.getModifiers());
		Set<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(binding.getKey(), -1);
		SourceLocation location = factory.location(filePath, line);
		TypeReference<TypeDecl> enclosingTypeRef = createTypeReference(enclosingType);
		boolean compileTimeConstant = binding.getConstantValue() != null &&
			(fieldType instanceof PrimitiveTypeReference || fieldType.equals(TypeReference.STRING));

		return factory.createField(makeMemberFqn(enclosingType, binding), visibility, mods,
			anns, location, enclosingTypeRef, fieldType, compileTimeConstant);
	}

	private ConstructorDecl convertConstructor(IMethodBinding binding, ITypeBinding enclosingType) {
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> mods = convertModifiers(binding.getModifiers());
		Set<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(binding.getKey(), -1);
		SourceLocation location = factory.location(filePath, line);
		List<FormalTypeParameter> typeParams = convertTypeParameters(binding.getTypeParameters());
		Set<ITypeReference> thrownExceptions = convertThrownExceptions(binding.getExceptionTypes());
		TypeReference<TypeDecl> enclosingTypeRef = createTypeReference(enclosingType);
		List<ParameterDecl> params = convertParameters(binding.getParameterNames(), binding.getParameterTypes(),
			binding.isVarargs());

		if (binding.isCompactConstructor() && enclosingType instanceof RecordDeclaration rec) {
			params.addAll(
				stream(rec.recordComponents(), SingleVariableDeclaration.class)
					.map(variable -> factory.createParameter(variable.getName().getIdentifier(),
						createITypeReference(variable.getType().resolveBinding()), false))
					.toList());
		}

		return factory.createConstructor(makeMemberFqn(enclosingType, "<init>"), visibility, mods, anns, location,
			enclosingTypeRef, enclosingTypeRef, params, typeParams, thrownExceptions);
	}

	private MethodDecl convertMethod(IMethodBinding binding, ITypeBinding enclosingType) {
		AccessModifier visibility = convertVisibility(binding.getModifiers());
		Set<Modifier> mods = convertModifiers(binding.getModifiers());
		Set<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(binding.getKey(), -1);
		SourceLocation location = factory.location(filePath, line);
		List<FormalTypeParameter> typeParams = convertTypeParameters(binding.getTypeParameters());
		Set<ITypeReference> thrownExceptions = convertThrownExceptions(binding.getExceptionTypes());
		TypeReference<TypeDecl> enclosingTypeRef = createTypeReference(enclosingType);
		ITypeReference returnType = createITypeReference(binding.getReturnType());
		List<ParameterDecl> params = convertParameters(binding.getParameterNames(), binding.getParameterTypes(),
			binding.isVarargs());

		return factory.createMethod(makeMemberFqn(enclosingType, binding), visibility, mods, anns, location,
			enclosingTypeRef, returnType, params, typeParams, thrownExceptions);
	}

	private AnnotationMethodDecl convertAnnotationMethod(IMethodBinding binding, ITypeBinding enclosingType) {
		Set<Annotation> anns = convertAnnotations(binding.getAnnotations());
		int line = lineNumbersMapping.getOrDefault(binding.getKey(), -1);
		SourceLocation location = factory.location(filePath, line);
		TypeReference<TypeDecl> enclosingTypeRef = createTypeReference(enclosingType);
		ITypeReference returnType = createITypeReference(binding.getReturnType());
		boolean hasDefault = binding.getDefaultValue() != null;

		return factory.createAnnotationMethod(makeMemberFqn(enclosingType, binding), anns, location,
			enclosingTypeRef, returnType, hasDefault);
	}

	private List<ParameterDecl> convertParameters(String[] names, ITypeBinding[] types, boolean isVarargs) {
		// Transform JDT's m(A, B[]) into m(A, varargs B)
		// We may not always have a name (bytecode p0), so iterate on types
		return IntStream.range(0, types.length)
			.mapToObj(i -> {
				String name = names.length > i ? names[i] : "p" + i;
				if (isVarargs && i == types.length - 1) {
					return factory.createParameter(name, createITypeReference(types[i].getComponentType()), true);
				} else {
					return factory.createParameter(name, createITypeReference(types[i]), false);
				}
			})
			.toList();
	}

	private Set<ITypeReference> convertThrownExceptions(ITypeBinding[] exceptions) {
		return Arrays.stream(exceptions)
			.map(this::createITypeReference)
			.collect(toSet());
	}

	private Set<Annotation> convertAnnotations(IAnnotationBinding[] annotations) {
		return Arrays.stream(annotations)
			// Only retain RUNTIME/CLASS annotations to align with bytecode
			.filter(ann -> !isSourceAnnotation(ann))
			.map(ann -> {
				Map<String, String> values = new HashMap<>(ann.getDeclaredMemberValuePairs().length);
				for (IMemberValuePairBinding pair : ann.getDeclaredMemberValuePairs()) {
					String key = pair.getName();
					Object value = pair.getValue();
					if (value != null) {
						values.put(key, formatAnnotationValue(value));
					}
				}
				return factory.createAnnotation(createTypeReference(ann.getAnnotationType()), values);
			})
			.collect(toSet());
	}

	private List<FormalTypeParameter> convertTypeParameters(ITypeBinding[] typeParameters) {
		return Arrays.stream(typeParameters)
			.map(tp -> factory.createFormalTypeParameter(tp.getName(),
				Arrays.stream(tp.getTypeBounds()).map(this::createITypeReference).toList()))
			.toList();
	}

	private Set<TypeReference<TypeDecl>> convertPermittedTypes(TypeDeclaration type) {
		if (!org.eclipse.jdt.core.dom.Modifier.isSealed(type.getModifiers())) {
			return Set.of();
		}

		// Workaround: JDT does not include an implicit permitted types list (nested types) in permittedTypes()
		// and I can't find an equivalent utility in the binding.
		// We assume every nested type implementing the containing type is permitted.
		if (type.permittedTypes().isEmpty()) {
			ITypeBinding binding = type.resolveBinding();
			return Arrays.stream(binding.getDeclaredTypes())
				.filter(t -> t.isSubTypeCompatible(binding))
				.map(this::createTypeReference)
				.collect(toSet());
		}

		return stream(type.permittedTypes(), Type.class)
			.map(Type::resolveBinding)
			.filter(Objects::nonNull)
			.map(this::createTypeReference)
			.collect(toSet());
	}

	// Attempt to resolve a simpleName that's left unresolved by JDT
	// If we find an import that corresponds, use that import's fqn
	// Otherwise, assume it's a type from the current package
	private String lookupUnresolvedName(String simpleName) {
		return stream(cu.imports(), ImportDeclaration.class)
			.filter(id -> !id.isOnDemand())
			.map(id -> id.getName().getFullyQualifiedName())
			.filter(fqn -> fqn.endsWith("." + simpleName))
			.findFirst()
			.orElse(packageName.isEmpty() ? simpleName : (packageName + "." + simpleName));
	}

	private ITypeReference createITypeReference(ITypeBinding binding) {
		if (binding == null) {
			return null;
		}
		if (binding.isPrimitive()) {
			return factory.references().createPrimitiveTypeReference(binding.getName());
		}
		if (binding.isArray()) {
			return factory.references().createArrayTypeReference(
				createITypeReference(binding.getElementType()), binding.getDimensions());
		}
		if (binding.isTypeVariable()) {
			return factory.references().createTypeParameterReference(binding.getName());
		}
		if (binding.isWildcardType()) {
			if (binding.getBound() != null) {
				return factory.references().createWildcardTypeReference(
					List.of(createITypeReference(binding.getBound())), binding.isUpperbound());
			}
			return factory.references().createWildcardTypeReference(List.of(TypeReference.OBJECT), true);
		}
		return createTypeReference(binding);
	}

	private <T extends TypeDecl> TypeReference<T> createTypeReference(ITypeBinding binding) {
		if (binding == null) {
			return null;
		}
		if (binding.isParameterizedType()) {
			var tas = Arrays.stream(binding.getTypeArguments())
				.map(this::createITypeReference)
				.toList();
			return factory.references().createTypeReference(makeFqn(binding), tas);
		}
		// JDT attempts to recover the FQN of missing bindings; if it fails, the unresolved type
		// is assumed to be in the current package. Try to fall back to imported types, or the current package.
		if (binding.isRecovered() && binding.getQualifiedName().startsWith(packageName)) {
			return factory.references().createTypeReference(lookupUnresolvedName(binding.getName()));
		}
		return factory.references().createTypeReference(makeFqn(binding));
	}

	private static Set<ElementType> convertAnnotationTargets(ITypeBinding binding) {
		return Arrays.stream(binding.getAnnotations())
			.filter(ab -> {
				ITypeBinding annType = ab.getAnnotationType();
				return annType != null && Target.class.getCanonicalName().equals(annType.getQualifiedName());
			})
			.flatMap(ab -> Arrays.stream(ab.getAllMemberValuePairs()))
			.filter(pair -> "value".equals(pair.getName()))
			.flatMap(pair -> Arrays.stream((Object[]) pair.getValue()))
			.filter(IVariableBinding.class::isInstance)
			.map(IVariableBinding.class::cast)
			.map(IVariableBinding::getName)
			.map(ElementType::valueOf)
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(ElementType.class)));
	}

	// We need to carry the AbstractTypeDeclaration in the utilities below to account for
	// https://github.com/eclipse-jdt/eclipse.jdt.core/pull/3252
	private static boolean isExported(IVariableBinding field, AbstractTypeDeclaration containingType) {
		return org.eclipse.jdt.core.dom.Modifier.isPublic(field.getModifiers()) ||
			(org.eclipse.jdt.core.dom.Modifier.isProtected(field.getModifiers()) &&
				!isEffectivelyFinal(field.getDeclaringClass(), containingType));
	}

	private static boolean isExported(IMethodBinding method, AbstractTypeDeclaration containingType) {
		return org.eclipse.jdt.core.dom.Modifier.isPublic(method.getModifiers()) ||
			(org.eclipse.jdt.core.dom.Modifier.isProtected(method.getModifiers()) &&
				!isEffectivelyFinal(method.getDeclaringClass(), containingType));
	}

	private static boolean isSourceAnnotation(IAnnotationBinding ann) {
		ITypeBinding binding = ann.getAnnotationType();
		if (binding != null) {
			Optional<IAnnotationBinding> find = Arrays.stream(binding.getAnnotations())
				.filter(a -> Retention.class.getCanonicalName().equals(a.getAnnotationType().getQualifiedName()))
				.findFirst();

			if (find.isPresent()) {
				for (IMemberValuePairBinding pair : find.get().getDeclaredMemberValuePairs()) {
					if ("value".equals(pair.getName()) && pair.getValue() instanceof IVariableBinding vb) {
						return RetentionPolicy.SOURCE.name().equals(vb.getName());
					}
				}
			}
		}
		return false;
	}

	private static String formatAnnotationValue(Object value) {
		return switch (value) {
			// We don't store those
			case Object[] _ -> "{}";
			// Enum constant
			case IVariableBinding varBinding -> makeMemberFqn(varBinding.getDeclaringClass(), varBinding);
			// Class literal
			case ITypeBinding typeBinding -> makeFqn(typeBinding);
			default -> value.toString();
		};
	}

	private static boolean isEffectivelyFinal(ITypeBinding binding, AbstractTypeDeclaration type) {
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

	private static AccessModifier convertVisibility(int modifiers) {
		if (org.eclipse.jdt.core.dom.Modifier.isPublic(modifiers)) {
			return AccessModifier.PUBLIC;
		}
		if (org.eclipse.jdt.core.dom.Modifier.isProtected(modifiers)) {
			return AccessModifier.PROTECTED;
		}
		if (org.eclipse.jdt.core.dom.Modifier.isPrivate(modifiers)) {
			return AccessModifier.PRIVATE;
		}
		return AccessModifier.PACKAGE_PRIVATE;
	}

	private static Set<Modifier> convertModifiers(int modifiers) {
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

	// Convert JDT-style Outer.Inner to Roseau-style Outer$Inner
	private static String makeFqn(ITypeBinding type) {
		if (type.getDeclaringClass() == null) {
			return type.getBinaryName();
		}
		// FIXME: doubly-nested types' getName() can contain generics...
		int last = type.getName().indexOf('<');
		var simpleName = last > 0 ? type.getName().substring(0, last) : type.getName();
		return makeFqn(type.getDeclaringClass()) + "$" + simpleName;
	}

	private static String makeMemberFqn(ITypeBinding containingType, IBinding member) {
		return makeMemberFqn(containingType, member.getName());
	}

	private static String makeMemberFqn(ITypeBinding containingType, String simpleName) {
		return makeFqn(containingType) + "." + simpleName;
	}

	@SuppressWarnings("unchecked")
	private static <T> Stream<T> stream(List raw, Class<T> cls) {
		return raw.stream().filter(cls::isInstance);
	}
}
