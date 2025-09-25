package io.github.alien.roseau.diff.changes;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;

public sealed interface BreakingChangeDetails {
	record None() implements BreakingChangeDetails {}

	record MethodReturnTypeChanged(
		ITypeReference previousType, ITypeReference newType)
		implements BreakingChangeDetails {}
	record MethodAddedToInterface(MethodDecl newMethod) implements BreakingChangeDetails {}
	record MethodAbstractAddedToClass(MethodDecl newMethod) implements BreakingChangeDetails {}
}
