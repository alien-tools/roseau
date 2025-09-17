package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;

import java.util.List;

public sealed interface ISealableTypeDecl permits ClassDecl, InterfaceDecl {
    // Just duplicating the simpleName of permitted types CtSealable for now, but we should probably refactor this
    List<String> getPermittedTypes();
}
