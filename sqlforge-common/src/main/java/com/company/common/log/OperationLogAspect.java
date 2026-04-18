package com.company.common.log;

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
        LOGGER.info("operation={} entity={} status=START", operationLog.operation(), entity);
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            LOGGER.info("operation={} entity={} costMs={} status=SUCCESS",
                operationLog.operation(), entity, cost);
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            LOGGER.error("operation={} entity={} costMs={} status=FAILED reason={}",
                operationLog.operation(), entity, cost, ex.getMessage());
            throw ex;
        }
    }
}
