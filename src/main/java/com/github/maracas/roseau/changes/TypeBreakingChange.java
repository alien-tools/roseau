package com.github.maracas.roseau.changes;


import com.github.maracas.roseau.model.MethodDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;

/**
 * Represents a type-related breaking change (class, interface, etc.) within an API.
 * This class extends the {@link BreakingChange} and handles exclusively type-specific BCs.
 */
public class TypeBreakingChange extends BreakingChange {

    public TypeBreakingChange(BreakingChangeKind breakingChangeKind, TypeDeclaration breakingChangeTypeDeclaration, String breakingChangePosition, BreakingChangeNature breakingChangeNature) {
        super(breakingChangeKind, breakingChangeTypeDeclaration, breakingChangePosition, breakingChangeNature);
    }

}
