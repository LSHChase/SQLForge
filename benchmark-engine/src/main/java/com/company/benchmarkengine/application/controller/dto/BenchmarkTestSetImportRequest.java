package com.company.benchmarkengine.application.controller.dto;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFileType;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BenchmarkTestSetImportRequest {

    @NotNull(message = "fileType is required")
    private BenchmarkTestSetFileType fileType;

    @NotBlank(message = "fileName is required")
    @Size(max = 255, message = "fileName exceeds 255 characters")
    private String fileName;

    @NotBlank(message = "contentBase64 is required")
    private String contentBase64;

    @Size(max = 32, message = "charset exceeds 32 characters")
    private String charset;

    @Size(max = 1, message = "delimiter must be a single character")
    private String delimiter;

    @Valid
    @NotEmpty(message = "fieldMappings are required")
    private List<BenchmarkTestSetFieldMappingDTO> fieldMappings;

    public BenchmarkTestSetFileType getFileType() {
        return fileType;
    }

    public void setFileType(BenchmarkTestSetFileType fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public List<BenchmarkTestSetFieldMappingDTO> getFieldMappings() {
        return fieldMappings;
    }

    public void setFieldMappings(List<BenchmarkTestSetFieldMappingDTO> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    @AssertTrue(message = "sqlText field mapping is required for batch-import benchmark test sets")
    public boolean isSqlTextFieldMapped() {
        if (fieldMappings == null) {
            return false;
        }
        for (BenchmarkTestSetFieldMappingDTO item : fieldMappings) {
            if (item != null && item.getField() == com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetField.SQL_TEXT) {
                return true;
            }
        }
        return false;
    }
}
