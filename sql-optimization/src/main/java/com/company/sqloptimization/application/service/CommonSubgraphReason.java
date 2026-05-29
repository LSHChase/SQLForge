package com.company.sqloptimization.application.service;

import java.util.LinkedHashMap;
import java.util.Map;

final class CommonSubgraphReason {

    private CommonSubgraphReason() {
    }

    static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }
}
