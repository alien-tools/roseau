package com.github.maracas.roseau.changes;
import com.github.maracas.roseau.model.FieldDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;


/**
 * Represents a field-related breaking change within an API.
 * This class extends the {@link BreakingChange} class and encapsulates information about the specific field causing the breaking change.
 */
public class FieldBreakingChange extends BreakingChange {
    public FieldDeclaration field;

    public FieldBreakingChange(BreakingChangeKind breakingChangeKind, TypeDeclaration breakingChangeTypeDeclaration, String breakingChangePosition, BreakingChangeNature breakingChangeNature, FieldDeclaration field) {
        super(breakingChangeKind, breakingChangeTypeDeclaration, breakingChangePosition, breakingChangeNature);
        this.field = field;
    }

    /**
     * Retrieves the field associated with the breaking change.
     *
     * @return The fieldDeclaration associated with the breaking change
     */
    public FieldDeclaration getField() {
        return field;
    }

}

