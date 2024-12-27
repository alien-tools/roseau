package com.github.maracas.roseau.api.model;

import java.util.List;

public sealed interface ISealableTypeDecl permits ClassDecl, InterfaceDecl {
    // Just duplicating the simpleName of permitted types CtSealable for now, but we should probably refactor this
    List<String> getPermittedTypes();
}
