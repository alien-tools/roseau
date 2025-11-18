package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.diff.rules.breaking.AnnotationNewMethodWithoutDefaultRule;
import io.github.alien.roseau.diff.rules.breaking.AnnotationMethodNoLongerDefaultRule;
import io.github.alien.roseau.diff.rules.breaking.AnnotationNoLongerRepeatableRule;
import io.github.alien.roseau.diff.rules.breaking.AnnotationTargetRemovedRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowAbstractRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowCheckedExceptionRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowFinalRule;
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
import io.github.alien.roseau.diff.rules.breaking.ClassNoLongerStaticRule;
import io.github.alien.roseau.diff.rules.breaking.ClassNowStaticRule;
import io.github.alien.roseau.diff.rules.breaking.TypeSupertypeRemovedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeFormalTypeParametersChangedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeKindChangedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeNewAbstractMethodRule;
import io.github.alien.roseau.diff.rules.breaking.TypeNowProtectedRule;
import io.github.alien.roseau.diff.rules.breaking.TypeRemovedRule;

import java.util.List;

public class DefaultRuleSet implements RuleSet {
	@Override
	public List<TypeRule> getTypeRules() {
		return List.of(
			new TypeKindChangedRule(),
			new TypeRemovedRule(),
			new TypeSupertypeRemovedRule(),
			new TypeFormalTypeParametersChangedRule(),
			new TypeNowProtectedRule()
		);
	}

	@Override
	public List<ClassRule> getClassRules() {
		return List.of(
			new ClassNowAbstractRule(),
			new ClassNowCheckedExceptionRule(),
			new ClassNowFinalRule(),
			new ClassNoLongerStaticRule(),
			new ClassNowStaticRule()
		);
	}

	@Override
	public List<InterfaceRule> getInterfaceRules() {
		return List.of();
	}

	@Override
	public List<EnumRule> getEnumRules() {
		return List.of();
	}

	@Override
	public List<RecordRule> getRecordRules() {
		return List.of();
	}

	@Override
	public List<AnnotationRule> getAnnotationRules() {
		return List.of(
			new AnnotationNoLongerRepeatableRule(),
			new AnnotationTargetRemovedRule()
		);
	}

	@Override
	public List<ExecutableRule> getExecutableRules() {
		return List.of(
			new ExecutableRemovedRule(),
			new ExecutableFormalTypeParametersChangedRule(),
			new ExecutableParameterGenericsChangedRule(),
			new ExecutableThrownExceptionsRule()
		);
	}

	@Override
	public List<MethodRule> getMethodRules() {
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
	public List<ConstructorRule> getConstructorRules() {
		return List.of(
			new ConstructorNowProtectedRule()
		);
	}

	@Override
	public List<AnnotationMethodRule> getAnnotationMethodRules() {
		return List.of(
			new AnnotationNewMethodWithoutDefaultRule(),
			new AnnotationMethodNoLongerDefaultRule()
		);
	}

	@Override
	public List<FieldRule> getFieldRules() {
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
