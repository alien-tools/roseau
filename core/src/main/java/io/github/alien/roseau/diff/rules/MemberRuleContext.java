package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.API;
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
	 * Checks whether existing clients can only see the erasure of the given member's type. This is the case for the
	 * constructors, instance methods, and non-static fields of a type that becomes generic, since existing clients
	 * necessarily use it as a raw type (JLS §4.8).
	 *
	 * @param newMember the member in the new version of the type
	 * @return true if the member's type is erased for existing clients
	 */
	public boolean isErasedForExistingClients(TypeMemberDecl newMember) {
		return oldType.getFormalTypeParameters().isEmpty() && !newType.getFormalTypeParameters().isEmpty() &&
			!newMember.isStatic();
	}
}
