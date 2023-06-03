package com.github.maracas.roseau;

import com.github.maracas.roseau.changes.BreakingChange;
import com.github.maracas.roseau.changes.BreakingChangeKind;
import com.github.maracas.roseau.changes.ConstructorBreakingChange;
import com.github.maracas.roseau.changes.FieldBreakingChange;
import com.github.maracas.roseau.changes.MethodBreakingChange;
import com.github.maracas.roseau.changes.TypeBreakingChange;
import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.ConstructorDeclaration;
import com.github.maracas.roseau.model.FieldDeclaration;
import com.github.maracas.roseau.model.MethodDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;
import com.github.maracas.roseau.model.VisibilityKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class APIDiff {
	private final API v1;
	private final API v2;
	private final List<BreakingChange> breakingChanges = new ArrayList<>();
	public APIDiff(API v1, API v2) {
		this.v1 = Objects.requireNonNull(v1);
		this.v2 = Objects.requireNonNull(v2);
	}

	public void diffAPIs() {
		v1.typeDeclarations().forEach(typeV1 -> {
			Optional<TypeDeclaration> typeV2 = v2.typeDeclarations().stream()
				.filter(t -> t.qualifiedName().equals(typeV1.qualifiedName()))
				.findAny();

			if (typeV2.isPresent()) {
				diffTypes(typeV1, typeV2.get());
			} else {
				breakingChanges.add(new TypeBreakingChange(typeV1, BreakingChangeKind.TYPE_REMOVED));
			}
		});
	}

	private void diffTypes(TypeDeclaration left, TypeDeclaration right) {
		if (!left.isAbstract() && right.isAbstract())
			breakingChanges.add(new TypeBreakingChange(left, BreakingChangeKind.CLASS_NOW_ABSTRACT));
		if (!left.isFinal() && right.isFinal())
			breakingChanges.add(new TypeBreakingChange(left, BreakingChangeKind.CLASS_NOW_FINAL));
		if (left.kind() != right.kind())
			breakingChanges.add(new TypeBreakingChange(left, BreakingChangeKind.TYPE_KIND_CHANGED));
		if (!left.isCheckedException() && right.isCheckedException())
			breakingChanges.add(new TypeBreakingChange(left, BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION));

		left.constructors().forEach(leftCons -> {
			Optional<ConstructorDeclaration> rightCons = right.constructors().stream()
				.filter(m -> m.signature().equals(leftCons.signature()))
				.findFirst();

			if (rightCons.isPresent()) {
				diffConstructors(leftCons, rightCons.get());
			} else {
				breakingChanges.add(new ConstructorBreakingChange(leftCons, BreakingChangeKind.CONSTRUCTOR_REMOVED));
			}
		});

		left.methods().forEach(leftMethod -> {
			Optional<MethodDeclaration> rightMethod = right.methods().stream()
				.filter(m -> m.signature().equals(leftMethod.signature()))
				.findFirst();

			if (rightMethod.isPresent()) {
				diffMethods(leftMethod, rightMethod.get());
			} else {
				breakingChanges.add(new MethodBreakingChange(leftMethod, BreakingChangeKind.METHOD_REMOVED));
			}
		});

		left.fields().forEach(leftField -> {
			Optional<FieldDeclaration> rightField = right.fields().stream()
				.filter(f -> f.name().equals(leftField.name()))
				.findFirst();

			if (rightField.isPresent()) {
				diffFields(leftField, rightField.get());
			} else {
				breakingChanges.add(new FieldBreakingChange(leftField, BreakingChangeKind.FIELD_REMOVED));
			}
		});
	}

	private void diffConstructors(ConstructorDeclaration left, ConstructorDeclaration right) {
		if (left.visibility() == VisibilityKind.PUBLIC && right.visibility() == VisibilityKind.PROTECTED)
			breakingChanges.add(new ConstructorBreakingChange(left, BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED));
	}

	private void diffMethods(MethodDeclaration left, MethodDeclaration right) {
		if (left.visibility() == VisibilityKind.PUBLIC && right.visibility() == VisibilityKind.PROTECTED)
			breakingChanges.add(new MethodBreakingChange(left, BreakingChangeKind.METHOD_NOW_PROTECTED));
		if (!left.isAbstract() && right.isAbstract())
			breakingChanges.add(new MethodBreakingChange(left, BreakingChangeKind.METHOD_NOW_ABSTRACT));
		if (!left.isFinal() && right.isFinal())
			breakingChanges.add(new MethodBreakingChange(left, BreakingChangeKind.METHOD_NOW_FINAL));
		if (!left.isStatic() && right.isStatic())
			breakingChanges.add(new MethodBreakingChange(left, BreakingChangeKind.METHOD_NOW_STATIC));
	}

	private void diffFields(FieldDeclaration left, FieldDeclaration right) {
		if (left.visibility() == VisibilityKind.PUBLIC && right.visibility() == VisibilityKind.PROTECTED)
			breakingChanges.add(new FieldBreakingChange(left, BreakingChangeKind.FIELD_NOW_PROTECTED));
		if (!left.isFinal() && right.isFinal())
			breakingChanges.add(new FieldBreakingChange(left, BreakingChangeKind.FIELD_NOW_FINAL));
		if (!left.isStatic() && right.isStatic())
			breakingChanges.add(new FieldBreakingChange(left, BreakingChangeKind.FIELD_NOW_STATIC));
		if (left.isStatic() && !right.isStatic())
			breakingChanges.add(new FieldBreakingChange(left, BreakingChangeKind.FIELD_NO_LONGER_STATIC));
	}

	public List<BreakingChange> getBreakingChanges() {
		return breakingChanges;
	}
}
