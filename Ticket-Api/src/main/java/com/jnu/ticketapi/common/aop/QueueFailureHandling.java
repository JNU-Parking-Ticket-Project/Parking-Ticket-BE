package com.jnu.ticketapi.common.aop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueueFailureHandling {
    /** 최대 재시도 횟수 */
    int maxRetries() default 3;

    /** 즉시 제거 대상 예외 클래스들 (재시도하지 않고 바로 큐에서 제거) 정상적인 비즈니스 로직으로 인한 예외들 */
    Class<? extends Throwable>[] removeImmediatelyExceptions();

    /** 실패 카운트 키 생성 전략 */
    String failCountKeyPrefix() default "fail_count:";
}
