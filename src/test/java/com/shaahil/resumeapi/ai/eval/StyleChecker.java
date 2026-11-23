package com.shaahil.resumeapi.ai.eval;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class StyleChecker {

    private static final Pattern TAG = Pattern.compile("\\[[^\\]]*\\]");
    private static final Pattern SENTENCE_START = Pattern.compile(
            "(?im)(^|[.!?]\\s+)(He\\s|Shaahil\\s+is\\b)");

    public boolean passes(String answer) {
        return violations(answer).isEmpty();
    }

    public List<String> violations(String answer) {
        String value = answer == null ? "" : answer;
        List<String> violations = new ArrayList<>();
        if (TAG.matcher(value).find()) {
            violations.add("source tag");
        }
        if (value.toLowerCase().contains("the material")) {
            violations.add("the material");
        }
        if (value.toLowerCase().contains("provided information")) {
            violations.add("provided information");
        }
        if (SENTENCE_START.matcher(value).find()) {
            violations.add("third-person sentence");
        }
        return violations;
    }
}
