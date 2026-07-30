package com.jnu.ticketapi.api.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventStatusException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserRole;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrationResultPersistenceServiceTest {

    @Mock private RegistrationAdaptor registrationAdaptor;
    @Mock private UserAdaptor userAdaptor;
    @Mock private EmailOutboxAdaptor emailOutboxAdaptor;
    @Mock private SectorAdaptor sectorAdaptor;

    private RegistrationResultPersistenceService service;

    @BeforeEach
    void setUp() {
        service =
                new RegistrationResultPersistenceService(
                        registrationAdaptor, userAdaptor, emailOutboxAdaptor, sectorAdaptor);
    }

    @Test
    @DisplayName("Redis 예약 결과를 DB에 저장하고 구간 체크포인트와 Outbox를 함께 갱신한다")
    void persistsRedisDecisionAndCheckpoint() {
        Registration registration = registration();
        User user = user();
        Sector sector = sector();
        givenLockedEntities(user, sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(registrationAdaptor.findSavedBySectorIdAndPosition(20L, 1))
                .thenReturn(Optional.empty());
        when(registrationAdaptor.saveAndFlush(registration)).thenReturn(registration);

        StockReservationResult result =
                service.persistRedisReservation(
                        registration,
                        30L,
                        20L,
                        10L,
                        StockReservationResult.reserved(1, UserStatus.SUCCESS, -2, 2),
                        1_234L);

        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(result.getRemainingAmount()).isEqualTo(2);
        assertThat(registration.isSaved()).isTrue();
        assertThat(registration.getSavedAt()).isEqualTo(1_234L);
        assertThat(registration.getResultStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(sector.getRemainingAmount()).isEqualTo(2);
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("Redis 장애 fallback은 DB row lock 안에서 다음 순번과 재고를 확정한다")
    void persistsWithDatabaseFallback() {
        Registration registration = registration();
        User user = user();
        Sector sector = sector();
        givenLockedEntities(user, sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(registrationAdaptor.saveAndFlush(registration)).thenReturn(registration);

        StockReservationResult result =
                service.persistWithDatabaseFallback(registration, 30L, 20L, 10L, 2_345L);

        assertThat(result.getPosition()).isEqualTo(1);
        assertThat(result.getResultStatus()).isEqualTo(UserStatus.SUCCESS);
        assertThat(result.getRemainingAmount()).isEqualTo(2);
        assertThat(sector.getRemainingAmount()).isEqualTo(2);
        verify(emailOutboxAdaptor).saveRegistrationResultIfAbsent(registration);
    }

    @Test
    @DisplayName("이미 DB에 확정된 이메일은 재고를 다시 차감하거나 Outbox를 중복 생성하지 않는다")
    void returnsExistingRegistrationIdempotently() {
        Registration incoming = registration();
        Registration existing = registration();
        existing.finalSave(2, UserStatus.SUCCESS, -2);
        User user = user();
        Sector sector = sector();
        givenLockedEntities(user, sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.of(existing));

        StockReservationResult result =
                service.persistWithDatabaseFallback(incoming, 30L, 20L, 10L, 3_456L);

        assertThat(result.getPosition()).isEqualTo(2);
        assertThat(sector.getRemainingAmount()).isEqualTo(3);
        verify(registrationAdaptor, never()).saveAndFlush(incoming);
        verify(emailOutboxAdaptor, never()).saveRegistrationResultIfAbsent(incoming);
    }

    @Test
    @DisplayName("이미 다른 신청이 사용한 Redis position은 DB에 덮어쓰지 않는다")
    void rejectsPositionOwnedByAnotherRegistration() {
        Registration registration = registration();
        Registration positionOwner = registration();
        User user = user();
        Sector sector = sector();
        givenLockedEntities(user, sector);
        when(registrationAdaptor.findSavedByEmailAndEventId("student@jnu.ac.kr", 10L))
                .thenReturn(Optional.empty());
        when(registrationAdaptor.findSavedBySectorIdAndPosition(20L, 1))
                .thenReturn(Optional.of(positionOwner));

        assertThatThrownBy(
                        () ->
                                service.persistRedisReservation(
                                        registration,
                                        30L,
                                        20L,
                                        10L,
                                        StockReservationResult.reserved(
                                                1, UserStatus.SUCCESS, -2, 2),
                                        1L))
                .isInstanceOf(IllegalStateException.class);

        verify(registrationAdaptor, never()).saveAndFlush(registration);
    }

    @Test
    @DisplayName("Redis 복구 스냅샷은 DB position과 체크포인트 중 더 진행된 값을 보존한다")
    void preparesRecoverySnapshotWithoutRewindingPosition() {
        Sector sector = sector();
        Registration saved = registration();
        saved.finalSave(2, UserStatus.SUCCESS, -2);
        saved.setSector(sector);
        when(registrationAdaptor.findByIsDeletedFalseAndIsSavedTrue(10L))
                .thenReturn(List.of(saved));
        when(sectorAdaptor.findByEventId(10L)).thenReturn(List.of(sector));
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);

        EventStockRecoverySnapshot snapshot = service.prepareRecoverySnapshot(10L);

        assertThat(snapshot.sectors()).containsExactly(sector);
        assertThat(snapshot.reservedEmails()).containsExactly("student@jnu.ac.kr");
        assertThat(sector.getRemainingAmount()).isEqualTo(1);
    }

    @Test
    @DisplayName("CLOSED 이벤트는 Redis 접수 상태를 다시 만들지 않는다")
    void rejectsRecoverySnapshotForClosedEvent() {
        Sector sector = sector();
        ReflectionTestUtils.setField(sector.getEvent(), "eventStatus", EventStatus.CLOSED);
        when(registrationAdaptor.findByIsDeletedFalseAndIsSavedTrue(10L)).thenReturn(List.of());
        when(sectorAdaptor.findByEventId(10L)).thenReturn(List.of(sector));
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);

        assertThatThrownBy(() -> service.prepareRecoverySnapshot(10L))
                .isSameAs(NotOpenEventStatusException.EXCEPTION);
    }

    private void givenLockedEntities(User user, Sector sector) {
        when(userAdaptor.findByIdForUpdate(30L)).thenReturn(user);
        when(sectorAdaptor.findByIdForUpdate(20L)).thenReturn(sector);
    }

    private Event event() {
        Event event = Event.builder().title("주차권").sector(java.util.List.of()).build();
        ReflectionTestUtils.setField(event, "id", 10L);
        ReflectionTestUtils.setField(event, "eventStatus", EventStatus.OPEN);
        return event;
    }

    private Sector sector() {
        Sector sector =
                Sector.builder()
                        .sectorNumber("1구간")
                        .name("공과대학")
                        .sectorCapacity(2)
                        .reserve(1)
                        .build();
        ReflectionTestUtils.setField(sector, "id", 20L);
        sector.setEvent(event());
        return sector;
    }

    private User user() {
        return User.builder()
                .email("student@jnu.ac.kr")
                .pwd("encoded-password")
                .userRole(UserRole.USER)
                .build();
    }

    private Registration registration() {
        return Registration.builder()
                .email("student@jnu.ac.kr")
                .name("학생")
                .studentNum("20240001")
                .affiliation("공과대학")
                .department("컴퓨터공학과")
                .carNum("12가3456")
                .isLight(false)
                .phoneNum("010-0000-0000")
                .createdAt(LocalDateTime.of(2026, 7, 31, 10, 0))
                .isSaved(false)
                .eventId(10L)
                .build();
    }
}
