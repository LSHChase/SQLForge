package com.company.sqloptimization.application.controller.vo;

public class RewriteActivationEligibilityReasonVO {

    private String code;
    private String message;
    private Boolean blocking;
    private String field;
    private String evidenceRef;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getBlocking() { return blocking; }
    public void setBlocking(Boolean blocking) { this.blocking = blocking; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }
}
