package com.company.sqlforge.common.security;

final class SensitiveDataCryptoEnvelope {

    private final byte[] iv;
    private final byte[] cipherBytes;

    SensitiveDataCryptoEnvelope(byte[] iv, byte[] cipherBytes) {
        this.iv = iv;
        this.cipherBytes = cipherBytes;
    }

    byte[] getIv() {
        return iv;
    }

    byte[] getCipherBytes() {
        return cipherBytes;
    }
}
