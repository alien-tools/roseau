package com.github.maracas.roseau;

import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Combinatorial {
	private static final String packageName = "com.example.testapi";
	private static final Path outputPath = Path.of("generated");
	private static final Set<Modifier> visibilities = Set.of(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.PUBLIC);
	private static final Set<Modifier> fieldModifiers = Set.of(Modifier.STATIC, Modifier.FINAL); // TRANSIENT, VOLATILE
	private static final Set<Modifier> methodModifiers = Set.of(Modifier.STATIC, Modifier.FINAL, Modifier.ABSTRACT, Modifier.NATIVE, Modifier.DEFAULT, Modifier.SYNCHRONIZED);
	private static final Set<Modifier> classModifiers = Set.of(Modifier.STATIC, Modifier.FINAL, Modifier.ABSTRACT, Modifier.SEALED, Modifier.NON_SEALED);
	private static final Set<Modifier> recordModifiers = Set.of(Modifier.STATIC, Modifier.FINAL);
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

	void generateAllTypes() {
		classes(null);
		records(null);
	}

	void classes(TypeSpec.Builder parent) {
		int i = 0;
		for (var vis : visibilities) {
			for (var mods : Sets.powerSet(classModifiers)) {
				var clsName = "C" + i++;

				if (parent == null && (vis == Modifier.PRIVATE || vis == Modifier.PROTECTED || mods.contains(Modifier.STATIC)))
					continue;
				if (mods.contains(Modifier.SEALED) && mods.contains(Modifier.NON_SEALED))
					continue;
				if (mods.contains(Modifier.FINAL) && (mods.contains(Modifier.ABSTRACT) || mods.contains(Modifier.SEALED) || mods.contains(Modifier.NON_SEALED)))
					continue;

				var cls = TypeSpec.classBuilder(clsName)
					.addModifiers(vis)
					.addModifiers(mods.toArray(new Modifier[0]));

				if (mods.contains(Modifier.SEALED)) {
					insertSubclassesSealed(cls);
				}

				if (mods.contains(Modifier.NON_SEALED)) {
					insertSuperclassSealed(cls);
				}

				storeType(clsName, cls);
			}
		}
	}

	void records(TypeSpec.Builder parent) {
		int i = 0;
		for (var vis : visibilities) {
			for (var mods : Sets.powerSet(recordModifiers)) {
				var clsName = "R" + i++;

				if (parent == null && (vis == Modifier.PRIVATE || vis == Modifier.PROTECTED || mods.contains(Modifier.STATIC)))
					continue;

				var cls = TypeSpec.recordBuilder(clsName)
					.addModifiers(vis)
					.addModifiers(mods.toArray(new Modifier[0]));

				storeType(clsName, cls);
			}
		}
	}

	void weaveAllFields(TypeSpec.Builder typeBuilder) {
		int i = 0;
		for (var vis : visibilities) {
			for (var mods : Sets.powerSet(fieldModifiers)) {
				for (var t : fieldTypes) {
					var fName = "f" + i++;
					var f = FieldSpec.builder(t, fName)
						.addModifiers(vis)
						.addModifiers(mods.toArray(new Modifier[0]));

					if (mods.contains(Modifier.FINAL))
						f.initializer("$L", getDefaultValue(t));

					typeBuilder.addField(f.build());
				}
			}
		}
	}

	void weaveAllMethods(TypeSpec.Builder typeBuilder) {
		int i = 0;
		for (var vis : visibilities) {
			for (var mods : Sets.powerSet(methodModifiers)) {
				for (var t : methodTypes) {
					var mName = "m" + i++;

					if (mods.contains(Modifier.DEFAULT) && typeBuilder.build().kind != TypeSpec.Kind.INTERFACE)
						continue;
					if (mods.contains(Modifier.ABSTRACT) && (
						vis == Modifier.PRIVATE || mods.contains(Modifier.FINAL) || mods.contains(Modifier.NATIVE) ||
						mods.contains(Modifier.STATIC) || mods.contains(Modifier.SYNCHRONIZED)))
						continue;
					if (typeBuilder.build().kind == TypeSpec.Kind.RECORD && mods.contains(Modifier.NATIVE))
						continue;

					var m = MethodSpec.methodBuilder(mName)
						.addModifiers(vis)
						.addModifiers(mods)
						.returns(t);

					if (!mods.contains(Modifier.ABSTRACT) && !mods.contains(Modifier.NATIVE) && !t.equals(TypeName.VOID))
						m.addCode("return $L;", getDefaultValue(t));

					var shouldOverride = false;
					var superM = superMethod(typeBuilder, m);
					if (superM != null) {
						if (superM.hasModifier(Modifier.FINAL))
							continue;
						if (superM.hasModifier(Modifier.PUBLIC) && mods.contains(Modifier.PROTECTED))
							continue;
						if (superM.hasModifier(Modifier.ABSTRACT) && !typeBuilder.modifiers.contains(Modifier.ABSTRACT))
							shouldOverride = true;
					}

					if (shouldOverride) {
						m.modifiers.remove(Modifier.ABSTRACT);
						if (!t.equals(TypeName.VOID))
							m.addCode("return $L;", getDefaultValue(t));
					} else if (mods.contains(Modifier.ABSTRACT) && !typeBuilder.modifiers.contains(Modifier.ABSTRACT)) {
						continue;
					}

					typeBuilder.addMethod(m.build());
					typeBuilder.build();
				}
			}
		}
	}

	void insertSubclassesSealed(TypeSpec.Builder cls) {
		// FIXME
		var supName = cls.build().name;
		var subName = "Sub" + supName;
		var sub = TypeSpec.classBuilder(subName)
			.addModifiers(Modifier.FINAL)
			.superclass(ClassName.get(packageName, supName));

		cls.addPermits(ClassName.get(packageName, subName));

		storeType(subName, sub);
	}

	void insertSuperclassSealed(TypeSpec.Builder cls) {
		var subName = cls.build().name;
		var supName = subName + "Sup";

		// FIXME
		var sup = TypeSpec.classBuilder(supName)
			.addModifiers(Modifier.SEALED)
			.addPermits(ClassName.get(packageName, subName));

		cls.superclass(ClassName.get(packageName, supName));

		storeType(supName, sup);
	}

	MethodSpec superMethod(TypeSpec.Builder t, MethodSpec.Builder m) {
		var sup = types.get(t.build().superclass.toString());
		if (sup != null) {
			var opt = sup.methodSpecs.stream().filter(superM -> superM.name.equals(m.build().name)).findFirst();
			if (opt.isPresent())
				return opt.get();
		}
		return null;
	}

	String getDefaultValue(TypeName type) {
		if (type.equals(TypeName.BOOLEAN)) return "false";
		if (type.equals(TypeName.BYTE) || type.equals(TypeName.SHORT) || type.equals(TypeName.INT) || type.equals(TypeName.LONG)) return "0";
		if (type.equals(TypeName.FLOAT)) return "0.0f";
		if (type.equals(TypeName.DOUBLE)) return "0.0d";
		if (type.equals(TypeName.CHAR)) return "'\\u0000'";
		return "null";
	}

	boolean isAbstract(TypeSpec.Builder typeBuilder) {
		return typeBuilder.modifiers.contains(Modifier.ABSTRACT);
	}

	TypeSpec.Builder findType(String name) {
		if (name == null)
			return null;

		return types.get(name);
	}

	void storeType(String name, TypeSpec.Builder typeBuilder) {
		types.put(packageName + "." + name, typeBuilder);
	}

	int typeHierarchy(TypeSpec.Builder t1, TypeSpec.Builder t2) {
		if (t1.build().superclass.toString().equals(packageName + "." + t2.build().name))
			return 1;
		if (t2.build().superclass.toString().equals(packageName + "." + t1.build().name))
			return -1;
		return t1.build().name.toString().compareTo(t2.build().name.toString());
	}
}
