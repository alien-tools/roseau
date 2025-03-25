package io.github.alien.roseau.api.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class StringUtils {
    public static String splitSpecialCharsAndCapitalize(String s) {
        return Arrays.stream(s.split("\\W+|_+"))
                .map(StringUtils::capitalizeFirstLetter)
                .collect(Collectors.joining());
    }

    public static String capitalizeFirstLetter(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
