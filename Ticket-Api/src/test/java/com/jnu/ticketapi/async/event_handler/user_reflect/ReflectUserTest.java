package com.jnu.ticketapi.async.event_handler.user_reflect;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.user.handler.UserReflectStatusEventHandler;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.event.UserReflectStatusEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class ReflectUserTest {

    @MockBean private RegistrationAdaptor registrationAdaptor;

    @MockBean private UserAdaptor userAdaptor;

    @SpyBean private UserReflectStatusEventHandler userReflectStatusEventHandler;

    private Registration registration;
    private Sector sector;
    private User user;

    @BeforeEach
    void setUp() {
        registration = Mockito.mock(Registration.class);
        sector = Mockito.mock(Sector.class);
        user = Mockito.mock(User.class);

        when(registration.getId()).thenReturn(1L);
        when(sector.getId()).thenReturn(1L);
    }

    @Test
    @DisplayName("유저 상태 반영 - 성공")
    void testHandle_Success() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        when(sector.getInitSectorCapacity()).thenReturn(100); // 합격 용량
        when(sector.getIssueAmount()).thenReturn(200); // 총 용량
        when(registrationAdaptor.findPositionBySavedAt(1L, 1L)).thenReturn(50); // 몇 번째 데이터인지 확인 (50번째)
        when(userAdaptor.findById(1L))
                .thenAnswer(
                        invocation -> {
                            latch.countDown();
                            return user;
                        });

        UserReflectStatusEvent event = UserReflectStatusEvent.of(1L, registration, sector);

        // When
        userReflectStatusEventHandler.handle(event);

        latch.await(5, TimeUnit.SECONDS);

        // Then
        verify(userAdaptor).findById(1L); // 유저 조회
        verify(registrationAdaptor).findPositionBySavedAt(1L, 1L); // Position 조회
        verify(user).success(); // 유저 성공 상태
        verify(userAdaptor).save(user); // 유저 저장
    }

    @Test
    @DisplayName("유저 상태 반영 - 대기")
    void testHandle_Prepare() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        when(sector.getInitSectorCapacity()).thenReturn(100); // 합격 용량
        when(sector.getIssueAmount()).thenReturn(200); // 총 용량
        when(registrationAdaptor.findPositionBySavedAt(1L, 1L)).thenReturn(150); // 몇 번째 데이터인지 확인 (150번째)
        when(userAdaptor.findById(1L))
                .thenAnswer(
                        invocation -> {
                            latch.countDown();
                            return user;
                        });

        UserReflectStatusEvent event = UserReflectStatusEvent.of(1L, registration, sector);

        // When
        userReflectStatusEventHandler.handle(event);

        latch.await(5, TimeUnit.SECONDS);

        // Then
        verify(userAdaptor).findById(1L); // 유저 조회
        verify(registrationAdaptor).findPositionBySavedAt(1L, 1L); // Position 조회
        verify(user).prepare(50); // 유저가 대기 상태로 전환 (150 - 100)
        verify(userAdaptor).save(user); // 상태 저장
    }

    @Test
    @DisplayName("유저 상태 반영 - 불합격")
    void testHandle_Fail() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        when(sector.getInitSectorCapacity()).thenReturn(100); // 합격 용량
        when(sector.getIssueAmount()).thenReturn(200); // 총 용량
        when(registrationAdaptor.findPositionBySavedAt(1L, 1L)).thenReturn(201); // // 몇 번째 데이터인지 확인 (201번째)
        when(userAdaptor.findById(1L))
                .thenAnswer(
                        invocation -> {
                            latch.countDown();
                            return user;
                        });

        UserReflectStatusEvent event = UserReflectStatusEvent.of(1L, registration, sector);

        // When
        userReflectStatusEventHandler.handle(event);

        latch.await(5, TimeUnit.SECONDS);

        // Then
        verify(userAdaptor).findById(1L); // 유저 조회
        verify(registrationAdaptor).findPositionBySavedAt(1L, 1L); // Position 조회
        verify(user).fail(); // 유저가 실패 상태로 전환
        verify(userAdaptor).save(user); // 상태 저장
    }
}
