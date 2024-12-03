package com.jnu.ticketapi.async.event_handler.user_reflect;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jnu.ticketapi.api.user.handler.UserReflectStatusEventHandler;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.event.UserReflectStatusEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class UserReflectStatusEventHandlerTest {

    @Autowired PlatformTransactionManager transactionManager;

    @MockBean private RegistrationAdaptor registrationAdaptor;

    @MockBean private UserAdaptor userAdaptor;

    @Autowired private ApplicationEventPublisher applicationEventPublisher;

    @MockBean private UserReflectStatusEventHandler userReflectStatusEventHandler;

    @Test
    @DisplayName("UserReflectStatusEvent를 처리할 때 handle이 호출되는지 확인")
    void testHandleMethodInvocation() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        Registration registration = Mockito.mock(Registration.class);
        Mockito.when(registration.getId()).thenReturn(1L);

        Sector sector = Mockito.mock(Sector.class);
        Mockito.when(sector.getId()).thenReturn(1L);

        UserReflectStatusEvent event = UserReflectStatusEvent.of(1L, registration, sector);

        // handle 메서드가 호출되면 latch를 countDown
        Mockito.doAnswer(
                        invocation -> {
                            latch.countDown();
                            return null;
                        })
                .when(userReflectStatusEventHandler)
                .handle(Mockito.any(UserReflectStatusEvent.class));

        // When: 이벤트 발행 (Handler가 After Commit 단계에서 실행되므로 TransactionTemplate 사용하여 강제 커밋)
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(
                status -> {
                    applicationEventPublisher.publishEvent(event);
                    status.flush();
                });

        // Then: 5초 안에 handle 메서드가 호출되었는지 확인
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "handle 메서드가 호출되지 않았습니다.");
        Mockito.verify(userReflectStatusEventHandler, Mockito.times(1))
                .handle(Mockito.any(UserReflectStatusEvent.class));
    }
}
