package com.company.sqlforge.common.exception;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BizException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(ErrorCodeConstants.SYSTEM_UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }
}
