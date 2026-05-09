package com.company.sqlforge.common.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SensitiveDataCryptoServiceTest {

    private static final String TEST_BASE64_KEY = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

    @Test
    void shouldEncryptAndDecryptEnvelopeWithDefaultProvider() {
        SensitiveDataCryptoService service = new SensitiveDataCryptoService(properties());

        String envelope = service.encrypt("jdbc.password=secret");

        assertTrue(envelope.startsWith("ENC::AES256_GCM::test-key::"));
        assertNotEquals("jdbc.password=secret", envelope);
        assertEquals("jdbc.password=secret", service.decrypt(envelope));
    }

    @Test
    void shouldEncryptAndDecryptBytesWithDefaultProvider() {
        SensitiveDataCryptoService service = new SensitiveDataCryptoService(properties());
        byte[] plainText = "SELECT * FROM orders WHERE query_date = '2026-05-09'"
            .getBytes(StandardCharsets.UTF_8);

        byte[] cipherText = service.encryptBytes(plainText);

        assertTrue(cipherText.length > plainText.length);
        assertArrayEquals(plainText, service.decryptBytes(cipherText));
    }

    @Test
    void shouldEncryptAndDecryptWithLightweightFallback() {
        SensitiveDataCryptoService service =
            new SensitiveDataCryptoService(properties(), null, true);

        String envelope = service.encrypt("legacy-jdk-8u112-secret");
        byte[] plainText = "SELECT 1".getBytes(StandardCharsets.UTF_8);
        byte[] cipherText = service.encryptBytes(plainText);

        assertTrue(envelope.startsWith("ENC::AES256_GCM::test-key::"));
        assertEquals("legacy-jdk-8u112-secret", service.decrypt(envelope));
        assertArrayEquals(plainText, service.decryptBytes(cipherText));
    }

    private SensitiveDataCryptoProperties properties() {
        SensitiveDataCryptoProperties properties = new SensitiveDataCryptoProperties();
        properties.setAlgorithm("AES256_GCM");
        properties.setKeyId("test-key");
        properties.setBase64Key(TEST_BASE64_KEY);
        return properties;
    }
}
