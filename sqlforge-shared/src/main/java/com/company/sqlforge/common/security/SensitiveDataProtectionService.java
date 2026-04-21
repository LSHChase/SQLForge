package com.company.sqlforge.common.security;

import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SensitiveDataProtectionService {

    private static final String MASKED_VALUE = "***";
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile(
        "(?i)((?:password|token|secret|access[-_]?key|secret[-_]?key|api[-_]?key|private[-_]?key|client[-_]?secret)"
            + "[^:=\\s\"']{0,32}\\s*[:=]\\s*[\"']?)([^,\\s\"'&}]+)"
    );
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(Bearer\\s+)([A-Za-z0-9\\-._~+/]+=*)");

    private final SensitiveDataCryptoService sensitiveDataCryptoService;
    private final ObjectMapper objectMapper;

    public SensitiveDataProtectionService(SensitiveDataCryptoService sensitiveDataCryptoService) {
        this.sensitiveDataCryptoService = sensitiveDataCryptoService;
        this.objectMapper = JsonUtils.objectMapper();
    }

    public boolean isSensitiveKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return normalized.contains("password")
            || normalized.contains("token")
            || normalized.contains("secret")
            || normalized.contains("apikey")
            || normalized.contains("accesskey")
            || normalized.contains("privatekey")
            || normalized.contains("clientsecret")
            || normalized.endsWith("key");
    }

    public String encryptSensitiveJson(String content) {
        return protectStructuredContent(content, SensitiveDataProtectionMode.ENCRYPT);
    }

    public String maskSensitiveJson(String content) {
        return protectStructuredContent(content, SensitiveDataProtectionMode.MASK);
    }

    public String maskSensitiveText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String masked = ASSIGNMENT_PATTERN.matcher(text).replaceAll("$1" + MASKED_VALUE);
        return BEARER_PATTERN.matcher(masked).replaceAll("$1" + MASKED_VALUE);
    }

    public String maskDisplayValue(String value) {
        if (!StringUtils.hasText(value)) {
            return MASKED_VALUE;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return MASKED_VALUE;
        }
        return trimmed.substring(0, 2) + MASKED_VALUE + trimmed.substring(trimmed.length() - 2);
    }

    private String protectStructuredContent(String content, SensitiveDataProtectionMode mode) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        try {
            JsonNode protectedNode = protectNode(objectMapper.readTree(content), mode);
            return objectMapper.writeValueAsString(protectedNode);
        } catch (Exception ex) {
            if (mode == SensitiveDataProtectionMode.ENCRYPT) {
                return objectMapper.createObjectNode()
                    .put("_protected", true)
                    .put("algorithm", sensitiveDataCryptoService.getAlgorithm())
                    .put("keyId", sensitiveDataCryptoService.getKeyId())
                    .put("ciphertext", sensitiveDataCryptoService.encrypt(content))
                    .put("masked", maskDisplayValue(content))
                    .toString();
            }
            return maskSensitiveText(content);
        }
    }

    private JsonNode protectNode(JsonNode node, SensitiveDataProtectionMode mode) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode source = (ObjectNode) node;
            ObjectNode target = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveKey(field.getKey())) {
                    target.set(field.getKey(), protectSensitiveNode(field.getValue(), mode));
                } else {
                    target.set(field.getKey(), protectNode(field.getValue(), mode));
                }
            }
            return target;
        }
        if (node.isArray()) {
            ArrayNode target = objectMapper.createArrayNode();
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                target.add(protectNode(elements.next(), mode));
            }
            return target;
        }
        return node;
    }

    private JsonNode protectSensitiveNode(JsonNode node, SensitiveDataProtectionMode mode) {
        if (mode == SensitiveDataProtectionMode.MASK) {
            return TextNode.valueOf(maskPreview(node));
        }
        ObjectNode protectedNode = objectMapper.createObjectNode();
        protectedNode.put("_protected", true);
        protectedNode.put("algorithm", sensitiveDataCryptoService.getAlgorithm());
        protectedNode.put("keyId", sensitiveDataCryptoService.getKeyId());
        protectedNode.put("ciphertext", sensitiveDataCryptoService.encrypt(node.toString()));
        protectedNode.put("masked", maskPreview(node));
        return protectedNode;
    }

    private String maskPreview(JsonNode node) {
        if (node == null || node.isNull()) {
            return MASKED_VALUE;
        }
        if (node.isTextual()) {
            return maskDisplayValue(node.asText());
        }
        return MASKED_VALUE;
    }
}
