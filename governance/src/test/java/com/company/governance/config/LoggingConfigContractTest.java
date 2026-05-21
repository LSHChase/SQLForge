package com.company.governance.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LoggingConfigContractTest {

    @Test
    void shouldPinUtf8ForAllGovernanceLogEncoders() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/logback-spring.xml")) {
            assertTrue(inputStream != null, "classpath 中必须存在 logback-spring.xml");
            String xml = new String(readAllBytes(inputStream), StandardCharsets.UTF_8);

            assertTrue(xml.contains("<appender name=\"CONSOLE\""), "必须存在控制台 appender 配置");
            assertTrue(xml.contains("<appender name=\"ROLLING_FILE\""), "必须存在滚动文件 appender 配置");
            assertTrue(xml.contains("<appender name=\"JSON_CONSOLE\""), "必须存在 JSON 控制台 appender 配置");
            assertEquals(3, countOccurrences(xml, "<charset>UTF-8</charset>"));
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[4096];
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private int countOccurrences(String content, String target) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
