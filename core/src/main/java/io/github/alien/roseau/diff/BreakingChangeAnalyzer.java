package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.rules.DefaultRuleSet;
import io.github.alien.roseau.diff.rules.MemberRule;
import io.github.alien.roseau.diff.rules.MemberRuleContext;
import io.github.alien.roseau.diff.rules.RuleSet;
import io.github.alien.roseau.diff.rules.TypeRule;
import io.github.alien.roseau.diff.rules.TypeRuleContext;

import java.util.List;
import java.util.function.BiConsumer;

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
		applyTypeRules(ruleSet.getTypeRules(), (rule, ctx) -> rule.onMatched(oldType, newType, ctx));
	}

	@Override
	public void onRemovedType(TypeDecl type) {
		applyTypeRules(ruleSet.getTypeRules(), (rule, ctx) -> rule.onRemoved(type, ctx));
	}

	@Override
	public void onAddedType(TypeDecl type) {
		applyTypeRules(ruleSet.getTypeRules(), (rule, ctx) -> rule.onAdded(type, ctx));
	}

	@Override
	public void onMatchedClass(ClassDecl oldCls, ClassDecl newCls) {
		applyTypeRules(ruleSet.getClassRules(), (rule, ctx) -> rule.onMatched(oldCls, newCls, ctx));
	}

	@Override
	public void onRemovedClass(ClassDecl cls) {
		applyTypeRules(ruleSet.getClassRules(), (rule, ctx) -> rule.onRemoved(cls, ctx));
	}

	@Override
	public void onAddedClass(ClassDecl cls) {
		applyTypeRules(ruleSet.getClassRules(), (rule, ctx) -> rule.onAdded(cls, ctx));
	}

	@Override
	public void onMatchedEnum(EnumDecl oldEnum, EnumDecl newEnum) {
		applyTypeRules(ruleSet.getEnumRules(), (rule, ctx) -> rule.onMatched(oldEnum, newEnum, ctx));
	}

	@Override
	public void onRemovedEnum(EnumDecl enm) {
		applyTypeRules(ruleSet.getEnumRules(), (rule, ctx) -> rule.onRemoved(enm, ctx));
	}

	@Override
	public void onAddedEnum(EnumDecl enm) {
		applyTypeRules(ruleSet.getEnumRules(), (rule, ctx) -> rule.onAdded(enm, ctx));
	}

	@Override
	public void onMatchedRecord(RecordDecl oldRecord, RecordDecl newRecord) {
		applyTypeRules(ruleSet.getRecordRules(), (rule, ctx) -> rule.onMatched(oldRecord, newRecord, ctx));
	}

	@Override
	public void onRemovedRecord(RecordDecl rcrd) {
		applyTypeRules(ruleSet.getRecordRules(), (rule, ctx) -> rule.onRemoved(rcrd, ctx));
	}

	@Override
	public void onAddedRecord(RecordDecl rcrd) {
		applyTypeRules(ruleSet.getRecordRules(), (rule, ctx) -> rule.onAdded(rcrd, ctx));
	}

	@Override
	public void onMatchedInterface(InterfaceDecl oldInterface, InterfaceDecl newInterface) {
		applyTypeRules(ruleSet.getInterfaceRules(), (rule, ctx) -> rule.onMatched(oldInterface, newInterface, ctx));
	}

	@Override
	public void onRemovedInterface(InterfaceDecl intf) {
		applyTypeRules(ruleSet.getInterfaceRules(), (rule, ctx) -> rule.onRemoved(intf, ctx));
	}

	@Override
	public void onAddedInterface(InterfaceDecl intf) {
		applyTypeRules(ruleSet.getInterfaceRules(), (rule, ctx) -> rule.onAdded(intf, ctx));
	}

	@Override
	public void onMatchedAnnotation(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation) {
		applyTypeRules(ruleSet.getAnnotationRules(), (rule, ctx) -> rule.onMatched(oldAnnotation, newAnnotation, ctx));
	}

	@Override
	public void onRemovedAnnotation(AnnotationDecl annotation) {
		applyTypeRules(ruleSet.getAnnotationRules(), (rule, ctx) -> rule.onRemoved(annotation, ctx));
	}

	@Override
	public void onAddedAnnotation(AnnotationDecl annotation) {
		applyTypeRules(ruleSet.getAnnotationRules(), (rule, ctx) -> rule.onAdded(annotation, ctx));
	}

	@Override
	public void onMatchedField(TypeDecl oldType, TypeDecl newType, FieldDecl oldField, FieldDecl newField) {
		applyMemberRules(ruleSet.getFieldRules(), oldType, newType, (rule, ctx) -> rule.onMatched(oldField, newField, ctx));
	}

	@Override
	public void onRemovedField(TypeDecl type, FieldDecl field) {
		applyMemberRules(ruleSet.getFieldRules(), type, type, (rule, ctx) -> rule.onRemoved(field, ctx));
	}

	@Override
	public void onAddedField(TypeDecl type, FieldDecl field) {
		applyMemberRules(ruleSet.getFieldRules(), type, type, (rule, ctx) -> rule.onAdded(field, ctx));
	}

	@Override
	public void onMatchedMethod(TypeDecl oldType, TypeDecl newType, MethodDecl oldMethod, MethodDecl newMethod) {
		applyMemberRules(ruleSet.getExecutableRules(), oldType, newType,
			(rule, ctx) -> rule.onMatched(oldMethod, newMethod, ctx));
		applyMemberRules(ruleSet.getMethodRules(), oldType, newType,
			(rule, ctx) -> rule.onMatched(oldMethod, newMethod, ctx));
	}

	@Override
	public void onRemovedMethod(TypeDecl type, MethodDecl method) {
		applyMemberRules(ruleSet.getExecutableRules(), type, type, (rule, ctx) -> rule.onRemoved(method, ctx));
		applyMemberRules(ruleSet.getMethodRules(), type, type, (rule, ctx) -> rule.onRemoved(method, ctx));
	}

	@Override
	public void onAddedMethod(TypeDecl type, MethodDecl method) {
		applyMemberRules(ruleSet.getExecutableRules(), type, type, (rule, ctx) -> rule.onAdded(method, ctx));
		applyMemberRules(ruleSet.getMethodRules(), type, type, (rule, ctx) -> rule.onAdded(method, ctx));
	}

	@Override
	public void onMatchedConstructor(ClassDecl oldCls, ClassDecl newCls, ConstructorDecl oldCons, ConstructorDecl newCons) {
		applyMemberRules(ruleSet.getExecutableRules(), oldCls, newCls,
			(rule, ctx) -> rule.onMatched(oldCons, newCons, ctx));
		applyMemberRules(ruleSet.getConstructorRules(), oldCls, newCls,
			(rule, ctx) -> rule.onMatched(oldCons, newCons, ctx));
	}

	@Override
	public void onRemovedConstructor(ClassDecl cls, ConstructorDecl cons) {
		applyMemberRules(ruleSet.getExecutableRules(), cls, cls, (rule, ctx) -> rule.onRemoved(cons, ctx));
		applyMemberRules(ruleSet.getConstructorRules(), cls, cls, (rule, ctx) -> rule.onRemoved(cons, ctx));
	}

	@Override
	public void onAddedConstructor(ClassDecl cls, ConstructorDecl cons) {
		applyMemberRules(ruleSet.getExecutableRules(), cls, cls, (rule, ctx) -> rule.onAdded(cons, ctx));
		applyMemberRules(ruleSet.getConstructorRules(), cls, cls, (rule, ctx) -> rule.onAdded(cons, ctx));
	}

	@Override
	public void onMatchedAnnotationMethod(AnnotationDecl oldAnnotation, AnnotationDecl newAnnotation,
	                                      AnnotationMethodDecl oldMethod, AnnotationMethodDecl newMethod) {
		applyMemberRules(ruleSet.getExecutableRules(), oldAnnotation, newAnnotation,
			(rule, ctx) -> rule.onMatched(oldMethod, newMethod, ctx));
		applyMemberRules(ruleSet.getMethodRules(), oldAnnotation, newAnnotation,
			(rule, ctx) -> rule.onMatched(oldMethod, newMethod, ctx));
		applyMemberRules(ruleSet.getAnnotationMethodRules(), oldAnnotation, newAnnotation,
			(rule, ctx) -> rule.onMatched(oldMethod, newMethod, ctx));
	}

	@Override
	public void onRemovedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method) {
		applyMemberRules(ruleSet.getExecutableRules(), annotation, annotation,
			(rule, ctx) -> rule.onRemoved(method, ctx));
		applyMemberRules(ruleSet.getMethodRules(), annotation, annotation,
			(rule, ctx) -> rule.onRemoved(method, ctx));
		applyMemberRules(ruleSet.getAnnotationMethodRules(), annotation, annotation,
			(rule, ctx) -> rule.onRemoved(method, ctx));
	}

	@Override
	public void onAddedAnnotationMethod(AnnotationDecl annotation, AnnotationMethodDecl method) {
		applyMemberRules(ruleSet.getExecutableRules(), annotation, annotation,
			(rule, ctx) -> rule.onAdded(method, ctx));
		applyMemberRules(ruleSet.getMethodRules(), annotation, annotation,
			(rule, ctx) -> rule.onAdded(method, ctx));
		applyMemberRules(ruleSet.getAnnotationMethodRules(), annotation, annotation,
			(rule, ctx) -> rule.onAdded(method, ctx));
	}

	private <T extends TypeDecl> void applyTypeRules(List<TypeRule<T>> rules,
	                                                 BiConsumer<TypeRule<T>, TypeRuleContext> action) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		rules.forEach(rule -> action.accept(rule, context));
	}

	private <T extends TypeMemberDecl> void applyMemberRules(List<MemberRule<T>> rules, TypeDecl oldType, TypeDecl newType,
	                                                         BiConsumer<MemberRule<T>, MemberRuleContext> action) {
		MemberRuleContext context = new MemberRuleContext(v1, v2, oldType, newType, builder);
		rules.forEach(rule -> action.accept(rule, context));
	}
}
