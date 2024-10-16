package com.github.maracas.roseau.combinatorial;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.Annotation;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.visit.APIPrettyPrinter;
import com.google.common.collect.Sets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
	static final List<AccessModifier> topLevelVisibilities = List.of(PUBLIC, AccessModifier.PACKAGE_PRIVATE);
	static final List<AccessModifier> nestedVisibilities   = List.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);

	static final Set<Set<Modifier>> fieldModifiers  = Sets.powerSet(Set.of(STATIC, FINAL));
	static final Set<Set<Modifier>> methodModifiers = Sets.powerSet(Set.of(STATIC, FINAL, ABSTRACT, NATIVE, DEFAULT, SYNCHRONIZED))
		.stream()
		.filter(mods -> !mods.containsAll(Set.of(FINAL, ABSTRACT)))
		.filter(mods -> !mods.contains(ABSTRACT) || Sets.intersection(mods, Set.of(NATIVE, STATIC, SYNCHRONIZED)).isEmpty())
		.filter(mods -> !mods.contains(DEFAULT) || Sets.intersection(mods, Set.of(STATIC, ABSTRACT)).isEmpty())
		.collect(Collectors.toSet());
	static final Set<Set<Modifier>> classModifiers  = Sets.powerSet(Set.of(STATIC, FINAL, ABSTRACT, SEALED, NON_SEALED))
		.stream()
		.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
		.filter(mods -> !mods.containsAll(Set.of(ABSTRACT, FINAL)))
		.filter(mods -> !mods.contains(FINAL) || Sets.intersection(mods, Set.of(ABSTRACT, SEALED, NON_SEALED)).isEmpty())
		.collect(Collectors.toSet());
	static final Set<Set<Modifier>> interfaceModifiers = Sets.powerSet(Set.of(STATIC, ABSTRACT, SEALED, NON_SEALED))
		.stream()
		.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
		.collect(Collectors.toSet());
	static final Set<Set<Modifier>> recordModifiers = Sets.powerSet(Set.of(STATIC, FINAL));
	static final Set<Set<Modifier>> enumModifiers   = Sets.powerSet(Set.of(STATIC));

	static final List<ITypeReference> fieldTypes = List.of(new PrimitiveTypeReference("int"));

	static final int typeHierarchyDepth = 2;
	static final Path dst = Path.of("generated");
	static int i = 0;

	Map<String, TypeDecl> typeStore = new HashMap<>();

	void createInterface() {
		// Modifiers, visibilities
		topLevelVisibilities.stream()
			.flatMap(visibility -> interfaceModifiers.stream()
				.flatMap(modifiers -> IntStream.range(0, typeHierarchyDepth)
					.mapToObj(depth -> {
						var implemented = IntStream.range(0, depth)
							.mapToObj(i -> newInterface("I" + i++, PUBLIC, Collections.emptySet()))
							.toList();

						var fqn = "I" + i++;
						var ref = new TypeReference<>(fqn);
						var intf = new InterfaceDecl(fqn, visibility, toEnumSet(modifiers, Modifier.class), weaveAnnotations(), SourceLocation.NO_LOCATION,
							implemented, Collections.emptyList(), weaveFields(ref), weaveMethods(), null);
						typeStore.put(fqn, intf);
						return weaveInnerTypes(intf);
					})
				)
			).forEach(intf -> {
				System.out.println(intf);
			});

		// Implemented interfaces
	}

	TypeReference<InterfaceDecl> newInterface(String fqn, AccessModifier visibility, Set<Modifier> modifiers) {
		var ref = new TypeReference<>(fqn);
		var intf = new InterfaceDecl(fqn, visibility, toEnumSet(modifiers, Modifier.class),
			weaveAnnotations(), SourceLocation.NO_LOCATION, Collections.emptyList(),
			Collections.emptyList(), weaveFields(ref), weaveMethods(), null);
		typeStore.put(fqn, intf);
		return new TypeReference<>(fqn);
	}

	private TypeDecl weaveInnerTypes(TypeDecl type) {
		return type;
	}

	private List<ConstructorDecl> weaveConstructors() {
		return Collections.emptyList();
	}

	private List<MethodDecl> weaveMethods() {
		return Collections.emptyList();
	}

	private List<Annotation> weaveAnnotations() {
		return Collections.emptyList();
	}

	private List<FieldDecl> weaveFields(TypeReference<TypeDecl> containing) {
		return nestedVisibilities.stream()
			.flatMap(visibility -> fieldModifiers.stream()
				.flatMap(modifiers -> fieldTypes.stream()
					.map(type -> {
						System.out.println("%s.%s%s%s".formatted(containing.getQualifiedName(), visibility, modifiers.stream().map(Modifier::toString).collect(Collectors.joining("")), type));
						return new FieldDecl("%s.%s%s%s".formatted(containing.getQualifiedName(), visibility, modifiers.stream().map(Modifier::toString).collect(Collectors.joining("")), type),
							visibility, toEnumSet(modifiers, Modifier.class),
							Collections.emptyList(), SourceLocation.NO_LOCATION, containing, type);
					})
				)
			)
			.toList();
	}

	private <T extends Enum<T>> EnumSet<T> toEnumSet(Set<T> set, Class<T> cls) {
		return set.isEmpty()
			? EnumSet.noneOf(cls)
			: EnumSet.copyOf(set);
	}

	public void generateCode(API api) {
		dst.toFile().mkdirs();
		var prettyPrinter = new APIPrettyPrinter();

		api.getAllTypes().forEach(t -> {
			var file = dst.resolve(t.getQualifiedName().replace('.', '/') + ".java");

			try {
				var code = prettyPrinter.$(t).print();

				file.toFile().createNewFile();
				Files.writeString(file, code);
				System.out.println("Generated " + file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public API getAPI() {
		return new API(typeStore.values().stream().toList(), new SpoonAPIFactory());
	}

	public static void main(String[] args) {
		var comb = new Combinatorial();
		comb.createInterface();
		var api = comb.getAPI();
		System.out.println("api="+api);
		comb.generateCode(api);
	}
}
