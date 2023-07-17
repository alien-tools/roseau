package com.github.maracas.roseau.model;


import java.util.List;

public class Signature {
    private String Name;
    private List<String> parameterTypes;

    public Signature(String methodName, List<String> parameterTypes) {
        this.Name = methodName;
        this.parameterTypes = parameterTypes;
    }

    public String getName() {
        return Name;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }



}
