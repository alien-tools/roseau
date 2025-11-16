package io.github.alien.roseau.diff.rules;

import java.util.List;

public class DefaultRuleSet implements RuleSet {
	@Override
	public List<TypeRule> getTypeRules() {
		return List.of(
			new TypeKindChangedRule(),
			new TypeRemovedRule(),
			new SupertypeRemovedRule(),
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
			new NestedClassNoLongerStaticRule(),
			new NestedClassNowStaticRule()
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
			new MethodAbstractAddedRule(),
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
