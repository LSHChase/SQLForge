package com.company.sqlforge.common.security;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SensitiveDataCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String AES = "AES";
    private static final String ENVELOPE_PREFIX = "ENC::";
    private static final int AES_256_KEY_LENGTH = 32;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SensitiveDataCryptoProperties properties;
    private final SecureRandom secureRandom;

    public SensitiveDataCryptoService(SensitiveDataCryptoProperties properties) {
        this.properties = properties;
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        byte[] cipherBytes = encryptInternal(plainText.getBytes(StandardCharsets.UTF_8), iv);
        return ENVELOPE_PREFIX
            + getAlgorithm()
            + "::"
            + getKeyId()
            + "::"
            + Base64.getEncoder().encodeToString(iv)
            + "::"
            + Base64.getEncoder().encodeToString(cipherBytes);
    }

    public String decrypt(String envelope) {
        if (!StringUtils.hasText(envelope)) {
            return envelope;
        }
        ParsedEnvelope parsedEnvelope = parseEnvelope(envelope);
        byte[] plainBytes = decryptInternal(parsedEnvelope.getCipherBytes(), parsedEnvelope.getIv());
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    public byte[] encryptBytes(byte[] plainBytes) {
        if (plainBytes == null) {
            return null;
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        byte[] cipherBytes = encryptInternal(plainBytes, iv);
        byte[] result = new byte[GCM_IV_LENGTH + cipherBytes.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(cipherBytes, 0, result, GCM_IV_LENGTH, cipherBytes.length);
        return result;
    }

    public byte[] decryptBytes(byte[] cipherBytes) {
        if (cipherBytes == null) {
            return null;
        }
        if (cipherBytes.length <= GCM_IV_LENGTH) {
            throw invalidCryptoConfiguration("Cipher bytes are shorter than the required IV length");
        }
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] payload = new byte[cipherBytes.length - GCM_IV_LENGTH];
        System.arraycopy(cipherBytes, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(cipherBytes, GCM_IV_LENGTH, payload, 0, payload.length);
        return decryptInternal(payload, iv);
    }

    public String getAlgorithm() {
        return properties.getAlgorithm();
    }

    public String getKeyId() {
        return properties.getKeyId();
    }

    private byte[] encryptInternal(byte[] plainBytes, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(plainBytes);
        } catch (GeneralSecurityException ex) {
            throw invalidCryptoConfiguration("Failed to encrypt sensitive data", ex);
        }
    }

    private byte[] decryptInternal(byte[] cipherBytes, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(cipherBytes);
        } catch (GeneralSecurityException ex) {
            throw invalidCryptoConfiguration("Failed to decrypt sensitive data", ex);
        }
    }

    private SecretKeySpec secretKey() {
        if (!StringUtils.hasText(properties.getBase64Key())) {
            throw invalidCryptoConfiguration("Missing base64Key for sensitive data encryption");
        }
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(properties.getBase64Key());
        } catch (IllegalArgumentException ex) {
            throw invalidCryptoConfiguration("Invalid base64Key for sensitive data encryption", ex);
        }
        if (decodedKey.length != AES_256_KEY_LENGTH) {
            throw invalidCryptoConfiguration("Sensitive data encryption key must decode to 32 bytes");
        }
        return new SecretKeySpec(decodedKey, AES);
    }

    private ParsedEnvelope parseEnvelope(String envelope) {
        String[] segments = envelope.split("::");
        if (segments.length != 5 || !"ENC".equals(segments[0])) {
            throw invalidCryptoConfiguration("Invalid sensitive data envelope");
        }
        if (!getAlgorithm().equals(segments[1])) {
            throw invalidCryptoConfiguration("Sensitive data envelope algorithm does not match active configuration");
        }
        if (!getKeyId().equals(segments[2])) {
            throw invalidCryptoConfiguration("Sensitive data envelope key id does not match active configuration");
        }
        try {
            return new ParsedEnvelope(Base64.getDecoder().decode(segments[3]), Base64.getDecoder().decode(segments[4]));
        } catch (IllegalArgumentException ex) {
            throw invalidCryptoConfiguration("Invalid base64 payload in sensitive data envelope", ex);
        }
    }

    private BizException invalidCryptoConfiguration(String message) {
        return invalidCryptoConfiguration(message, null);
    }

    private BizException invalidCryptoConfiguration(String message, Throwable cause) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
            HttpStatus.INTERNAL_SERVER_ERROR,
            message,
            cause
        );
    }

    private static final class ParsedEnvelope {

        private final byte[] iv;
        private final byte[] cipherBytes;

        private ParsedEnvelope(byte[] iv, byte[] cipherBytes) {
            this.iv = iv;
            this.cipherBytes = cipherBytes;
        }

        private byte[] getIv() {
            return iv;
        }

        private byte[] getCipherBytes() {
            return cipherBytes;
        }
    }
}
