package com.jnu.ticketapi.common.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class CacheLoggingAspect {

    @Around("@annotation(cacheable)")
    public Object logCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable)
            throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String cacheName = String.join(", ", cacheable.value());
        Object result = joinPoint.proceed();
        log.debug(
                "[CACHE] ANNOTATION(@Cacheable) / METHODNAME({}) / CACHENAME({}) / RESULT({})",
                methodName,
                cacheName,
                result);
        return result;
    }

    @Around("@annotation(cachePut)")
    public Object logCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String cacheName = String.join(", ", cachePut.value());
        Object result = joinPoint.proceed();
        log.debug(
                "[CACHE] ANNOTATION(@CachePut) / METHODNAME({}) / CACHENAME({}) / RESULT({})",
                methodName,
                cacheName,
                result);
        return result;
    }

    @Around("@annotation(cacheEvict)")
    public Object logCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict)
            throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String cacheName = String.join(", ", cacheEvict.value());
        Object result = joinPoint.proceed();
        log.debug(
                "[CACHE] ANNOTATION(@CacheEvict) / METHODNAME({}) / CACHENAME({}) / RESULT({})",
                methodName,
                cacheName,
                result);
        return result;
    }
}
