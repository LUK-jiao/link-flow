package com.linkflow.gateway.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.dto.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GatewayControllerLogAspect {

    private final ObjectMapper objectMapper;

    @Around("execution(public * com.linkflow.gateway.controller..*(..))")
    public Object logControllerCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        String argsJson = toJson(joinPoint.getArgs());

        long startNanos = System.nanoTime();
        log.info("gateway-call start method={} args={}", method, argsJson);

        try {
            Object result = joinPoint.proceed();
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            String resultCode = extractResultCode(result);
            log.info("gateway-call done method={} costMs={} resultCode={}", method, costMs, resultCode);
            return result;
        } catch (Throwable ex) {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("gateway-call fail method={} costMs={} error={}", method, costMs, ex.getMessage());
            throw ex;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String extractResultCode(Object result) {
        if (result instanceof Result<?> resultWrapper) {
            return String.valueOf(resultWrapper.getCode());
        }
        return "N/A";
    }
}
