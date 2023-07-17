package com.github.maracas.roseau.model;

import java.util.List;

public class API {
    public List<TypeDeclaration> AllTheTypes;

    public API(List<TypeDeclaration> AlTheTypes) {
        this.AllTheTypes = AlTheTypes;
    }

    public List<TypeDeclaration> getAllTheTypes() {
        return AllTheTypes;
    }

    public void setAllTheTypes(List<TypeDeclaration> AlTheTypes) {
        this.AllTheTypes = AlTheTypes;
    }

}
