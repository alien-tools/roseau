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
import io.github.alien.roseau.diff.rules.breaking.AnnotationMethodNoLongerDefaultRule;
import io.github.alien.roseau.diff.rules.breaking.AnnotationNewMethodWithoutDefaultRule;
import io.github.alien.roseau.diff.rules.breaking.AnnotationNoLongerRepeatableRule;
import io.github.alien.roseau.diff.rules.breaking.AnnotationTargetRemovedRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNoLongerStaticRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowAbstractRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowCheckedExceptionRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowFinalRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowStaticRule;
import io.github.alien.roseau.diff.rules.breaking.ConstructorNowProtectedRule;
import io.github.alien.roseau.diff.rules.breaking.ExecutableFormalTypeParametersChangedRule;
import io.github.alien.roseau.diff.rules.breaking.ExecutableParameterGenericsChangedRule;
import io.github.alien.roseau.diff.rules.breaking.ExecutableRemovedRule;
import io.github.alien.roseau.diff.rules.breaking.ExecutableThrownExceptionsRule;
import io.github.alien.roseau.diff.rules.breaking.FieldNoLongerStaticRule;
import io.github.alien.roseau.diff.rules.breaking.FieldNowFinalRule;
import io.github.alien.roseau.diff.rules.breaking.FieldNowProtectedRule;
import io.github.alien.roseau.diff.rules.breaking.FieldNowStaticRule;
import io.github.alien.roseau.diff.rules.breaking.FieldRemovedRule;
import io.github.alien.roseau.diff.rules.breaking.FieldTypeChangedRule;
import io.github.alien.roseau.diff.rules.breaking.MethodNoLongerStaticRule;
import io.github.alien.roseau.diff.rules.breaking.MethodNowAbstractRule;
import io.github.alien.roseau.diff.rules.breaking.MethodNowFinalRule;
import io.github.alien.roseau.diff.rules.breaking.MethodNowProtectedRule;
import io.github.alien.roseau.diff.rules.breaking.MethodNowStaticRule;
import io.github.alien.roseau.diff.rules.breaking.MethodReturnTypeChangedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeFormalTypeParametersChangedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeKindChangedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeNewAbstractMethodRule;
import io.github.alien.roseau.diff.rules.breaking.TypeNowProtectedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeRemovedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeSupertypeRemovedRule;

import java.util.List;

public class DefaultRuleSet implements RuleSet {
	@Override
	public List<Rule<TypeDecl>> getTypeRules() {
		return List.of(
			new TypeKindChangedRule(),
			new TypeRemovedRule(),
			new TypeSupertypeRemovedRule(),
			new TypeFormalTypeParametersChangedRule(),
			new TypeNowProtectedRule()
		);
	}

	@Override
	public List<Rule<ClassDecl>> getClassRules() {
		return List.of(
			new ClassNowAbstractRule(),
			new ClassNowCheckedExceptionRule(),
			new ClassNowFinalRule(),
			new ClassNoLongerStaticRule(),
			new ClassNowStaticRule()
		);
	}

	@Override
	public List<Rule<InterfaceDecl>> getInterfaceRules() {
		return List.of();
	}

	@Override
	public List<Rule<EnumDecl>> getEnumRules() {
		return List.of();
	}

	@Override
	public List<Rule<RecordDecl>> getRecordRules() {
		return List.of();
	}

	@Override
	public List<Rule<AnnotationDecl>> getAnnotationRules() {
		return List.of(
			new AnnotationNoLongerRepeatableRule(),
			new AnnotationTargetRemovedRule()
		);
	}

	@Override
	public List<MemberRule<ExecutableDecl>> getExecutableRules() {
		return List.of(
			new ExecutableRemovedRule(),
			new ExecutableFormalTypeParametersChangedRule(),
			new ExecutableParameterGenericsChangedRule(),
			new ExecutableThrownExceptionsRule()
		);
	}

	@Override
	public List<MemberRule<MethodDecl>> getMethodRules() {
		return List.of(
			new TypeNewAbstractMethodRule(),
			new MethodNoLongerStaticRule(),
			new MethodNowAbstractRule(),
			new MethodNowFinalRule(),
			new MethodNowProtectedRule(),
			new MethodNowStaticRule(),
			new MethodReturnTypeChangedRule()
		);
	}

	@Override
	public List<MemberRule<ConstructorDecl>> getConstructorRules() {
		return List.of(
			new ConstructorNowProtectedRule()
		);
	}

	@Override
	public List<MemberRule<AnnotationMethodDecl>> getAnnotationMethodRules() {
		return List.of(
			new AnnotationNewMethodWithoutDefaultRule(),
			new AnnotationMethodNoLongerDefaultRule()
		);
	}

	@Override
	public List<MemberRule<FieldDecl>> getFieldRules() {
		return List.of(
			new FieldRemovedRule(),
			new FieldNoLongerStaticRule(),
			new FieldNowFinalRule(),
			new FieldNowProtectedRule(),
			new FieldNowStaticRule(),
			new FieldTypeChangedRule()
		);
	}
}
