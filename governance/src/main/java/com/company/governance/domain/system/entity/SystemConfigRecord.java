package com.company.governance.domain.system.entity;

public class SystemConfigRecord {

    private String configKey;
    private String configValue;
    private Boolean sensitiveFlag;
    private String valueCiphertext;
    private String valueMask;
    private String encryptionAlgorithm;
    private String encryptionKeyId;
    private String configType;
    private String description;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public Boolean getSensitiveFlag() {
        return sensitiveFlag;
    }

    public void setSensitiveFlag(Boolean sensitiveFlag) {
        this.sensitiveFlag = sensitiveFlag;
    }

    public String getValueCiphertext() {
        return valueCiphertext;
    }

    public void setValueCiphertext(String valueCiphertext) {
        this.valueCiphertext = valueCiphertext;
    }

    public String getValueMask() {
        return valueMask;
    }

    public void setValueMask(String valueMask) {
        this.valueMask = valueMask;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
