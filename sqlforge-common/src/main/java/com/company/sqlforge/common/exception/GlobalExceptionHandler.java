package com.company.sqlforge.common.exception;

import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex,
                                                                     HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(buildErrorResponse(ex.getCode(), ex.getMessage(), request));
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ErrorResponse> handleBizException(BizException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getHttpStatus())
            .body(buildErrorResponse(ex.getCode(), ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                   HttpServletRequest request) {
        String message = ex.getBindingResult().getAllErrors().isEmpty()
            ? "Validation failed"
            : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(buildErrorResponse(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, message, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorResponse(ErrorCodeConstants.SYSTEM_UNKNOWN_ERROR, "Internal server error", request));
    }

    private ErrorResponse buildErrorResponse(int code, String message, HttpServletRequest request) {
        return new ErrorResponse(
            code,
            message,
            request.getRequestURI(),
            resolveContextValue(request, RequestHeaderConstants.REQUEST_ID, RequestContext.getRequestId()),
            resolveContextValue(request, RequestHeaderConstants.TRACE_ID, RequestContext.getTraceId())
        );
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
