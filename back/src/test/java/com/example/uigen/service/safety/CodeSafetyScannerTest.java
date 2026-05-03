package com.example.uigen.service.safety;

import com.example.uigen.model.dto.SafetyScanResult;
import com.example.uigen.model.enums.SafetyLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CodeSafetyScannerTest {

    private final CodeSafetyScanner scanner = new CodeSafetyScanner();

    @Test
    void shouldBlockDangerousPatterns() {
        String code = """
                <template><div/></template>
                <script setup>
                const run = () => eval("alert(1)");
                </script>
                <style scoped></style>
                """;
        SafetyScanResult result = scanner.scan(code);
        Assertions.assertEquals(SafetyLevel.BLOCKED, result.level());
        Assertions.assertTrue(result.reason().contains("eval usage"));
    }

    @Test
    void shouldAllowSafeCode() {
        String code = """
                <template><div class="box">ok</div></template>
                <script setup>
                const msg = 'hello'
                </script>
                <style scoped>.box{padding:8px;}</style>
                """;
        SafetyScanResult result = scanner.scan(code);
        Assertions.assertEquals(SafetyLevel.SAFE, result.level());
    }
}
