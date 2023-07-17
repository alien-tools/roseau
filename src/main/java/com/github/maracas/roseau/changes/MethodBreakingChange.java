package com.github.maracas.roseau.changes;
import com.github.maracas.roseau.model.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

public class MethodBreakingChange extends BreakingChangeElement {
    public MethodDeclaration method;

    public MethodBreakingChange(MethodDeclaration method) {
        this.method = method;
    }


    public List<String> getElement() {
        List<String> elementInfo = new ArrayList<>();
        elementInfo.add(method.getName());
        elementInfo.add(method.getType().getName());
        return elementInfo;
    }

    public void setMethodBreakingChange(MethodDeclaration method) {
        this.method = method;
    }
}
