package com.company.sqlforge.common.exception;

import com.company.sqlforge.common.utils.DateUtils;

public class ErrorResponse {

    private final int code;
    private final String message;
    private final String timestamp;
    private final String path;
    private final String requestId;
    private final String traceId;

    public ErrorResponse(int code, String message, String path, String requestId, String traceId) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
        this.traceId = traceId;
        this.timestamp = DateUtils.format(DateUtils.now());
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }
}
