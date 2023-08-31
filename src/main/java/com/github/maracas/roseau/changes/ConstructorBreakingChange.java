package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.ConstructorDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;


/**
 * Represents a constructor-related breaking change within an API.
 * This class extends the {@link BreakingChange} class and encapsulates information about the specific constructor causing the breaking change.
 */
public class ConstructorBreakingChange extends BreakingChange {
    private ConstructorDeclaration constructor;

    public ConstructorBreakingChange(BreakingChangeKind breakingChangeKind, TypeDeclaration breakingChangeTypeDeclaration, String breakingChangePosition, BreakingChangeNature breakingChangeNature, ConstructorDeclaration constructor) {
        super(breakingChangeKind, breakingChangeTypeDeclaration, breakingChangePosition, breakingChangeNature);
        this.constructor = constructor;
    }

    /**
     * Retrieves the constructor associated with the breaking change.
     *
     * @return The constructorDeclaration associated with the breaking change
     */

    public ConstructorDeclaration getConstructor() {
        return constructor;
    }

}

