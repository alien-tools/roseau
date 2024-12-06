package com.github.maracas.roseau.combinatorial;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.Annotation;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.visit.APIPrettyPrinter;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.maracas.roseau.api.model.AccessModifier.PACKAGE_PRIVATE;
import static com.github.maracas.roseau.api.model.AccessModifier.PRIVATE;
import static com.github.maracas.roseau.api.model.AccessModifier.PROTECTED;
import static com.github.maracas.roseau.api.model.AccessModifier.PUBLIC;
import static com.github.maracas.roseau.api.model.Modifier.ABSTRACT;
import static com.github.maracas.roseau.api.model.Modifier.DEFAULT;
import static com.github.maracas.roseau.api.model.Modifier.FINAL;
import static com.github.maracas.roseau.api.model.Modifier.NATIVE;
import static com.github.maracas.roseau.api.model.Modifier.NON_SEALED;
import static com.github.maracas.roseau.api.model.Modifier.SEALED;
import static com.github.maracas.roseau.api.model.Modifier.STATIC;
import static com.github.maracas.roseau.api.model.Modifier.SYNCHRONIZED;

public class Combinatorial {
	static final List<AccessModifier> topLevelVisibilities = List.of(PUBLIC, PACKAGE_PRIVATE);

	// STATIC handled separately for nested types only
	static final Set<Set<Modifier>> classModifiers  = Sets.powerSet(Set.of(FINAL, ABSTRACT/*, SEALED, NON_SEALED*/)) // TODO: sealed
		.stream()
		.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
		.filter(mods -> !mods.containsAll(Set.of(ABSTRACT, FINAL)))
		.filter(mods -> !mods.contains(FINAL) || Sets.intersection(mods, Set.of(ABSTRACT/*, SEALED, NON_SEALED*/)).isEmpty()) // TODO: sealed
		.collect(Collectors.toSet());
	static final Set<Set<Modifier>> interfaceModifiers = Sets.powerSet(Set.of(ABSTRACT/*, SEALED, NON_SEALED*/)) // TODO: sealed
		.stream()
		.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
		.collect(Collectors.toSet());
	static final Set<Set<Modifier>> recordModifiers = Sets.powerSet(Set.of(FINAL));
	static final Set<Set<Modifier>> enumModifiers   = Sets.powerSet(Set.of());

	static final List<ITypeReference> fieldTypes = List.of(
		new PrimitiveTypeReference("int")/*,
		new TypeReference<>("java.lang.Integer"),
		new TypeReference<>("java.lang.String"),
		new ArrayTypeReference(new PrimitiveTypeReference("int"), 1)*/
	);
	static final List<ITypeReference> methodTypes = fieldTypes;
	static final Set<Set<TypeReference<ClassDecl>>> thrownExceptions = Sets.powerSet(Set.of(TypeReference.EXCEPTION /* No throws for unchecked: TypeReference.RUNTIME_EXCEPTION*/));

	static final int typeHierarchyDepth = 2;
	static final int typeHierarchyWidth = 2;
	static final int paramsCount = 1;
	static final Path dst = Path.of("generated/v1");
	static int i = 0;

	Map<String, TypeDeclBuilder> typeStore = new HashMap<>();

	<T> Set<Set<T>> powerSet(T... elements) {
		return Sets.powerSet(Set.of(elements));
	}

	Set<AccessModifier> fieldVisibilities(Builder container) {
		return switch (container) {
			case InterfaceBuilder i -> Set.of(PUBLIC, PACKAGE_PRIVATE);
			default -> Set.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
		};
	}

	Set<AccessModifier> methodVisibilities(Builder container) {
		return switch (container) {
			case InterfaceBuilder i -> Set.of(PUBLIC, /*PACKAGE_PRIVATE,*/ PRIVATE);
			default -> Set.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);
		};
	}

	Set<Set<Modifier>> fieldModifiers(Builder container) {
		return switch (container) {
			default -> powerSet(STATIC, FINAL);
		};
	}

	Set<Set<Modifier>> methodModifiers(Builder container) {
		var modifiers = powerSet(STATIC, FINAL, ABSTRACT, NATIVE, DEFAULT, SYNCHRONIZED)
			.stream()
			.filter(mods -> !mods.containsAll(Set.of(FINAL, ABSTRACT)))
			.filter(mods -> !mods.contains(ABSTRACT) || Sets.intersection(mods, Set.of(NATIVE, STATIC, SYNCHRONIZED)).isEmpty())
			.filter(mods -> !mods.contains(DEFAULT) || Sets.intersection(mods, Set.of(STATIC, ABSTRACT)).isEmpty())
			.collect(Collectors.toSet());

		return switch (container) {
			case InterfaceBuilder b -> modifiers.stream()
				.filter(mods -> Sets.intersection(mods, Set.of(NATIVE, SYNCHRONIZED, FINAL)).isEmpty())
				.collect(Collectors.toSet());
			case ClassBuilder b -> modifiers.stream()
				.filter(mods -> !mods.contains(DEFAULT))
				.filter(mods -> b.make().isAbstract() || !mods.contains(ABSTRACT))
				.collect(Collectors.toSet());
			default -> modifiers;
		};
	}

	void createTypes() {
		createInterfaces();
		createClasses();
	}

	void createHierarchies() {
		List.copyOf(typeStore.values()).forEach(t -> {
			if (t instanceof ClassBuilder c)
				createSubtypes(c);
			if (t instanceof InterfaceBuilder i)
				createSubtypes(i);
		});
	}

	void createSubtypes(InterfaceBuilder intf) {
		var api = getAPI();
		var intfDecl = (InterfaceDecl) api.findType(intf.qualifiedName).get();
		createOverridingInterface(intfDecl);
		createImplementingClass(intfDecl);
	}

	void createSubtypes(ClassBuilder cls) {
		var api = getAPI();
		var clsDecl = (ClassDecl) api.findType(cls.qualifiedName).get();
		if (!clsDecl.getModifiers().contains(FINAL)) // isEffectivelyFinal always true cause there are no constructors
			createSubclass(clsDecl);
	}

	void createOverridingInterface(InterfaceDecl intf) {
		topLevelVisibilities.forEach(visibility ->
			interfaceModifiers.forEach(modifiers -> {
				var builder = new InterfaceBuilder();
				builder.qualifiedName = "I" + ++i;
				builder.visibility = visibility;
				builder.modifiers = toEnumSet(modifiers, Modifier.class);
				builder.implementedInterfaces.add(new TypeReference<>(intf.getQualifiedName()));
				intf.getAllMethods().forEach(m -> {
					// @Override ann?
					var mBuilder = new MethodBuilder();
					mBuilder.qualifiedName = builder.qualifiedName + "." + m.getSimpleName();
					mBuilder.visibility = m.getVisibility();
					mBuilder.modifiers = toEnumSet(m.getModifiers(), Modifier.class);
					if (!builder.make().isAbstract())
						mBuilder.modifiers.remove(ABSTRACT);
					mBuilder.type = m.getType();
					mBuilder.containingType = new TypeReference<>(builder.qualifiedName);
					mBuilder.thrownExceptions = m.getThrownExceptions();
					m.getParameters().forEach(p -> {
						mBuilder.parameters.add(p);
					});
					builder.methods.add(mBuilder.make());
				});
				// Field shadowing?
				store(builder);
			})
		);
	}

	void createImplementingClass(InterfaceDecl intf) {
		topLevelVisibilities.forEach(visibility ->
			classModifiers.forEach(modifiers -> {
				var builder = new ClassBuilder();
				builder.qualifiedName = "C" + ++i;
				builder.visibility = visibility;
				builder.modifiers = toEnumSet(modifiers, Modifier.class);
				builder.implementedInterfaces.add(new TypeReference<>(intf.getQualifiedName()));
				intf.getAllMethods().forEach(m -> {
					// @Override ann?
					var mBuilder = new MethodBuilder();
					mBuilder.qualifiedName = builder.qualifiedName + "." + m.getSimpleName();
					mBuilder.visibility = m.getVisibility();
					mBuilder.modifiers = toEnumSet(m.getModifiers(), Modifier.class);
					if (!builder.make().isAbstract())
						mBuilder.modifiers.remove(ABSTRACT);
					mBuilder.modifiers.remove(DEFAULT);
					mBuilder.type = m.getType();
					mBuilder.containingType = new TypeReference<>(builder.qualifiedName);
					mBuilder.thrownExceptions = m.getThrownExceptions();
					m.getParameters().forEach(p -> {
						mBuilder.parameters.add(p);
					});
					builder.methods.add(mBuilder.make());
				});
				// Field shadowing?
				store(builder);
			})
		);
	}

	void createSubclass(ClassDecl cls) {
		topLevelVisibilities.forEach(visibility ->
			classModifiers.forEach(modifiers -> {
				var builder = new ClassBuilder();
				builder.qualifiedName = "C" + ++i;
				builder.visibility = visibility;
				builder.modifiers = toEnumSet(modifiers, Modifier.class);
				builder.superClass = new TypeReference<>(cls.getQualifiedName());
				cls.getAllMethods()
					.filter(m -> !m.isFinal())
					.forEach(m -> {
					// @Override ann?
						var mBuilder = new MethodBuilder();
						mBuilder.qualifiedName = builder.qualifiedName + "." + m.getSimpleName();
						mBuilder.visibility = m.getVisibility();
						mBuilder.modifiers = toEnumSet(m.getModifiers(), Modifier.class);
						if (!builder.make().isAbstract())
							mBuilder.modifiers.remove(ABSTRACT);
						mBuilder.modifiers.remove(DEFAULT);
						mBuilder.type = m.getType();
						mBuilder.containingType = new TypeReference<>(builder.qualifiedName);
						mBuilder.thrownExceptions = m.getThrownExceptions();
						m.getParameters().forEach(p -> {
							mBuilder.parameters.add(p);
						});
						builder.methods.add(mBuilder.make());
					});
				// Field shadowing?
				store(builder);
			})
		);
	}

	void createInterfaces() {
		topLevelVisibilities.forEach(visibility ->
			interfaceModifiers.forEach(modifiers -> {
				var builder = new InterfaceBuilder();
				builder.qualifiedName = "I" + ++i;
				builder.visibility = visibility;
				builder.modifiers = toEnumSet(modifiers, Modifier.class);
				store(builder);

				// [O, 1, N]?
				IntStream.range(0, typeHierarchyWidth).forEach(intf -> {
					var superBuilder = new InterfaceBuilder();
					superBuilder.qualifiedName = "I" + ++i;
					superBuilder.visibility = visibility;
					superBuilder.modifiers = toEnumSet(modifiers, Modifier.class);
					builder.implementedInterfaces.add(new TypeReference<>(superBuilder.qualifiedName));
					store(superBuilder);
				});
			})
		);
	}

	void createClasses() {
		topLevelVisibilities.forEach(visibility ->
			classModifiers.forEach(modifiers -> {
				var builder = new ClassBuilder();
				builder.qualifiedName = "C" + ++i;
				builder.visibility = visibility;
				builder.modifiers = toEnumSet(modifiers, Modifier.class);
				store(builder);
			})
		);
	}

	private void weaveFields() {
		typeStore.forEach((fqn, t) ->
			fieldVisibilities(t).stream().forEach(visibility ->
				fieldModifiers(t).stream().forEach(modifiers ->
					fieldTypes.stream().forEach(type -> {
						var builder = new FieldBuilder();
						builder.qualifiedName = t.qualifiedName + ".f" + ++i;
						builder.visibility = visibility;
						builder.modifiers = toEnumSet(modifiers, Modifier.class);
						builder.type = type;
						builder.containingType = new TypeReference<>(t.qualifiedName);
						t.fields.add(builder.make());
					})
				)
			)
		);
	}

	void weaveMethods() {
		// TODO: overloading
		typeStore.forEach((fqn, t) ->
			methodVisibilities(t).stream().forEach(visibility ->
				methodModifiers(t).stream().forEach(modifiers ->
					methodTypes.stream().forEach(type ->
						thrownExceptions.stream().forEach(exc ->
							IntStream.range(0, paramsCount + 1).forEach(pCount -> {
								/* vis/mod interactions are annoying; don't want them there */
								if (visibility == PRIVATE && !Sets.intersection(modifiers, Set.of(DEFAULT, ABSTRACT)).isEmpty())
									return;

								var builder = new MethodBuilder();
								builder.qualifiedName = t.qualifiedName + ".m" + ++i;
								builder.visibility = visibility;
								builder.modifiers = toEnumSet(modifiers, Modifier.class);
								builder.type = type;
								builder.containingType = new TypeReference<>(t.qualifiedName);
								builder.thrownExceptions = exc.stream().toList();
								IntStream.range(0, pCount).forEach(p -> {
									// TODO: varargs
									// TODO: parameter types
									builder.parameters.add(new ParameterDecl("p" + p, TypeReference.OBJECT, false));
								});
								t.methods.add(builder.make());
							})
						)
					)
				)
			)
		);
	}

	void store(TypeDeclBuilder type) {
		typeStore.put(type.qualifiedName, type);
	}

	private <T extends Enum<T>> EnumSet<T> toEnumSet(Set<T> set, Class<T> cls) {
		return set.isEmpty()
			? EnumSet.noneOf(cls)
			: EnumSet.copyOf(set);
	}

	public void generateCode(API api) {
		var outputDirFile = dst.toFile();
        var packageName = dst.toString().replace('/', '.');

		if (outputDirFile.exists()) {
			try {
				FileUtils.cleanDirectory(outputDirFile);
			} catch (IOException e) {
				System.err.println("Error cleaning output directory: " + e.getMessage());
			}
		} else {
			outputDirFile.mkdirs();
		}

		var prettyPrinter = new APIPrettyPrinter();

		api.getAllTypes().forEach(t -> {
			var file = dst.resolve(t.getQualifiedName().replace('.', '/') + ".java");

			try {
				var code = prettyPrinter.$(t).print();

				file.toFile().createNewFile();
				Files.writeString(file, "package %s;\n\n%s".formatted(packageName, code));
				System.out.println("Generated " + file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public API getAPI() {
		return new API(typeStore.values().stream().map(TypeDeclBuilder::make).toList(), new SpoonAPIFactory());
	}

	public static void main(String[] args) {
		var comb = new Combinatorial();
		comb.createTypes();
		comb.weaveFields();
		comb.weaveMethods();
		comb.createHierarchies();
		var api = comb.getAPI();
		System.out.println("api="+api);
		comb.generateCode(api);
	}

	/* ... */
	interface Builder { TypeDecl make(); }
	class SymbolBuilder {
		String qualifiedName;
		AccessModifier visibility;
		EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
		List<Annotation> annotations = new ArrayList<>();
		SourceLocation location = SourceLocation.NO_LOCATION;
	}

	abstract class TypeDeclBuilder extends SymbolBuilder implements Builder {
		List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
		List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
		List<FieldDecl> fields = new ArrayList<>();
		List<MethodDecl> methods = new ArrayList<>();
		TypeReference<TypeDecl> enclosingType;
	}

	class InterfaceBuilder extends TypeDeclBuilder {
		public InterfaceDecl make() {
			return new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
		}
	}

	class ClassBuilder extends TypeDeclBuilder {
		TypeReference<ClassDecl> superClass;
		List<ConstructorDecl> constructors = new ArrayList<>();

		public ClassDecl make() {
			return new ClassDecl(qualifiedName, visibility, modifiers, annotations, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, superClass,
				constructors);
		}
	}

	class TypeMemberBuilder extends SymbolBuilder {
		TypeReference<TypeDecl> containingType;
		ITypeReference type;
	}

	class FieldBuilder extends TypeMemberBuilder {
		FieldDecl make() {
			return new FieldDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type);
		}
	}

	class ExecutableBuilder extends TypeMemberBuilder {
		List<ParameterDecl> parameters = new ArrayList<>();
		List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
		List<TypeReference<ClassDecl>> thrownExceptions = new ArrayList<>();
	}

	class MethodBuilder extends ExecutableBuilder {
		MethodDecl make() {
			return new MethodDecl(qualifiedName, visibility, modifiers, annotations, location,
				containingType, type, parameters, formalTypeParameters, thrownExceptions);
		}
	}
}
