package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
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

	public BreakingChangeAnalyzer(API v1, API v2) {
		this.v1 = Preconditions.checkNotNull(v1);
		this.v2 = Preconditions.checkNotNull(v2);
		this.ruleSet = new DefaultRuleSet();
		this.builder = RoseauReport.builder(v1, v2);
	}

	@Override
	public RoseauReport get() {
		return this.builder.build();
	}

	@Override
	public void onMatchedType(TypeDecl oldType, TypeDecl newType) {
		ruleSet.getTypeRules().forEach(rule -> rule.onMatchedType(oldType, newType, new TypeRuleContext(v1, v2, builder)));

		if (oldType instanceof ClassDecl c1 && newType instanceof ClassDecl c2) {
			diffClass(c1, c2);
		}

		if (oldType instanceof AnnotationDecl a1 && newType instanceof AnnotationDecl a2) {
			diffAnnotationInterface(a1, a2);
		}
	}

	private void diffClass(ClassDecl c1, ClassDecl c2) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getClassRules().forEach(rule -> rule.onMatchedClass(c1, c2, context));
	}

	private void diffAnnotationInterface(AnnotationDecl a1, AnnotationDecl a2) {
		TypeRuleContext context = new TypeRuleContext(v1, v2, builder);
		ruleSet.getAnnotationRules().forEach(rule -> rule.onMatchedAnnotation(a1, a2, context));

		a1.getAnnotationMethods().forEach(m1 -> {
			// Annotation methods do not have parameters, so no overloading going on
			//   -> simple name matching should be enough
			Optional<AnnotationMethodDecl> optMatch = a2.getAnnotationMethods().stream()
				.filter(m2 -> m1.getSimpleName().equals(m2.getSimpleName()))
				.findFirst();

			optMatch.ifPresentOrElse(m2 -> {
				if (m1.hasDefault() && !m2.hasDefault()) {
					builder.memberBC(BreakingChangeKind.ANNOTATION_METHOD_NO_LONGER_DEFAULT, a1, m1, m2);
				}

				if (!m1.getType().equals(m2.getType())) {
					builder.memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, a1, m1, m2,
						new BreakingChangeDetails.MethodReturnTypeChanged(m1.getType(), m2.getType()));
				}
			}, () -> builder.memberBC(BreakingChangeKind.METHOD_REMOVED, a1, m1));
		});

		a2.getAnnotationMethods().stream()
			.filter(m2 -> !m2.hasDefault())
			.filter(m2 -> a1.getAnnotationMethods().stream()
				.noneMatch(m1 -> m1.getSimpleName().equals(m2.getSimpleName())))
			.forEach(m2 ->
				builder.typeBC(BreakingChangeKind.ANNOTATION_METHOD_ADDED_WITHOUT_DEFAULT, a1,
					new BreakingChangeDetails.AnnotationMethodAddedWithoutDefault(m2)));
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
}
