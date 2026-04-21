package com.company.queryexecution.application.controller.vo;

public class QueryErrorDetailVO {

    private final int code;
    private final String message;
    private final String suggestedAction;
    private final boolean retryable;

    public QueryErrorDetailVO(int code, String message, String suggestedAction, boolean retryable) {
        this.code = code;
        this.message = message;
        this.suggestedAction = suggestedAction;
        this.retryable = retryable;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
