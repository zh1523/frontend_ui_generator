package com.example.uigen.generation;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SfcCodeExtractor {

    private static final Pattern FENCED_VUE = Pattern.compile("```(?:vue|html)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPLATE = Pattern.compile("(?s)(<template[\\s\\S]*?</template>)");
    private static final Pattern SCRIPT = Pattern.compile("(?s)(<script[^>]*>[\\s\\S]*?</script>)");
    private static final Pattern STYLE = Pattern.compile("(?s)(<style[^>]*>[\\s\\S]*?</style>)");

    public SfcSections extract(String rawOutput) {
        String code = extractCodeBlock(rawOutput);
        String template = extractOrDefault(code, TEMPLATE, "<template>\n  <div>Generated component</div>\n</template>");
        String script = ensureScriptSetup(extractOrDefault(code, SCRIPT, "<script setup>\n</script>"));
        String style = extractOrDefault(code, STYLE, "<style scoped>\n</style>");
        String normalized = template + "\n\n" + script + "\n\n" + style + "\n";
        return new SfcSections(normalized, template, script, style);
    }

    private String extractCodeBlock(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return "";
        }
        Matcher matcher = FENCED_VUE.matcher(rawOutput);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return rawOutput.trim();
    }

    private String extractOrDefault(String content, Pattern pattern, String fallback) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return fallback;
    }

    private String ensureScriptSetup(String scriptTag) {
        if (scriptTag.contains("setup")) {
            return scriptTag;
        }
        String body = scriptTag.replaceFirst("(?is)<script[^>]*>", "")
                .replaceFirst("(?is)</script>", "")
                .trim();
        if (body.isEmpty()) {
            return "<script setup>\n</script>";
        }
        return "<script setup>\n" + body + "\n</script>";
    }
}
