package com.github.maracas.roseau.changes;

public sealed interface BreakingChange permits TypeBreakingChange, ConstructorBreakingChange, MethodBreakingChange, FieldBreakingChange {
}
