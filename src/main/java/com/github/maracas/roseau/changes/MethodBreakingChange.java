
package com.github.maracas.roseau.changes;
import com.github.maracas.roseau.model.MethodDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;

/**
 * Represents a method-related breaking change within an API.
 * This class extends the {@link BreakingChange} class and encapsulates information about the specific method causing the breaking change.
 */
public class MethodBreakingChange extends BreakingChange {
    private MethodDeclaration method;

    public MethodBreakingChange(BreakingChangeKind breakingChangeKind, TypeDeclaration breakingChangeTypeDeclaration, String breakingChangePosition, BreakingChangeNature breakingChangeNature, MethodDeclaration method) {
        super(breakingChangeKind, breakingChangeTypeDeclaration, breakingChangePosition, breakingChangeNature);
        this.method = method;
    }

    /**
     * Retrieves the method associated with the breaking change.
     *
     * @return The methodDeclaration associated with the breaking change
     */
    public MethodDeclaration getMethod() {
        return method;
    }

}
