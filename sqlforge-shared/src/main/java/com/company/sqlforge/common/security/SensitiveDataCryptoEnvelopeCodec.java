package com.company.sqlforge.common.security;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import java.util.Base64;
import org.springframework.http.HttpStatus;

final class SensitiveDataCryptoEnvelopeCodec {

    private SensitiveDataCryptoEnvelopeCodec() {
    }

    static String format(String envelopePrefix,
                         String algorithm,
                         String keyId,
                         byte[] iv,
                         byte[] cipherBytes) {
        return envelopePrefix
            + algorithm
            + "::"
            + keyId
            + "::"
            + Base64.getEncoder().encodeToString(iv)
            + "::"
            + Base64.getEncoder().encodeToString(cipherBytes);
    }

    static SensitiveDataCryptoEnvelope parse(String envelope, String expectedAlgorithm, String expectedKeyId) {
        String[] segments = envelope.split("::");
        if (segments.length != 5 || !"ENC".equals(segments[0])) {
            throw invalidCryptoConfiguration("敏感数据信封无效");
        }
        if (!expectedAlgorithm.equals(segments[1])) {
            throw invalidCryptoConfiguration("敏感数据信封算法与当前配置不匹配");
        }
        if (!expectedKeyId.equals(segments[2])) {
            throw invalidCryptoConfiguration("敏感数据信封 key id 与当前配置不匹配");
        }
        try {
            return new SensitiveDataCryptoEnvelope(
                Base64.getDecoder().decode(segments[3]),
                Base64.getDecoder().decode(segments[4])
            );
        } catch (IllegalArgumentException ex) {
            throw invalidCryptoConfiguration("敏感数据信封中的 base64 载荷无效", ex);
        }
    }

    private static BizException invalidCryptoConfiguration(String message) {
        return invalidCryptoConfiguration(message, null);
    }

    private static BizException invalidCryptoConfiguration(String message, Throwable cause) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_CONFIG_INVALID,
            HttpStatus.INTERNAL_SERVER_ERROR,
            message,
            cause
        );
    }
}
