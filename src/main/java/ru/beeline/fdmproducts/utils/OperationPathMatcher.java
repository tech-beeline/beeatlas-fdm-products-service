/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OperationPathMatcher {

    private static final Pattern PATH_PARAM = Pattern.compile("\\{[^}]+}");
    private static final Pattern REGEX_SPECIAL = Pattern.compile("([.^$*+?()\\[\\]|\\\\])");
    private static final String PARAM_PLACEHOLDER = "{param}";
    private static final String PARAM_SEGMENT_REGEX = "[^/]+";

    private OperationPathMatcher() {
    }

    public static boolean matches(String catalogName, String requestedName) {
        if (catalogName == null || requestedName == null) {
            return false;
        }
        String catalog = withoutQuery(catalogName);
        String requested = withoutQuery(requestedName);
        if (normalize(catalog).equals(normalize(requested))) {
            return true;
        }
        return !hasPathParam(requested) && hasPathParam(catalog)
                && requested.matches(toSegmentRegex(catalog));
    }

    public static boolean typeMatches(String catalogType, String requestedType) {
        return catalogType != null && requestedType != null
                && catalogType.trim().equalsIgnoreCase(requestedType.trim());
    }

    public static String normalize(String name) {
        return PATH_PARAM.matcher(withoutQuery(name)).replaceAll(Matcher.quoteReplacement(PARAM_PLACEHOLDER))
                .toLowerCase(Locale.ROOT);
    }

    private static boolean hasPathParam(String name) {
        return PATH_PARAM.matcher(name).find();
    }

    private static String toSegmentRegex(String catalog) {
        String escaped = REGEX_SPECIAL.matcher(catalog).replaceAll("\\\\$1");
        return "(?i)" + PATH_PARAM.matcher(escaped).replaceAll(PARAM_SEGMENT_REGEX);
    }

    private static String withoutQuery(String name) {
        int idx = name.indexOf('?');
        return (idx >= 0 ? name.substring(0, idx) : name).trim();
    }
}
