package com.github.maracas.roseau.api.model.reference;

import com.github.maracas.roseau.api.utils.StringUtils;

import java.util.Objects;

public record PrimitiveTypeReference(String qualifiedName) implements ITypeReference {
    public PrimitiveTypeReference {
        Objects.requireNonNull(qualifiedName);
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public String getPrettyQualifiedName() {
        return StringUtils.splitSpecialCharsAndCapitalize(getQualifiedName());
    }

    @Override
    public boolean isSubtypeOf(ITypeReference other) {
        return equals(other); // FIXME
    }

    @Override
    public String toString() {
        return qualifiedName;
    }
}
