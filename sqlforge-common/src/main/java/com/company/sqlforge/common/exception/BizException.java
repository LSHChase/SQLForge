package com.company.sqlforge.common.exception;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final HttpStatus httpStatus;

    public BizException(int code, String message) {
        this(code, HttpStatus.BAD_REQUEST, message);
    }

    public BizException(int code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BizException(int code, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
