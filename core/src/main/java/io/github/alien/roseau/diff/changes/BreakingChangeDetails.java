package io.github.alien.roseau.diff.changes;

import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.lang.annotation.ElementType;

public sealed interface BreakingChangeDetails {
	record None() implements BreakingChangeDetails {}
	record TypeNewAbstractMethod(MethodDecl newMethod) implements BreakingChangeDetails {}
	record TypeKindChanged(Class<?> oldType, Class<?> newType) implements BreakingChangeDetails {}
	record MethodParameterGenericsChanged(ITypeReference oldType, ITypeReference newType)
		implements BreakingChangeDetails {}
	record TypeSupertypeRemoved(TypeReference<TypeDecl> superType) implements BreakingChangeDetails {}
	record AnnotationNewMethodWithoutDefault(MethodDecl newMethod) implements BreakingChangeDetails {}
	record AnnotationTargetRemoved(ElementType target) implements BreakingChangeDetails {}
	record FieldTypeChanged(ITypeReference oldType, ITypeReference newType) implements BreakingChangeDetails {}
	record MethodReturnTypeChanged(ITypeReference oldType, ITypeReference newType) implements BreakingChangeDetails {}
	record MethodNoLongerThrowsCheckedException(ITypeReference exception) implements BreakingChangeDetails {}
	record MethodNowThrowsCheckedException(ITypeReference exception) implements BreakingChangeDetails {}
	record FormalTypeParametersChanged(FormalTypeParameter oldFtp, FormalTypeParameter newFtp)
		implements BreakingChangeDetails {}
	record FormalTypeParametersRemoved(FormalTypeParameter ftp) implements BreakingChangeDetails {}
	record FormalTypeParametersAdded(FormalTypeParameter ftp) implements BreakingChangeDetails {}
}
