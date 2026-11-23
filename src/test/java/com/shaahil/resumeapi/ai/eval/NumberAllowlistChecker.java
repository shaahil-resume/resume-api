package com.shaahil.resumeapi.ai.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NumberAllowlistChecker {

    private static final Pattern NUMBER = Pattern.compile("(?<![A-Za-z])\\d+(?:\\.\\d+)?");

    private final Set<String> allowedNumbers;

    public NumberAllowlistChecker() {
        this(Path.of("src/main/resources/knowledge"));
    }

    public NumberAllowlistChecker(Path knowledgeRoot) {
        this.allowedNumbers = readNumbers(knowledgeRoot);
    }

    public boolean passes(String answer, String question) {
        return invalidNumbers(answer, question).isEmpty();
    }

    public List<String> invalidNumbers(String answer, String question) {
        Set<String> questionNumbers = extractNumbers(question == null ? "" : question);
        List<String> invalid = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(answer == null ? "" : answer);
        while (matcher.find()) {
            String number = matcher.group();
            if (isListNumber(answer, matcher.start(), matcher.end())) {
                continue;
            }
            if (!allowedNumbers.contains(number) && !questionNumbers.contains(number)) {
                invalid.add(number);
            }
        }
        return invalid;
    }

    private static Set<String> readNumbers(Path knowledgeRoot) {
        try (var paths = Files.walk(knowledgeRoot)) {
            Set<String> numbers = new HashSet<>();
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    numbers.addAll(extractNumbers(Files.readString(path)));
                } catch (IOException exception) {
                    throw new IllegalArgumentException("Could not read knowledge file " + path, exception);
                }
            });
            return Set.copyOf(numbers);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read knowledge directory " + knowledgeRoot, exception);
        }
    }

    private static Set<String> extractNumbers(String text) {
        Set<String> numbers = new HashSet<>();
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers;
    }

    private static boolean isListNumber(String text, int start, int end) {
        int lineStart = text.lastIndexOf('\n', start - 1) + 1;
        boolean onlyWhitespaceBefore = text.substring(lineStart, start).isBlank();
        boolean followedByListPunctuation = end < text.length()
                && (text.charAt(end) == '.' || text.charAt(end) == ')');
        String number = text.substring(start, end);
        return onlyWhitespaceBefore && followedByListPunctuation
                && (number.equals("1") || number.equals("2") || number.equals("3"));
    }
}
