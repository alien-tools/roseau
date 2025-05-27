package io.github.alien.roseau.api.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class StringUtils {
    public static String splitSpecialCharsAndCapitalize(String s) {
        return Arrays.stream(s
                        .replaceAll("\\[]", "Arr")
                        .replaceAll("\\.\\.\\.", "Varargs")
                        .split("\\W+|_+")
                )
                .map(StringUtils::capitalizeFirstLetter)
                .collect(Collectors.joining());
    }

    public static String cleanInnerSymbolInQualifiedName(String s) {
        return s.replaceAll("\\$", ".");
    }

    public static String cleanInnerSymbolInSimpleName(String s) {
        var parts = s.split("\\$");
        return parts[parts.length - 1];
    }

    public static String capitalizeFirstLetter(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
