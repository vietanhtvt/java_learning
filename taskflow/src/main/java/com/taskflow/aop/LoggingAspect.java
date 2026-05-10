package com.taskflow.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logs every service method call: class, method, args, execution time, and outcome.
 * Uses @Around so it wraps both success and exception paths.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // Pointcut: all public methods in any class inside com.taskflow.service
    @Pointcut("execution(public * com.taskflow.service.*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className  = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        Object[] args     = joinPoint.getArgs();

        log.debug("→ {}.{}() args={}", className, methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("← {}.{}() completed in {}ms", className, methodName, elapsed);
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("✕ {}.{}() threw {} after {}ms — {}",
                className, methodName, ex.getClass().getSimpleName(), elapsed, ex.getMessage());
            throw ex;
        }
    }
}
