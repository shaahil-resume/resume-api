package com.shaahil.resumeapi.ai.eval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public final class CaseChecker {

    private final NumberAllowlistChecker numberAllowlistChecker;
    private final StyleChecker styleChecker;

    public CaseChecker(NumberAllowlistChecker numberAllowlistChecker, StyleChecker styleChecker) {
        this.numberAllowlistChecker = numberAllowlistChecker;
        this.styleChecker = styleChecker;
    }

    public boolean passes(GoldenCase goldenCase, String answer, String actualRoute, Collection<String> actualKeys) {
        return failures(goldenCase, answer, actualRoute, actualKeys).isEmpty();
    }

    public List<String> failures(GoldenCase goldenCase, String answer, String actualRoute,
                                 Collection<String> actualKeys) {
        List<String> failures = new ArrayList<>();
        if (!routeAllowed(goldenCase, actualRoute)) {
            failures.add("route");
        }
        for (String expectedKey : goldenCase.expectedKeyContains()) {
            if (actualKeys.stream().noneMatch(key -> key.contains(expectedKey))) {
                failures.add("key " + expectedKey);
            }
        }
        for (String regex : goldenCase.mustMatch()) {
            if (!Pattern.compile(regex).matcher(answer).find()) {
                failures.add("mustMatch " + regex);
            }
        }
        for (String regex : goldenCase.mustNotMatch()) {
            if (Pattern.compile(regex).matcher(answer).find()) {
                failures.add("mustNotMatch " + regex);
            }
        }
        if (!numberAllowlistChecker.passes(answer, goldenCase.question())) {
            failures.add("number allowlist");
        }
        if (!styleChecker.passes(answer)) {
            failures.add("style");
        }
        return failures;
    }

    private static boolean routeAllowed(GoldenCase goldenCase, String actualRoute) {
        if (goldenCase.category().equals("NOT_ON_RESUME") || goldenCase.category().equals("FALSE_PREMISE")) {
            return actualRoute.equals("ANSWER") || actualRoute.equals("DECLINE");
        }
        return goldenCase.expectedRoute().equals(actualRoute);
    }
}
