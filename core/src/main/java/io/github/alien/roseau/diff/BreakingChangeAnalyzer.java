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
		ruleSet.getTypeRules().forEach(rule -> rule.onMatched(oldType, newType, context));
	}

	@Override
	public void onRemovedType(TypeDecl type) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getTypeRules().forEach(rule -> rule.onRemoved(type, context));
	}

	@Override
	public void onAddedType(TypeDecl type) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getTypeRules().forEach(rule -> rule.onAdded(type, context));
	}

	@Override
	public void onMatchedClass(ClassDecl oldCls, ClassDecl newCls) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getClassRules().forEach(rule -> rule.onMatched(oldCls, newCls, context));
	}

	@Override
	public void onRemovedClass(ClassDecl cls) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getClassRules().forEach(rule -> rule.onRemoved(cls, context));
	}

	@Override
	public void onAddedClass(ClassDecl cls) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getClassRules().forEach(rule -> rule.onAdded(cls, context));
	}

	@Override
	public void onMatchedEnum(EnumDecl oldEnum, EnumDecl newEnum) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getEnumRules().forEach(rule -> rule.onMatched(oldEnum, newEnum, context));
	}

	@Override
	public void onRemovedEnum(EnumDecl enm) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getEnumRules().forEach(rule -> rule.onRemoved(enm, context));
	}

	@Override
	public void onAddedEnum(EnumDecl enm) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getEnumRules().forEach(rule -> rule.onAdded(enm, context));
	}

	@Override
	public void onMatchedRecord(RecordDecl oldRecord, RecordDecl newRecord) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getRecordRules().forEach(rule -> rule.onMatched(oldRecord, newRecord, context));
	}

	@Override
	public void onRemovedRecord(RecordDecl rcrd) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getRecordRules().forEach(rule -> rule.onRemoved(rcrd, context));
	}

	@Override
	public void onAddedRecord(RecordDecl rcrd) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getRecordRules().forEach(rule -> rule.onAdded(rcrd, context));
	}

	@Override
	public void onMatchedInterface(InterfaceDecl oldInterface, InterfaceDecl newInterface) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getInterfaceRules().forEach(rule -> rule.onMatched(oldInterface, newInterface, context));
	}

	@Override
	public void onRemovedInterface(InterfaceDecl intf) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getInterfaceRules().forEach(rule -> rule.onRemoved(intf, context));
	}

	@Override
	public void onAddedInterface(InterfaceDecl intf) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getInterfaceRules().forEach(rule -> rule.onAdded(intf, context));
	}

	@Override
	public void onMatchedAnnotation(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getAnnotationRules().forEach(rule -> rule.onMatched(oldAnnotation, newAnnotation, context));
	}

	@Override
	public void onRemovedAnnotation(AnnotationDecl annotation) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getAnnotationRules().forEach(rule -> rule.onRemoved(annotation, context));
	}

	@Override
	public void onAddedAnnotation(AnnotationDecl annotation) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getAnnotationRules().forEach(rule -> rule.onAdded(annotation, context));
	}

	@Override
	public void onMatchedField(TypeDecl oldType, TypeDecl newType, FieldDecl oldField, FieldDecl newField) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldType, newType, builder);
		ruleSet.getFieldRules().forEach(rule -> rule.onMatched(oldField, newField, context));
	}

	@Override
	public void onRemovedField(TypeDecl type, FieldDecl field) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getFieldRules().forEach(rule -> rule.onRemoved(field, context));
	}

	@Override
	public void onAddedField(TypeDecl type, FieldDecl field) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getFieldRules().forEach(rule -> rule.onAdded(field, context));
	}

	@Override
	public void onMatchedMethod(TypeDecl oldType, TypeDecl newType, MethodDecl oldMethod, MethodDecl newMethod) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldType, newType, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onMatched(oldMethod, newMethod, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onMatched(oldMethod, newMethod, context));
	}

	@Override
	public void onRemovedMethod(TypeDecl type, MethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onRemoved(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onRemoved(method, context));
	}

	@Override
	public void onAddedMethod(TypeDecl type, MethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, type, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onAdded(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onAdded(method, context));
	}

	@Override
	public void onMatchedConstructor(ClassDecl oldCls, ClassDecl newCls, ConstructorDecl oldCons, ConstructorDecl newCons) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldCls, newCls, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onMatched(oldCons, newCons, context));
		ruleSet.getConstructorRules().forEach(rule -> rule.onMatched(oldCons, newCons, context));
	}

	@Override
	public void onRemovedConstructor(ClassDecl cls, ConstructorDecl cons) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, cls, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onRemoved(cons, context));
		ruleSet.getConstructorRules().forEach(rule -> rule.onRemoved(cons, context));
	}

	@Override
	public void onAddedConstructor(ClassDecl cls, ConstructorDecl cons) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, cls, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onAdded(cons, context));
		ruleSet.getConstructorRules().forEach(rule -> rule.onAdded(cons, context));
	}

	@Override
	public void onMatchedAnnotationMethod(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation,
	                                      AnnotationMethodDecl oldMethod, AnnotationMethodDecl newMethod) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldAnnotation, newAnnotation, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onMatched(oldMethod, newMethod, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onMatched(oldMethod, newMethod, context));
		ruleSet.getAnnotationMethodRules().forEach(rule -> rule.onMatched(oldMethod, newMethod, context));
	}

	@Override
	public void onRemovedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, annotation, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onRemoved(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onRemoved(method, context));
		ruleSet.getAnnotationMethodRules().forEach(rule -> rule.onRemoved(method, context));
	}

	@Override
	public void onAddedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, annotation, null, builder);
		ruleSet.getExecutableRules().forEach(rule -> rule.onAdded(method, context));
		ruleSet.getMethodRules().forEach(rule -> rule.onAdded(method, context));
		ruleSet.getAnnotationMethodRules().forEach(rule -> rule.onAdded(method, context));
	}
}
