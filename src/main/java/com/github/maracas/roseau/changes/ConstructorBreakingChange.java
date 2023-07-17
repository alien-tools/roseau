package com.github.maracas.roseau.changes;

import com.github.maracas.roseau.model.ConstructorDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ConstructorBreakingChange extends BreakingChangeElement {
    public ConstructorDeclaration constructor;

    public ConstructorBreakingChange(ConstructorDeclaration constructor) {
        this.constructor = constructor;
    }

    public List<String> getElement() {
        List<String> elementInfo = new ArrayList<>();
        elementInfo.add(constructor.getName());
        elementInfo.add(constructor.getType().getName());
        return elementInfo;
    }

    public void setConstructorBreakingChange(ConstructorDeclaration constructor) {
        this.constructor = constructor;
    }
}

