package com.company.sqloptimization.application.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestUtils() {
    }

    static String readValue(String json, String jsonPath) throws Exception {
        JsonNode current = OBJECT_MAPPER.readTree(json);
        String[] parts = jsonPath.replace("$.", "").split("\\.");
        for (String part : parts) {
            current = current.get(part);
        }
        return current.asText();
    }
}
