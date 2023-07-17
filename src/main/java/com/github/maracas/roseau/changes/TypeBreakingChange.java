package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class TypeBreakingChange extends BreakingChangeElement {
    public TypeDeclaration type;

    public TypeBreakingChange(TypeDeclaration type) {
        this.type = type;
    }

    public List<String> getElement() {
        List<String> elementInfo = new ArrayList<>();
        elementInfo.add(type.getName());
        elementInfo.add(type.getName());
        return elementInfo;
    }

    public void setTypeBreakingChange(TypeDeclaration type) {
        this.type = type;
    }
}
