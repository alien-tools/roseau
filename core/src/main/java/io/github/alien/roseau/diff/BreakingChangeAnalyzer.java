package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.rules.DefaultRuleSet;
import io.github.alien.roseau.diff.rules.MemberRuleContext;
import io.github.alien.roseau.diff.rules.RuleSet;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

import java.util.Optional;
import java.util.Set;

/**
 * Computes the list of breaking changes between two {@link API} instances.
 * <br>
 * The compared APIs are visited deeply to match their symbols pairwise based on their unique name and compare their
 * properties when their names match. This implementation visits all {@link TypeDecl} instances in parallel.
 */
public class BreakingChangeAnalyzer implements ApiDiffer<RoseauReport> {
	private final API v1;
	private final API v2;
	private final RuleSet ruleSet;
	private final RoseauReport.Builder builder;

	public BreakingChangeAnalyzer(API v1, API v2, RuleSet ruleSet) {
		this.v1 = Preconditions.checkNotNull(v1);
		this.v2 = Preconditions.checkNotNull(v2);
		this.ruleSet = ruleSet;
		this.builder = RoseauReport.builder(v1, v2);
	}

	public BreakingChangeAnalyzer(API v1, API v2) {
		this(v1, v2, new DefaultRuleSet());
	}

	@Override
	public RoseauReport get() {
		return builder.build();
	}

	@Override
	public void onMatchedType(TypeDecl oldType, TypeDecl newType) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getTypeRules().forEach(rule -> rule.onMatchedType(oldType, newType, context));
	}

	@Override
	public void onRemovedType(TypeDecl type) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getTypeRules().forEach(rule -> rule.onRemovedType(type, context));
	}

	@Override
	public void onAddedType(TypeDecl type) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getTypeRules().forEach(rule -> rule.onAddedType(type, context));
	}

	@Override
	public void onMatchedClass(ClassDecl oldCls, ClassDecl newCls) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getClassRules().forEach(rule -> rule.onMatchedClass(oldCls, newCls, context));
	}

	@Override
	public void onRemovedClass(ClassDecl cls) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getClassRules().forEach(rule -> rule.onRemovedClass(cls, context));
	}

	@Override
	public void onAddedClass(ClassDecl cls) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getClassRules().forEach(rule -> rule.onAddedClass(cls, context));
	}

	@Override
	public void onMatchedEnum(EnumDecl oldEnum, EnumDecl newEnum) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getEnumRules().forEach(rule -> rule.onMatchedEnum(oldEnum, newEnum, context));
	}

	@Override
	public void onRemovedEnum(EnumDecl enm) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getEnumRules().forEach(rule -> rule.onRemovedEnum(enm, context));
	}

	@Override
	public void onAddedEnum(EnumDecl enm) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getEnumRules().forEach(rule -> rule.onAddedEnum(enm, context));
	}

	@Override
	public void onMatchedRecord(RecordDecl oldRecord, RecordDecl newRecord) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getRecordRules().forEach(rule -> rule.onMatchedRecord(oldRecord, newRecord, context));
	}

	@Override
	public void onRemovedRecord(RecordDecl rcrd) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getRecordRules().forEach(rule -> rule.onRemovedRecord(rcrd, context));
	}

	@Override
	public void onAddedRecord(RecordDecl rcrd) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getRecordRules().forEach(rule -> rule.onAddedRecord(rcrd, context));
	}

	@Override
	public void onMatchedInterface(InterfaceDecl oldInterface, InterfaceDecl newInterface) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getInterfaceRules().forEach(rule -> rule.onMatchedInterface(oldInterface, newInterface, context));
	}

	@Override
	public void onRemovedInterface(InterfaceDecl intf) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getInterfaceRules().forEach(rule -> rule.onRemovedInterface(intf, context));
	}

	@Override
	public void onAddedInterface(InterfaceDecl intf) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getInterfaceRules().forEach(rule -> rule.onAddedInterface(intf, context));
	}

	@Override
	public void onMatchedAnnotation(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getAnnotationRules().forEach(rule -> rule.onMatchedAnnotation(oldAnnotation, newAnnotation, context));
	}

	@Override
	public void onRemovedAnnotation(AnnotationDecl annotation) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getAnnotationRules().forEach(rule -> rule.onRemovedAnnotation(annotation, context));
	}

	@Override
	public void onAddedAnnotation(AnnotationDecl annotation) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getAnnotationRules().forEach(rule -> rule.onAddedAnnotation(annotation, context));
	}

	@Override
	public void onMatchedField(TypeDecl oldType, TypeDecl newType, FieldDecl oldField, FieldDecl newField) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldType, newType, builder);
		ruleSet.getFieldRules().forEach(rule -> rule.onMatchedField(oldField, newField, context));
	}

	@Override
	public void onRemovedField(TypeDecl type, FieldDecl field) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getFieldRules().forEach(rule -> rule.onRemovedField(field, context));
	}

	@Override
	public void onAddedField(TypeDecl type, FieldDecl field) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getFieldRules().forEach(rule -> rule.onAddedField(field, context));
	}

	@Override
	public void onMatchedMethod(TypeDecl oldType, TypeDecl newType, MethodDecl oldMethod, MethodDecl newMethod) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldType, newType, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onMatchedExecutable(oldMethod, newMethod, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onMatchedMethod(oldMethod, newMethod, context));
	}

	@Override
	public void onRemovedMethod(TypeDecl type, MethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onRemovedExecutable(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onRemovedMethod(method, context));
	}

	@Override
	public void onAddedMethod(TypeDecl type, MethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onAddedExecutable(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onAddedMethod(method, context));
	}

	@Override
	public void onMatchedConstructor(ClassDecl oldCls, ClassDecl newCls, ConstructorDecl oldCons, ConstructorDecl newCons) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldCls, newCls, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onMatchedExecutable(oldCons, newCons, context));
		ruleSet.getConstructorRules().forEach(rule -> rule.onMatchedConstructor(oldCons, newCons, context));
	}

	@Override
	public void onRemovedConstructor(ClassDecl cls, ConstructorDecl cons) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, cls, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onRemovedExecutable(cons, context));
		ruleSet.getConstructorRules().forEach(rule -> rule.onRemovedConstructor(cons, context));
	}

	@Override
	public void onAddedConstructor(ClassDecl cls, ConstructorDecl cons) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, cls, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onAddedExecutable(cons, context));
		ruleSet.getConstructorRules().forEach(rule -> rule.onAddedConstructor(cons, context));
	}

	@Override
	public void onMatchedAnnotationMethod(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation,
	                                      AnnotationMethodDecl oldMethod, AnnotationMethodDecl newMethod) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldAnnotation, newAnnotation, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onMatchedExecutable(oldMethod, newMethod, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onMatchedMethod(oldMethod, newMethod, context));
		ruleSet.getAnnotationMethodRules().forEach(rule -> rule.onMatchedAnnotationMethod(oldMethod, newMethod, context));
	}

	@Override
	public void onRemovedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, annotation, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onRemovedExecutable(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onRemovedMethod(method, context));
		ruleSet.getAnnotationMethodRules().forEach(rule -> rule.onRemovedAnnotationMethod(method, context));
	}

	@Override
	public void onAddedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, annotation, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onAddedExecutable(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onRemovedMethod(method, context));
		ruleSet.getAnnotationMethodRules().forEach(rule -> rule.onAddedAnnotationMethod(method, context));
	}
}
