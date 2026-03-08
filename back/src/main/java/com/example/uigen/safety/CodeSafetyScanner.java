package com.example.uigen.safety;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CodeSafetyScanner {

    private static final List<PatternRule> BLOCKED_RULES = List.of(
            new PatternRule("eval usage", Pattern.compile("\\beval\\s*\\(")),
            new PatternRule("Function constructor", Pattern.compile("new\\s+Function\\s*\\(")),
            new PatternRule("remote script tag", Pattern.compile("<script\\s+[^>]*src\\s*=")),
            new PatternRule("remote import", Pattern.compile("import\\s+[^;]*from\\s*['\"]https?://")),
            new PatternRule("dynamic import url", Pattern.compile("import\\s*\\(\\s*['\"]https?://")),
            new PatternRule("cookie access", Pattern.compile("document\\.cookie")),
            new PatternRule("network request", Pattern.compile("\\b(fetch|XMLHttpRequest|WebSocket)\\b"))
    );

    public SafetyScanResult scan(String vueCode) {
        List<String> hits = new ArrayList<>();
        for (PatternRule rule : BLOCKED_RULES) {
            if (rule.pattern().matcher(vueCode).find()) {
                hits.add(rule.name());
            }
        }
        if (!hits.isEmpty()) {
            return new SafetyScanResult(SafetyLevel.BLOCKED, "Blocked rules: " + String.join(", ", hits));
        }
        return new SafetyScanResult(SafetyLevel.SAFE, "OK");
    }

    private record PatternRule(String name, Pattern pattern) {
    }
}
