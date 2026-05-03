package com.example.uigen.service.parser;

import com.example.uigen.model.dto.SfcSections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SfcCodeExtractorTest {

    private final SfcCodeExtractor extractor = new SfcCodeExtractor();

    @Test
    void shouldExtractSectionsFromFencedCode() {
        String raw = """
                ```vue
                <template><div>Hello</div></template>
                <script setup>
                const message = 'hi'
                </script>
                <style scoped>
                .x { color: red; }
                </style>
                ```
                """;
        SfcSections sections = extractor.extract(raw);
        Assertions.assertTrue(sections.templateCode().contains("<template>"));
        Assertions.assertTrue(sections.scriptCode().contains("<script setup>"));
        Assertions.assertTrue(sections.styleCode().contains("<style scoped>"));
        Assertions.assertTrue(sections.vueCode().contains("Hello"));
    }

    @Test
    void shouldFallbackWhenMissingBlocks() {
        SfcSections sections = extractor.extract("plain text");
        Assertions.assertTrue(sections.templateCode().contains("<template>"));
        Assertions.assertTrue(sections.scriptCode().contains("<script setup>"));
        Assertions.assertTrue(sections.styleCode().contains("<style scoped>"));
    }
}
