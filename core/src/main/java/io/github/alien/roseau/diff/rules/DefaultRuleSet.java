package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.rules.breaking.AnnotationMethodNoLongerDefault;
import io.github.alien.roseau.diff.rules.breaking.AnnotationNewMethodWithoutDefault;
import io.github.alien.roseau.diff.rules.breaking.AnnotationNoLongerRepeatable;
import io.github.alien.roseau.diff.rules.breaking.AnnotationTargetRemoved;
import io.github.alien.roseau.diff.rules.breaking.ClassNoLongerStatic;
import io.github.alien.roseau.diff.rules.breaking.ClassNowAbstract;
import io.github.alien.roseau.diff.rules.breaking.ClassNowCheckedException;
import io.github.alien.roseau.diff.rules.breaking.ClassNowFinal;
import io.github.alien.roseau.diff.rules.breaking.ClassNowStatic;
import io.github.alien.roseau.diff.rules.breaking.ConstructorNowProtected;
import io.github.alien.roseau.diff.rules.breaking.ExecutableFormalTypeParametersChanged;
import io.github.alien.roseau.diff.rules.breaking.ExecutableParameterGenericsChanged;
import io.github.alien.roseau.diff.rules.breaking.ExecutableRemoved;
import io.github.alien.roseau.diff.rules.breaking.ExecutableThrownExceptions;
import io.github.alien.roseau.diff.rules.breaking.FieldNoLongerStatic;
import io.github.alien.roseau.diff.rules.breaking.FieldNowFinal;
import io.github.alien.roseau.diff.rules.breaking.FieldNowProtected;
import io.github.alien.roseau.diff.rules.breaking.FieldNowStatic;
import io.github.alien.roseau.diff.rules.breaking.FieldRemoved;
import io.github.alien.roseau.diff.rules.breaking.FieldTypeChanged;
import io.github.alien.roseau.diff.rules.breaking.MethodNoLongerStatic;
import io.github.alien.roseau.diff.rules.breaking.MethodNowAbstract;
import io.github.alien.roseau.diff.rules.breaking.MethodNowFinal;
import io.github.alien.roseau.diff.rules.breaking.MethodNowProtected;
import io.github.alien.roseau.diff.rules.breaking.MethodNowStatic;
import io.github.alien.roseau.diff.rules.breaking.MethodReturnTypeChanged;
import io.github.alien.roseau.diff.rules.breaking.TypeFormalTypeParametersChanged;
import io.github.alien.roseau.diff.rules.breaking.TypeKindChanged;
import io.github.alien.roseau.diff.rules.breaking.TypeNewAbstractMethod;
import io.github.alien.roseau.diff.rules.breaking.TypeNowProtected;
import io.github.alien.roseau.diff.rules.breaking.TypeRemoved;
import io.github.alien.roseau.diff.rules.breaking.TypeSupertypeRemoved;

import java.util.List;

public class DefaultRuleSet implements RuleSet {
	@Override
	public List<TypeRule<TypeDecl>> getTypeRules() {
		return List.of(
			new TypeKindChanged(),
			new TypeRemoved(),
			new TypeSupertypeRemoved(),
			new TypeFormalTypeParametersChanged(),
			new TypeNowProtected()
		);
	}

	@Override
	public List<TypeRule<ClassDecl>> getClassRules() {
		return List.of(
			new ClassNowAbstract(),
			new ClassNowCheckedException(),
			new ClassNowFinal(),
			new ClassNoLongerStatic(),
			new ClassNowStatic()
		);
	}

	@Override
	public List<TypeRule<InterfaceDecl>> getInterfaceRules() {
		return List.of();
	}

	@Override
	public List<TypeRule<EnumDecl>> getEnumRules() {
		return List.of();
	}

	@Override
	public List<TypeRule<RecordDecl>> getRecordRules() {
		return List.of();
	}

	@Override
	public List<TypeRule<AnnotationDecl>> getAnnotationRules() {
		return List.of(
			new AnnotationNoLongerRepeatable(),
			new AnnotationTargetRemoved()
		);
	}

	@Override
	public List<MemberRule<ExecutableDecl>> getExecutableRules() {
		return List.of(
			new ExecutableRemoved(),
			new ExecutableFormalTypeParametersChanged(),
			new ExecutableParameterGenericsChanged(),
			new ExecutableThrownExceptions()
		);
	}

	@Override
	public List<MemberRule<MethodDecl>> getMethodRules() {
		return List.of(
			new TypeNewAbstractMethod(),
			new MethodNoLongerStatic(),
			new MethodNowAbstract(),
			new MethodNowFinal(),
			new MethodNowProtected(),
			new MethodNowStatic(),
			new MethodReturnTypeChanged()
		);
	}

	@Override
	public List<MemberRule<ConstructorDecl>> getConstructorRules() {
		return List.of(
			new ConstructorNowProtected()
		);
	}

	@Override
	public List<MemberRule<AnnotationMethodDecl>> getAnnotationMethodRules() {
		return List.of(
			new AnnotationNewMethodWithoutDefault(),
			new AnnotationMethodNoLongerDefault()
		);
	}

	@Override
	public List<MemberRule<FieldDecl>> getFieldRules() {
		return List.of(
			new FieldRemoved(),
			new FieldNoLongerStatic(),
			new FieldNowFinal(),
			new FieldNowProtected(),
			new FieldNowStatic(),
			new FieldTypeChanged()
		);
	}
}
