package com.github.maracas.roseau;

import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Combinatorial {
	enum TypeKind {
		CLASS, INTERFACE, RECORD, ENUM /* ANNOTATION */
	}

	enum Visibility {
		PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE;

		static List<Visibility> topLevelVisibilities = List.of(PUBLIC, PACKAGE_PRIVATE);
		static List<Visibility> nestedVisibilities   = List.of(PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE);

		Set<javax.lang.model.element.Modifier> toJavaxModifier() {
			return switch (this) {
				case PUBLIC ->          Set.of(javax.lang.model.element.Modifier.PUBLIC);
				case PROTECTED ->       Set.of(javax.lang.model.element.Modifier.PROTECTED);
				case PACKAGE_PRIVATE -> Set.of();
				case PRIVATE ->         Set.of(javax.lang.model.element.Modifier.PRIVATE);
			};
		}
	}

	enum Modifier {
		ABSTRACT, DEFAULT, FINAL, NATIVE, NON_SEALED, SEALED, STATIC, SYNCHRONIZED; /* TRANSIENT, VOLATILE */

		static Set<Set<Modifier>> fieldModifiers  = Sets.powerSet(Set.of(STATIC, FINAL));
		static Set<Set<Modifier>> methodModifiers = Sets.powerSet(Set.of(STATIC, FINAL, ABSTRACT, NATIVE, DEFAULT, SYNCHRONIZED))
			.stream()
			.filter(mods -> !mods.containsAll(Set.of(FINAL, ABSTRACT)))
			.filter(mods -> !mods.contains(ABSTRACT) || Sets.intersection(mods, Set.of(NATIVE, STATIC, SYNCHRONIZED)).isEmpty())
			.filter(mods -> !mods.contains(DEFAULT) || Sets.intersection(mods, Set.of(STATIC, ABSTRACT)).isEmpty())
			.collect(Collectors.toSet());
		static Set<Set<Modifier>> classModifiers  = Sets.powerSet(Set.of(STATIC, FINAL, ABSTRACT, SEALED, NON_SEALED))
			.stream()
			.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
			.filter(mods -> !mods.containsAll(Set.of(ABSTRACT, FINAL)))
			.filter(mods -> !mods.contains(FINAL) || Sets.intersection(mods, Set.of(ABSTRACT, SEALED, NON_SEALED)).isEmpty())
			.collect(Collectors.toSet());
		static Set<Set<Modifier>> interfaceModifiers = Sets.powerSet(Set.of(STATIC, ABSTRACT, SEALED, NON_SEALED))
			.stream()
			.filter(mods -> !mods.containsAll(Set.of(SEALED, NON_SEALED)))
			.collect(Collectors.toSet());
		static Set<Set<Modifier>> recordModifiers = Sets.powerSet(Set.of(STATIC, FINAL));
		static Set<Set<Modifier>> enumModifiers   = Sets.powerSet(Set.of(STATIC));

		javax.lang.model.element.Modifier toJavaxModifier() {
			return javax.lang.model.element.Modifier.valueOf(name());
		}
	}

	private static final String packageName = "com.example.testapi";
	private static final Path outputPath = Path.of("generated");
	private static final Set<TypeName> fieldTypes = Set.of(
		TypeName.INT,
		TypeName.INT.box(),
		ClassName.get(String.class),
		ParameterizedTypeName.get(List.class, String.class)
	);
	private static final Set<TypeName> methodTypes = Sets.union(fieldTypes, Set.of(TypeName.VOID));

	private Map<String, TypeSpec.Builder> types = new HashMap<>();

	public static void main(String[] args) {
		new Combinatorial().run();
	}

	void run() {
		generateAllTypes();

		types.values().stream().sorted(this::typeHierarchy).forEach(type -> {
			System.out.println(type.build().name);
			weaveAllFields(type);
			weaveAllMethods(type);
			writeJavaFile(type);
		});
	}

	void writeJavaFile(TypeSpec.Builder type) {
		try {
			var javaFile = JavaFile.builder(packageName, type.build()).build();
			javaFile.writeTo(outputPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	javax.lang.model.element.Modifier[] toArray(Set<javax.lang.model.element.Modifier> mods) {
		return mods.toArray(new javax.lang.model.element.Modifier[0]);
	}

	Set<javax.lang.model.element.Modifier> toJavaxModifiers(Set<Modifier> mods) {
		return mods.stream().map(Modifier::toJavaxModifier).collect(Collectors.toSet());
	}

	void generateAllTypes() {
		for (var kind : TypeKind.values())
			type(kind, null);
	}

	void type(TypeKind kind, TypeSpec.Builder containing) {
		int i = 0;
		var visibilities = containing == null ? Visibility.topLevelVisibilities : Visibility.nestedVisibilities;
		var modifiers = switch (kind) {
			case CLASS     -> Modifier.classModifiers;
			case INTERFACE -> Modifier.interfaceModifiers;
			case RECORD    -> Modifier.recordModifiers;
			case ENUM      -> Modifier.enumModifiers;
		};
		for (var vis : visibilities) {
			for (var mods : modifiers) {
				if (containing == null && mods.contains(Modifier.STATIC))
					continue;

				var typeName = kind.name().substring(0, 1) + i++;

				var builder = switch (kind) {
					case CLASS     -> TypeSpec.classBuilder(typeName);
					case INTERFACE -> TypeSpec.interfaceBuilder(typeName);
					case RECORD    -> TypeSpec.recordBuilder(typeName);
					case ENUM      -> TypeSpec.enumBuilder(typeName);
				};

				builder
					.addModifiers(toArray(vis.toJavaxModifier()))
					.addModifiers(toArray(toJavaxModifiers(mods)));

				if (mods.contains(Modifier.SEALED)) {
					insertSubclassesSealed(builder);
				}

				if (mods.contains(Modifier.NON_SEALED)) {
					insertSupertypesSealed(builder);
				}

				storeType(typeName, builder);
			}
		}
	}

	void weaveAllFields(TypeSpec.Builder containing) {
		int i = 0;
		var kind = containing.build().kind;
		for (var vis : Visibility.nestedVisibilities) {
			for (var mods : Modifier.fieldModifiers) {
				for (var t : fieldTypes) {
					var fName = "f" + i++;

					if (kind == TypeSpec.Kind.INTERFACE) {
						if (vis != Visibility.PUBLIC)
							continue;
						if (!mods.containsAll(Set.of(Modifier.FINAL, Modifier.STATIC)))
							continue;
					}

					var f = FieldSpec.builder(t, fName)
						.addModifiers(toArray(vis.toJavaxModifier()))
						.addModifiers(toArray(toJavaxModifiers(mods)));

					if (mods.contains(Modifier.FINAL))
						f.initializer("$L", getDefaultValue(t));

					containing.addField(f.build());
				}
			}
		}
	}

	void weaveAllMethods(TypeSpec.Builder containing) {
		int i = 0;
		var kind = containing.build().kind;
		for (var vis : Visibility.nestedVisibilities) {
			for (var mods : Modifier.methodModifiers) {
				for (var t : methodTypes) {
					var mName = "m" + i++;

					if (mods.contains(Modifier.NATIVE) && kind != TypeSpec.Kind.CLASS)
						continue;
					if (mods.contains(Modifier.DEFAULT) && kind != TypeSpec.Kind.INTERFACE)
						continue;
					if (mods.contains(Modifier.ABSTRACT) && vis == Visibility.PRIVATE)
						continue;
					if (kind == TypeSpec.Kind.INTERFACE) {
						if (vis == Visibility.PROTECTED || vis == Visibility.PACKAGE_PRIVATE)
							continue;
						if (mods.contains(Modifier.FINAL) || mods.contains(Modifier.SYNCHRONIZED))
							continue;
						if (mods.contains(Modifier.DEFAULT) && vis != Visibility.PUBLIC)
							continue;
						if (vis == Visibility.PUBLIC && Sets.intersection(mods, Set.of(Modifier.ABSTRACT, Modifier.STATIC, Modifier.DEFAULT)).isEmpty())
							continue;
					}

					var m = MethodSpec.methodBuilder(mName)
						.addModifiers(toArray(vis.toJavaxModifier()))
						.addModifiers(toArray(toJavaxModifiers(mods)))
						.returns(t);

					if (!mods.contains(Modifier.ABSTRACT) && !mods.contains(Modifier.NATIVE) && !t.equals(TypeName.VOID))
						m.addCode("return $L;", getDefaultValue(t));

					var shouldImplement = false;
					var superM = superMethod(containing, m);
					if (superM != null) {
						if (superM.hasModifier(Modifier.FINAL.toJavaxModifier()) || superM.hasModifier(Modifier.STATIC.toJavaxModifier()))
							continue;
						if (superM.modifiers.containsAll(Visibility.PUBLIC.toJavaxModifier()) && vis != Visibility.PUBLIC)
							continue;
						if (superM.hasModifier(Modifier.ABSTRACT.toJavaxModifier()) && !isAbstract(containing))
							shouldImplement = true;
					}

					if (shouldImplement) {
						m.modifiers.remove(Modifier.ABSTRACT.toJavaxModifier());
						if (!t.equals(TypeName.VOID))
							m.addCode("return $L;", getDefaultValue(t));
					} else if (mods.contains(Modifier.ABSTRACT) && !containing.modifiers.contains(Modifier.ABSTRACT.toJavaxModifier())) {
						continue;
					}

					containing.addMethod(m.build());
					containing.build();
				}
			}
		}
	}

	void insertSubclassesSealed(TypeSpec.Builder cls) {
		// FIXME
		var supName = cls.build().name;
		var subName = supName + "Sub";
		if (cls.build().kind == TypeSpec.Kind.CLASS) {
			var sub = TypeSpec.classBuilder(subName)
				.addModifiers(Modifier.FINAL.toJavaxModifier())
				.superclass(ClassName.get(packageName, supName));
			cls.addPermits(ClassName.get(packageName, subName));
			storeType(subName, sub);
		} else {
			var sub = TypeSpec.classBuilder(subName)
				.addModifiers(Modifier.FINAL.toJavaxModifier())
				.addSuperinterface(ClassName.get(packageName, supName));
			cls.addPermits(ClassName.get(packageName, subName));
			storeType(subName, sub);
		}
	}

	void insertSupertypesSealed(TypeSpec.Builder cls) {
		if (cls.build().kind == TypeSpec.Kind.CLASS)
			insertSuperclassSealed(cls);

		insertSupertypeSealed(cls);
	}

	void insertSuperclassSealed(TypeSpec.Builder cls) {
		var subName = cls.build().name;
		var supName = subName + "Sup";

		// FIXME
		var sup = TypeSpec.classBuilder(supName)
			.addModifiers(Modifier.SEALED.toJavaxModifier())
			.addPermits(ClassName.get(packageName, subName));

		cls.superclass(ClassName.get(packageName, supName));

		storeType(supName, sup);
	}

	void insertSupertypeSealed(TypeSpec.Builder cls) {
		var subName = cls.build().name;
		var supName = subName + "SupI";

		// FIXME
		var sup = TypeSpec.interfaceBuilder(supName)
			.addModifiers(Modifier.SEALED.toJavaxModifier())
			.addPermits(ClassName.get(packageName, subName));

		cls.addSuperinterface(ClassName.get(packageName, supName));

		storeType(supName, sup);
	}

	MethodSpec superMethod(TypeSpec.Builder t, MethodSpec.Builder m) {
		var supers = Stream.concat(
			Stream.of(t.build().superclass.toString()),
			t.build().superinterfaces.stream().map(intf -> intf.toString())
		);

		for (var s : supers.toList()) {
			var superType = types.get(s);
			if (superType != null) {
				var opt = superType.methodSpecs.stream().filter(superM -> superM.name.equals(m.build().name)).findFirst();
				if (opt.isPresent())
					return opt.get();
			}
		}

		return null;
	}

	boolean isAbstract(TypeSpec.Builder type) {
		return type.build().kind == TypeSpec.Kind.INTERFACE || type.modifiers.contains(Modifier.ABSTRACT.toJavaxModifier());
	}

	String getDefaultValue(TypeName type) {
		if (type.equals(TypeName.BOOLEAN)) return "false";
		if (type.equals(TypeName.BYTE) || type.equals(TypeName.SHORT) || type.equals(TypeName.INT) || type.equals(TypeName.LONG)) return "0";
		if (type.equals(TypeName.FLOAT)) return "0.0f";
		if (type.equals(TypeName.DOUBLE)) return "0.0d";
		if (type.equals(TypeName.CHAR)) return "'\\u0000'";
		return "null";
	}

	void storeType(String name, TypeSpec.Builder typeBuilder) {
		types.put(packageName + "." + name, typeBuilder);
	}

	// FIXME: too lazy for topological sort
	int typeHierarchy(TypeSpec.Builder t1, TypeSpec.Builder t2) {
		var t1name = t1.build().name;
		var t2name = t2.build().name;

		if (t1name.contains("Sup") && !t2name.contains("Sup"))
			return -1;
		if (t2name.contains("Sup") && !t1name.contains("Sup"))
			return 1;
		if (t1name.contains("Sub") && !t2name.contains("Sub"))
			return 1;
		if (t2name.contains("Sub") && !t1name.contains("Sub"))
			return -1;

		return t1name.compareTo(t2name);
	}
}
