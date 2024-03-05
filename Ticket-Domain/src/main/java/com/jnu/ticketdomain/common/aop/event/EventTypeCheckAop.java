package com.jnu.ticketdomain.common.aop.event;


import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.exception.NotReadyEventStatusException;
import com.jnu.ticketdomain.domains.events.out.EventLoadPort;
import io.vavr.control.Validation;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnExpression("${ableEventLock:true}")
public class EventTypeCheckAop {
    private final EventLoadPort eventLoadPort;

    @Before(value = "@annotation(com.jnu.ticketdomain.common.aop.event.EventTypeCheck)")
    public void eventTypeCheck(JoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        EventTypeCheck eventTypeCheck = method.getAnnotation(EventTypeCheck.class);

        validateEventType(eventTypeCheck.eventType())
                .fold(
                        throwable -> {
                            throw throwable;
                        },
                        validatedEventType -> null);
    }

    Validation<TicketCodeException, Object> validateEventType(EventStatus eventType) {
        Event openEvent = eventLoadPort.findReadyOrOpenEvent().getOrThrow();
        if (openEvent.getEventStatus() != eventType) {
            return Validation.invalid(NotReadyEventStatusException.EXCEPTION);
        }
        return Validation.valid(eventType);
    }
}
