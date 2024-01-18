package com.jnu.ticketdomain.common.aop.event;


import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EventStatus 를 체크하는 어노테이션 (OPEN, READY)
 *
 * @see com.jnu.ticketdomain.domains.events.domain.EventStatus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTypeCheck {
    EventStatus eventType();
}
