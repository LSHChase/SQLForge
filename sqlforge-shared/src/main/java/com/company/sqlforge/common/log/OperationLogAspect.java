package com.company.sqlforge.common.log;

import com.company.sqlforge.common.context.RequestContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogAspect.class);

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        String entity = operationLog.entity().isEmpty() ? joinPoint.getSignature().toShortString() : operationLog.entity();
        LOGGER.info("操作日志 operation={} entity={} requestId={} traceId={} status=START",
            operationLog.operation(),
            entity,
            RequestContext.getRequestId(),
            RequestContext.getTraceId());
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            LOGGER.info("操作日志 operation={} entity={} requestId={} traceId={} costMs={} status=SUCCESS",
                operationLog.operation(),
                entity,
                RequestContext.getRequestId(),
                RequestContext.getTraceId(),
                cost);
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            LOGGER.error("操作日志 operation={} entity={} requestId={} traceId={} costMs={} status=FAILED reason={}",
                operationLog.operation(),
                entity,
                RequestContext.getRequestId(),
                RequestContext.getTraceId(),
                cost,
                ex.getMessage());
            throw ex;
        }
    }
}
