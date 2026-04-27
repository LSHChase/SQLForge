package com.company.sqloptimization.application.controller.dto;

import javax.validation.constraints.NotBlank;

public class ParseBatchIngestRequest {

    private String fileName;

    @NotBlank(message = "contentBase64 is required")
    private String contentBase64;

    private String charset;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentBase64() { return contentBase64; }
    public void setContentBase64(String contentBase64) { this.contentBase64 = contentBase64; }
    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }
}
