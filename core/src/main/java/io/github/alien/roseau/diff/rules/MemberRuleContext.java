package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.RoseauReport;

public record MemberRuleContext(
	API v1,
	API v2,
	TypeDecl oldType,
	TypeDecl newType,
	RoseauReport.Builder builder
) {
	/**
	 * Checks whether existing clients can only see the erasure of the given member's declaration. This is the case
	 * for the constructors, instance methods, and non-static fields of a type that becomes generic, since existing
	 * clients necessarily use it as a raw type (JLS §4.8).
	 *
	 * @param newMember the member in the new version of the type
	 * @return true if the member's type is erased for existing clients
	 * @see io.github.alien.roseau.api.analysis.HierarchyProvider#isErasedInRawType(TypeDecl, TypeMemberDecl)
	 */
	public boolean isErasedForExistingClients(TypeMemberDecl newMember) {
		return oldType.getFormalTypeParameters().isEmpty() && v2.analyzer().isErasedInRawType(newType, newMember);
	}

	/**
	 * Checks whether lambda expressions and method references can implement the given executable. This is the case
	 * for the abstract methods of an interface, which they implement directly or through a subinterface that
	 * implements its other abstract methods (JLS §9.8).
	 *
	 * @param oldExecutable the executable in the old version of the type
	 * @return true if lambda expressions can implement the executable
	 */
	public boolean isLambdaImplementable(ExecutableDecl oldExecutable) {
		return oldType.isInterface() &&
			oldExecutable instanceof MethodDecl method && !method.isDefault() && !method.isStatic();
	}
}
