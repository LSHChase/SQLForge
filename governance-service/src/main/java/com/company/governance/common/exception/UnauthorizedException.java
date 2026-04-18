package com.company.governance.common.exception;

import com.company.common.constants.ErrorCodeConstants;
import com.company.common.exception.BizException;

public class UnauthorizedException extends BizException {

    public UnauthorizedException(String message) {
        super(ErrorCodeConstants.SYSTEM_UNAUTHORIZED, message);
    }
}
