package com.example.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    @Around("execution(* com.example.blog.controller..*(..))")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            log.info("[API] {} completed in {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.error("[API] {} failed in {}ms: {}", method, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }
}
