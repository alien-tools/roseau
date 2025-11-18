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

import java.util.List;

public interface RuleSet {
	List<Rule<TypeDecl>> getTypeRules();
	List<Rule<ClassDecl>> getClassRules();
	List<Rule<InterfaceDecl>> getInterfaceRules();
	List<Rule<EnumDecl>> getEnumRules();
	List<Rule<RecordDecl>> getRecordRules();
	List<Rule<AnnotationDecl>> getAnnotationRules();

	List<MemberRule<ExecutableDecl>> getExecutableRules();
	List<MemberRule<MethodDecl>> getMethodRules();
	List<MemberRule<ConstructorDecl>> getConstructorRules();
	List<MemberRule<AnnotationMethodDecl>> getAnnotationMethodRules();
	List<MemberRule<FieldDecl>> getFieldRules();
}
