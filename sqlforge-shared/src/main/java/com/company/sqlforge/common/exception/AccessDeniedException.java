package com.company.sqlforge.common.exception;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import org.springframework.http.HttpStatus;

public class AccessDeniedException extends BizException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(ErrorCodeConstants.SYSTEM_ACCESS_DENIED, HttpStatus.FORBIDDEN, message);
    }
}
