package com.company.governance.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

final class GovernanceAccelerationPlanTraceSupport {

    private GovernanceAccelerationPlanTraceSupport() {
    }

    static boolean isTerminalStatus(String status) {
        return "ACTIVE".equals(status)
            || "PAUSED".equals(status)
            || "ACTIVATE_FAILED".equals(status)
            || "PAUSE_FAILED".equals(status);
    }

    static String requireContext(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "受保护请求上下文缺失：" + fieldName
            );
        }
        return value;
    }

    static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return value.trim();
    }

    static String sanitizeKey(String value) {
        return requireText(value, "planId").replaceAll("[^A-Za-z0-9]+", "-");
    }

    static Instant parseInstant(String value, Instant fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return Instant.parse(value.trim());
    }

    static LocalDateTime toDatabaseTime(Instant instant) {
        return DateUtils.toBeijingDateTime(instant);
    }
}
