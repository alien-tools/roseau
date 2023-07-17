package com.github.maracas.roseau.changes;
import com.github.maracas.roseau.model.FieldDeclaration;
import java.util.List;
import java.util.ArrayList;


public class FieldBreakingChange extends BreakingChangeElement {
    public FieldDeclaration field;

    public FieldBreakingChange(FieldDeclaration field) {
        this.field = field;
    }

    public List<String> getElement() {
        List<String> elementInfo = new ArrayList<>();
        elementInfo.add(field.getName());
        elementInfo.add(field.getType().getName());
        return elementInfo;
    }

    public void setFieldBreakingChange(FieldDeclaration field) {
        this.field = field;
    }
}
