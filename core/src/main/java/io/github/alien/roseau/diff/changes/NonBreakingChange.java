package io.github.alien.roseau.diff.changes;

import io.github.alien.roseau.api.model.Symbol;

import java.util.Objects;

/**
 * Represents a non-breaking change between two API versions.
 */
public final class NonBreakingChange {
    private final NonBreakingChangeKind kind;
    private final Symbol impactedSymbol;
    private final Symbol newSymbol;

    public NonBreakingChange(NonBreakingChangeKind kind, Symbol impactedSymbol, Symbol newSymbol) {
        this.kind = Objects.requireNonNull(kind);
        this.impactedSymbol = impactedSymbol;
        this.newSymbol = newSymbol;
    }

    public NonBreakingChangeKind kind() {
        return kind;
    }

    public Symbol impactedSymbol() {
        return impactedSymbol;
    }

    public Symbol newSymbol() {
        return newSymbol;
    }

    @Override
    public String toString() {
        String impacted = impactedSymbol != null ? impactedSymbol.getQualifiedName() : "";
        String added = newSymbol != null ? newSymbol.getQualifiedName() : "";
        return kind + " " + (impacted.isEmpty() ? added : impacted);
    }
}


