package com.jnu.ticketdomain.common.aop.redissonLock;


import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@RequiredArgsConstructor
public class RedissonLockAop {

    private final Logger log = LoggerFactory.getLogger(RedissonLock.class);

    private final RedissonClient redissonClient;

    @Around("@annotation(RedissonLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        RedissonLock distributedLock =
                ((MethodSignature) joinPoint.getSignature())
                        .getMethod()
                        .getAnnotation(RedissonLock.class);
        log.debug(
                joinPoint.getSignature().getName()
                        + " 에서 LOCK("
                        + distributedLock.LockName()
                        + ") 획득 시도");
        RLock lock = redissonClient.getLock(distributedLock.LockName());
        boolean isLocked =
                lock.tryLock(
                        distributedLock.waitTime(),
                        distributedLock.leaseTime(),
                        distributedLock.timeUnit());
        try {
            if (!isLocked) {
                throw new IllegalStateException("[" + distributedLock.LockName() + "] lock 획득 실패");
            }
            log.debug(
                    joinPoint.getSignature().getName()
                            + " 에서 LOCK("
                            + distributedLock.LockName()
                            + ") 획득");
            return joinPoint.proceed();
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.debug(
                    joinPoint.getSignature().getName()
                            + " 에서 LOCK("
                            + distributedLock.LockName()
                            + ") 반납");
        }
    }
}
