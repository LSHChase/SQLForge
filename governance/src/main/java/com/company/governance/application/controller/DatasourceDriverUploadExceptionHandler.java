package com.company.governance.application.controller;

import com.company.governance.config.GovernanceDatasourceDriverProperties;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.ErrorResponse;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice(assignableTypes = DatasourceDriverArtifactController.class)
public class DatasourceDriverUploadExceptionHandler {

    private final GovernanceDatasourceDriverProperties properties;

    public DatasourceDriverUploadExceptionHandler(GovernanceDatasourceDriverProperties properties) {
        this.properties = properties;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
                                                                    HttpServletRequest request) {
        return badRequest(uploadSizeExceededMessage(), request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException ex,
                                                                  HttpServletRequest request) {
        if (containsMaxSwallowSize(ex)) {
            return badRequest(uploadSizeExceededMessage(), request);
        }
        return badRequest("驱动上传请求无效，请确认表单编码和文件内容后重试", request);
    }

    private ResponseEntity<ErrorResponse> badRequest(String message, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                message,
                request.getRequestURI(),
                resolveContextValue(request, RequestHeaderConstants.REQUEST_ID, RequestContext.getRequestId()),
                resolveContextValue(request, RequestHeaderConstants.TRACE_ID, RequestContext.getTraceId())
            ));
    }

    private String uploadSizeExceededMessage() {
        DataSize maxFileSize = properties.getMaxFileSize();
        long maxFileSizeMb = maxFileSize.toBytes() / DataSize.ofMegabytes(1).toBytes();
        return "驱动文件超过大小限制，请将 JDBC 驱动文件控制在 " + maxFileSizeMb + " MB 以内后重试";
    }

    private boolean containsMaxSwallowSize(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("maxswallowsize")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String resolveContextValue(HttpServletRequest request, String headerName, String currentValue) {
        if (currentValue != null) {
            return currentValue;
        }
        Object requestAttribute = request.getAttribute(headerName);
        if (requestAttribute instanceof String) {
            return (String) requestAttribute;
        }
        return request.getHeader(headerName);
    }
}
