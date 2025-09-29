package io.github.alien.roseau.diff.changes;

import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.lang.annotation.ElementType;

public sealed interface BreakingChangeDetails {
	record None() implements BreakingChangeDetails {}
	record MethodReturnTypeChanged(ITypeReference previousType, ITypeReference newType)
		implements BreakingChangeDetails {}
	record FieldTypeChanged(ITypeReference previousType, ITypeReference newType)
		implements BreakingChangeDetails {}
	record MethodAddedToInterface(MethodDecl newMethod)
		implements BreakingChangeDetails {}
	record MethodAbstractAddedToClass(MethodDecl newMethod)
		implements BreakingChangeDetails {}
	record ClassTypeChanged(Class<?> oldType, Class<?> newType)
		implements BreakingChangeDetails {}
	record SuperTypeRemoved(TypeReference<TypeDecl> superType)
		implements BreakingChangeDetails {}
	record AnnotationTargetRemoved(ElementType target)
		implements BreakingChangeDetails {}
	record MethodNoLongerThrowsCheckedException(ITypeReference exception)
		implements BreakingChangeDetails {}
	record MethodNowThrowsCheckedException(ITypeReference exception)
		implements BreakingChangeDetails {}
	record MethodParameterGenericsChanged(ITypeReference oldType,
	                                      ITypeReference newType)
		implements BreakingChangeDetails {}
	record TypeFormalTypeParametersRemoved(FormalTypeParameter ftp)
		implements BreakingChangeDetails {}
	record TypeFormalTypeParametersAdded(FormalTypeParameter ftp)
		implements BreakingChangeDetails {}
	record TypeFormalTypeParametersChanged(FormalTypeParameter oldFtp,
	                                       FormalTypeParameter newFtp)
		implements BreakingChangeDetails {}
	record MethodFormalTypeParametersRemoved(FormalTypeParameter ftp)
		implements BreakingChangeDetails {}
	record MethodFormalTypeParametersAdded(FormalTypeParameter ftp)
		implements BreakingChangeDetails {}
	record MethodFormalTypeParametersChanged(FormalTypeParameter oldFtp,
	                                       FormalTypeParameter newFtp)
		implements BreakingChangeDetails {}
	record AnnotationMethodAddedWithoutDefault(MethodDecl newMethod)
		implements BreakingChangeDetails {}
}
