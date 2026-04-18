package com.company.common.exception;

import com.company.common.utils.DateUtils;

public class ErrorResponse {

    private final int code;
    private final String message;
    private final String timestamp;
    private final String path;

    public ErrorResponse(int code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
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
}
