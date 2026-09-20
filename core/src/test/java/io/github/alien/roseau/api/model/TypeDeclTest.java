package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiTestFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypeDeclTest {
	static final TypeReference<TypeDecl> T = new TypeReference<>("pkg.T");

	static ClassDecl classWith(Set<MethodDecl> methods, Set<FieldDecl> fields, Set<ConstructorDecl> constructors,
	                           Set<Modifier> modifiers, Set<TypeReference<TypeDecl>> permittedTypes) {
		return new ClassDecl("pkg.T", AccessModifier.PUBLIC, modifiers, Set.of(), SourceLocation.NO_LOCATION,
			Set.of(), List.of(), fields, methods, null, null, constructors, permittedTypes);
	}

	@Test
	void class_without_explicit_superclass_extends_object() {
		var c = ApiTestFactory.newClass("pkg.T", AccessModifier.PUBLIC);

		assertThat(c.getSuperClass()).isEqualTo(TypeReference.OBJECT);
	}

	@Test
	void interfaces_are_implicitly_abstract() {
		var i = ApiTestFactory.newInterface("pkg.I", AccessModifier.PUBLIC);

		assertThat(i.isAbstract()).isTrue();
		assertThat(i.getModifiers()).contains(Modifier.ABSTRACT);
		assertThat(i.isInterface()).isTrue();
	}

	@Test
	void records_are_implicitly_final_and_extend_record() {
		var r = ApiTestFactory.newRecord("pkg.R", AccessModifier.PUBLIC);

		assertThat(r.isFinal()).isTrue();
		assertThat(r.getSuperClass()).isEqualTo(TypeReference.RECORD);
		assertThat(r.isRecord()).isTrue();
		assertThat(r.isClass()).isTrue();
	}

	@Test
	void enums_extend_enum() {
		var e = ApiTestFactory.newEnum("pkg.E", AccessModifier.PUBLIC);

		assertThat(e.getSuperClass()).isEqualTo(TypeReference.ENUM);
		assertThat(e.isEnum()).isTrue();
		assertThat(e.isClass()).isTrue();
	}

	@Test
	void permitted_types_imply_sealed() {
		var sealed = classWith(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(new TypeReference<>("pkg.Sub")));
		var plain = classWith(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

		assertThat(sealed.isSealed()).isTrue();
		assertThat(sealed.getModifiers()).contains(Modifier.SEALED);
		assertThat(plain.isSealed()).isFalse();
	}

	@Test
	void sealed_without_permitted_types_stays_sealed() {
		var sealed = classWith(Set.of(), Set.of(), Set.of(), Set.of(Modifier.SEALED), Set.of());

		assertThat(sealed.isSealed()).isTrue();
		assertThat(sealed.getPermittedTypes()).isEmpty();
	}

	@Test
	void class_is_effectively_abstract_when_abstract_or_without_constructors() {
		var withConstructor = classWith(Set.of(), Set.of(),
			Set.of(ApiTestFactory.newConstructor("pkg.T.<init>", T, List.of())), Set.of(), Set.of());
		var withoutConstructor = classWith(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
		var abstractClass = classWith(Set.of(), Set.of(),
			Set.of(ApiTestFactory.newConstructor("pkg.T.<init>", T, List.of())), Set.of(Modifier.ABSTRACT), Set.of());

		assertThat(withConstructor.isEffectivelyAbstract()).isFalse();
		assertThat(withoutConstructor.isEffectivelyAbstract()).isTrue();
		assertThat(abstractClass.isEffectivelyAbstract()).isTrue();
	}

	@Test
	void top_level_types_cannot_be_private_or_protected() {
		assertThatThrownBy(() -> ApiTestFactory.newClass("pkg.T", AccessModifier.PRIVATE))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ApiTestFactory.newClass("pkg.T", AccessModifier.PROTECTED))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void nested_types_can_have_any_visibility() {
		var nested = new ClassDecl("pkg.T$Inner", AccessModifier.PRIVATE, Set.of(), Set.of(),
			SourceLocation.NO_LOCATION, Set.of(), List.of(), Set.of(), Set.of(), T, null, Set.of(), Set.of());

		assertThat(nested.isNested()).isTrue();
		assertThat(nested.getEnclosingType()).hasValue(T);
		assertThat(nested.isPrivate()).isTrue();
	}

	@Test
	void top_level_types_have_no_enclosing_type() {
		var c = ApiTestFactory.newClass("pkg.T", AccessModifier.PUBLIC);

		assertThat(c.isNested()).isFalse();
		assertThat(c.getEnclosingType()).isEmpty();
	}

	@Test
	void package_name_of_nested_and_default_package_types() {
		assertThat(ApiTestFactory.newClass("pkg.sub.T", AccessModifier.PUBLIC).getPackageName()).isEqualTo("pkg.sub");
		assertThat(ApiTestFactory.newClass("pkg.T$Inner", AccessModifier.PUBLIC).getPackageName()).isEqualTo("pkg");
		assertThat(ApiTestFactory.newClass("T", AccessModifier.PUBLIC).getPackageName()).isEmpty();
	}
}
