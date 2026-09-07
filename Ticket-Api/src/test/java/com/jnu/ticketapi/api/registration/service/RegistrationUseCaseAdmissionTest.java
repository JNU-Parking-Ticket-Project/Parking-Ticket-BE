package com.jnu.ticketapi.api.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketapi.api.captcha.service.ValidateCaptchaUseCase;
import com.jnu.ticketapi.api.event.service.EventWithDrawUseCase;
import com.jnu.ticketapi.api.event.service.RegistrationAdmissionJournalService;
import com.jnu.ticketapi.api.registration.model.request.FinalSaveRequest;
import com.jnu.ticketapi.api.registration.model.response.FinalSaveResponse;
import com.jnu.ticketapi.application.helper.Converter;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.captcha.exception.NotFoundCaptchaLogException;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationUseCaseAdmissionTest {

    private static final long EVENT_ID = 10L;
    private static final long SECTOR_ID = 20L;
    private static final long USER_ID = 30L;
    private static final long CAPTCHA_LOG_ID = 40L;
    private static final String EMAIL = "student@jnu.ac.kr";

    @Mock private RegistrationAdaptor registrationAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;
    @Mock private Converter converter;
    @Mock private UserAdaptor userAdaptor;
    @Mock private EventWithDrawUseCase eventWithDrawUseCase;
    @Mock private ValidateCaptchaUseCase validateCaptchaUseCase;
    @Mock private RegistrationAdmissionJournalService registrationAdmissionJournalService;
    @Mock private EventAdaptor eventAdaptor;
    @Mock private Event event;
    @Mock private Sector sector;
    @Mock private User user;

    private RegistrationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase =
                new RegistrationUseCase(
                        registrationAdaptor,
                        sectorAdaptor,
                        converter,
                        userAdaptor,
                        eventWithDrawUseCase,
                        validateCaptchaUseCase,
                        registrationAdmissionJournalService,
                        eventAdaptor);
        ReflectionTestUtils.setField(useCase, "ableRedis", false);
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new TestingAuthenticationToken(
                                String.valueOf(USER_ID), "password", "ROLE_ADMIN"));

        lenient().when(sectorAdaptor.findByIdAndEventId(SECTOR_ID, EVENT_ID)).thenReturn(sector);
        lenient().when(eventAdaptor.findById(EVENT_ID)).thenReturn(event);
        lenient().when(event.getPublish()).thenReturn(true);
        lenient().when(event.getId()).thenReturn(EVENT_ID);
        lenient().when(event.getEventStatus()).thenReturn(EventStatus.OPEN);
        lenient()
                .when(event.getDateTimePeriod())
                .thenReturn(
                        DateTimePeriod.between(
                                LocalDateTime.now().minusMinutes(1),
                                LocalDateTime.now().plusMinutes(10)));
        lenient().when(userAdaptor.findById(USER_ID)).thenReturn(user);
        lenient().when(user.getId()).thenReturn(USER_ID);
        lenient()
                .when(
                        eventWithDrawUseCase.findExistingAdmission(
                                any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID)))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("admission이 실패하면 검증한 캡차를 소비하지 않는다")
    void doesNotConsumeCaptchaWhenAdmissionFails() {
        when(validateCaptchaUseCase.validateWithoutConsume("code", "answer"))
                .thenReturn(CAPTCHA_LOG_ID);
        when(registrationAdaptor.findByEmailAndIsSaved(EMAIL, false)).thenReturn(List.of());
        when(eventWithDrawUseCase.issueEvent(
                        any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID)))
                .thenThrow(new IllegalStateException("admission failed"));

        assertThatThrownBy(() -> useCase.finalSave(request(), EMAIL, EVENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("admission failed");

        verify(validateCaptchaUseCase, never()).consume(any());
    }

    @Test
    @DisplayName("durable admission이 성공한 뒤 캡차를 소비한다")
    void consumesCaptchaAfterAdmissionSucceeds() {
        when(validateCaptchaUseCase.validateWithoutConsume("code", "answer"))
                .thenReturn(CAPTCHA_LOG_ID);
        when(registrationAdaptor.findByEmailAndIsSaved(EMAIL, false)).thenReturn(List.of());
        when(eventWithDrawUseCase.issueEvent(
                        any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID)))
                .thenReturn(StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 299));

        FinalSaveResponse response = useCase.finalSave(request(), EMAIL, EVENT_ID);

        assertThat(response.email()).isEqualTo(EMAIL);
        verify(validateCaptchaUseCase).consume(CAPTCHA_LOG_ID);
    }

    @Test
    @DisplayName("응답 유실 재시도는 캡차와 중복 검사 전에 기존 확정 결과를 복원한다")
    void restoresCompletedAdmissionBeforeCaptchaAndDuplicateChecks() {
        when(eventWithDrawUseCase.findExistingAdmission(
                        any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID)))
                .thenReturn(Optional.of(existingAdmission(true)));

        FinalSaveResponse response = useCase.finalSave(request(), EMAIL, EVENT_ID);

        assertThat(response.email()).isEqualTo(EMAIL);
        verify(validateCaptchaUseCase, never()).validateWithoutConsume(any(), any());
        verify(registrationAdaptor, never()).existsByEmailAndIsSavedTrue(any(), any());
        verify(event, never()).getPublish();
        verify(event, never()).getEventStatus();
    }

    @Test
    @DisplayName("RECEIVED 저널 재시도는 소비된 캡차를 다시 요구하지 않고 admission을 재개한다")
    void resumesReceivedAdmissionWithoutRevalidatingCaptcha() {
        when(eventWithDrawUseCase.findExistingAdmission(
                        any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID)))
                .thenReturn(Optional.of(existingAdmission(false)));

        FinalSaveResponse response = useCase.finalSave(request(), EMAIL, EVENT_ID);

        assertThat(response.email()).isEqualTo(EMAIL);
        verify(validateCaptchaUseCase, never()).validateWithoutConsume(any(), any());
        verify(eventWithDrawUseCase)
                .resumeExistingAdmission(
                        any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID));
    }

    @Test
    @DisplayName("최초 조회 직후 캡차가 소비되면 matching 저널을 재조회해 admission을 재개한다")
    void rechecksJournalWhenCaptchaWasConsumedByConcurrentRequest() {
        when(eventWithDrawUseCase.findExistingAdmission(
                        any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID)))
                .thenReturn(Optional.empty(), Optional.of(existingAdmission(false)));
        when(validateCaptchaUseCase.validateWithoutConsume("code", "answer"))
                .thenThrow(NotFoundCaptchaLogException.EXCEPTION);

        FinalSaveResponse response = useCase.finalSave(request(), EMAIL, EVENT_ID);

        assertThat(response.email()).isEqualTo(EMAIL);
        verify(eventWithDrawUseCase, org.mockito.Mockito.times(2))
                .findExistingAdmission(
                        any(Registration.class), eq(USER_ID), eq(sector), eq(EVENT_ID));
        verify(validateCaptchaUseCase, never()).consume(any());
    }

    private FinalSaveRequest request() {
        return FinalSaveRequest.builder()
                .name("홍길동")
                .studentNum("183027")
                .affiliation("공과대학")
                .department("컴퓨터정보통신공학과")
                .carNum("12가3456")
                .isLight(true)
                .phoneNum("010-1111-2222")
                .selectSectorId(SECTOR_ID)
                .captchaCode("code")
                .captchaAnswer("answer")
                .build();
    }

    private RegistrationAdmissionJournalService.ExistingAdmission existingAdmission(
            boolean accepted) {
        return new RegistrationAdmissionJournalService.ExistingAdmission(accepted);
    }
}
