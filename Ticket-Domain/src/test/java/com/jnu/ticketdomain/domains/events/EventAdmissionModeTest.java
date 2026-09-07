package com.jnu.ticketdomain.domains.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.exception.AlreadyOpenStatusException;
import com.jnu.ticketdomain.domains.events.exception.CannotModifyOpenEventException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventAdmissionModeTest {

    @Test
    @DisplayName("DB fallback 전환은 epoch를 한 번만 증가시켜 이전 Redis 요청을 차단한다")
    void fencesPreviousRedisEpochWhenFallbackActivates() {
        Event event = Event.builder().title("주차권").build();
        event.open();
        long redisEpoch = event.getAdmissionEpoch();

        event.activateDatabaseAdmissionFallback();
        long fallbackEpoch = event.getAdmissionEpoch();
        event.activateDatabaseAdmissionFallback();

        assertThat(event.getAdmissionMode()).isEqualTo(EventAdmissionMode.DB_FALLBACK);
        assertThat(fallbackEpoch).isEqualTo(redisEpoch + 1L);
        assertThat(event.getAdmissionEpoch()).isEqualTo(fallbackEpoch);
        assertThat(event.isRedisAdmission(redisEpoch)).isFalse();
    }

    @Test
    @DisplayName("이미 OPEN인 이벤트를 다시 열어 DB fallback fence를 되돌릴 수 없다")
    void doesNotReopenFallbackEvent() {
        Event event = Event.builder().title("주차권").build();
        event.open();
        event.activateDatabaseAdmissionFallback();
        long fallbackEpoch = event.getAdmissionEpoch();

        assertThatThrownBy(event::open).isSameAs(AlreadyOpenStatusException.EXCEPTION);
        assertThat(event.getAdmissionMode()).isEqualTo(EventAdmissionMode.DB_FALLBACK);
        assertThat(event.getAdmissionEpoch()).isEqualTo(fallbackEpoch);
    }

    @Test
    @DisplayName("OPEN 이벤트를 READY로 되돌려 admission fence를 우회할 수 없다")
    void doesNotMoveOpenEventBackToReady() {
        Event event = Event.builder().title("주차권").build();
        event.open();

        assertThatThrownBy(event::ready).isSameAs(CannotModifyOpenEventException.EXCEPTION);
    }
}
