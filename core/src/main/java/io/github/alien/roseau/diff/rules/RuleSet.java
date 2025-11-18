package io.github.alien.roseau.diff.rules;

import java.util.List;

public interface RuleSet {
	List<TypeRule> getTypeRules();
	List<ClassRule> getClassRules();
	List<InterfaceRule> getInterfaceRules();
	List<EnumRule> getEnumRules();
	List<RecordRule> getRecordRules();
	List<AnnotationRule> getAnnotationRules();

	List<ExecutableRule> getExecutableRules();
	List<MethodRule> getMethodRules();
	List<ConstructorRule> getConstructorRules();
	List<AnnotationMethodRule> getAnnotationMethodRules();

	List<FieldRule> getFieldRules();
}
