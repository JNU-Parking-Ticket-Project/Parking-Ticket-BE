package com.jnu.ticketdomain.domains.events.domain;

import static com.jnu.ticketdomain.domains.events.domain.EventStatus.CALCULATING;
import static com.jnu.ticketdomain.domains.events.domain.EventStatus.CLOSED;
import static com.jnu.ticketdomain.domains.events.domain.EventStatus.OPEN;
import static com.jnu.ticketdomain.domains.events.domain.EventStatus.READY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.jnu.ticketcommon.exception.TicketCodeException;
import com.jnu.ticketdomain.common.domainEvent.Events;
import com.jnu.ticketdomain.common.vo.DateTimePeriod;
import com.jnu.ticketdomain.domains.events.event.CouponExpiredEvent;
import com.jnu.ticketdomain.domains.events.event.EventStatusChangeEvent;
import com.jnu.ticketdomain.domains.events.exception.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Where(clause = "is_deleted = false")
@JsonIgnoreProperties("sector")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(name = "title")
    private String title;

    // 쿠폰 pubsub을 위한 일련번호 -> UUID String 6자리
    @Column(name = "event_code")
    private String eventCode;

    @Embedded private DateTimePeriod dateTimePeriod;

    @Enumerated(EnumType.STRING)
    private EventStatus eventStatus;
    // 쿠폰 발행 가능 기간

    // 구간별 정보
    //    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event", cascade = CascadeType.ALL)
    private List<Sector> sector = new ArrayList<>();

    @Column(name = "publish")
    private Boolean publish;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Builder
    public Event(DateTimePeriod dateTimePeriod, List<Sector> sector) {
        this.eventCode = UUID.randomUUID().toString().substring(0, 6);
        this.dateTimePeriod = dateTimePeriod;
        this.sector = sector;
        this.eventStatus = EventStatus.READY;
        this.publish = false;
        this.isDeleted = false;
    }

    @Builder
    public Event(DateTimePeriod dateTimePeriod, List<Sector> sector, String title) {
        this.eventCode = UUID.randomUUID().toString().substring(0, 6);
        this.dateTimePeriod = dateTimePeriod;
        this.sector = sector;
        this.title = title;
        this.eventStatus = EventStatus.READY;
        this.publish = false;
        this.isDeleted = false;
    }

    @PostPersist
    public void postPersist() {
        Events.raise(CouponExpiredEvent.from(dateTimePeriod));
    }

    public void validateOpenStatus() {
        if (eventStatus == OPEN) throw CannotModifyOpenEventException.EXCEPTION;
    }

    public void validateNotOpenStatus() {
        if (eventStatus != OPEN) throw NotOpenEventPeriodException.EXCEPTION;
    }

    public void setSector(List<Sector> sector) {
        this.sector = sector;
    }

    public void validateIssuePeriod() {
        LocalDateTime nowTime = LocalDateTime.now();
        // 과거 시간을 포함하기로 기획을 변경했습니다.
        // if (dateTimePeriod.contains(nowTime)
        if (dateTimePeriod.getEndAt().isBefore(nowTime)
                || dateTimePeriod.getEndAt().isBefore(dateTimePeriod.getStartAt())) {
            throw InvalidPeriodEventException.EXCEPTION;
        }
    }

    public void validationPublishStatus() {
        if (this.publish) {
            throw CannotUpdatePublishEventException.EXCEPTION;
        }
    }

    public void updateStatus(EventStatus status, TicketCodeException exception) {
        //        if (this.eventStatus == status) throw exception;
        this.eventStatus = status;
        Events.raise(EventStatusChangeEvent.of(this));
    }

    public void isPublish(Boolean publish) {
        this.publish = publish;
    }

    public void open() {
        //        validateOpenStatus();
        updateStatus(OPEN, AlreadyOpenStatusException.EXCEPTION);
    }

    public void calculate() {
        updateStatus(CALCULATING, AlreadyCalculatingStatusException.EXCEPTION);
    }

    public void ready() {
        updateStatus(READY, AlreadyReadyStatusException.EXCEPTION);
    }

    public void close() {
        updateStatus(CLOSED, AlreadyCloseStatusException.EXCEPTION);
    }

    public void updateDateTimePeriod(DateTimePeriod dateTimePeriod) {
        this.dateTimePeriod = dateTimePeriod;
    }

    public void deleteEvent() {
        this.isDeleted = true;
    }

    public void update(String title, DateTimePeriod dateTimePeriod) {
        this.title = title;
        this.dateTimePeriod = dateTimePeriod;
    }
}
