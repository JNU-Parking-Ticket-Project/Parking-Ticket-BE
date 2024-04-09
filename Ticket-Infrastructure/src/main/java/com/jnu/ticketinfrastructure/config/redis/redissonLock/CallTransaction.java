package com.jnu.ticketinfrastructure.config.redis.redissonLock;


import org.aspectj.lang.ProceedingJoinPoint;

public interface CallTransaction {
    Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable;
}
