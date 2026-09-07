package com.jnu.ticketdomain.domains.registration.domain;


import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketdomain.domains.user.domain.UserStatusConverter;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "registration_admission_journal",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_admission_journal_event_email",
                    columnNames = {"event_id", "email"}),
            @UniqueConstraint(
                    name = "uk_admission_journal_sector_position",
                    columnNames = {"sector_id", "position"}),
            @UniqueConstraint(
                    name = "uk_admission_journal_registration",
                    columnNames = {"registration_id"})
        },
        indexes = {
            @Index(name = "idx_admission_journal_event_state", columnList = "event_id,state,id"),
            @Index(name = "idx_admission_journal_state", columnList = "state,id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistrationAdmissionJournal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "admission_epoch", nullable = false)
    private Long admissionEpoch;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private RegistrationAdmissionState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_source")
    private RegistrationDecisionSource decisionSource;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "position")
    private Integer position;

    @Column(name = "result_status")
    @Convert(converter = UserStatusConverter.class)
    private UserStatus resultStatus;

    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "remaining_amount")
    private Integer remainingAmount;

    @Column(name = "registration_payload", nullable = false, length = 10000)
    private String registrationPayload;

    @Column(name = "payload_version", nullable = false)
    private Integer payloadVersion;

    @Column(name = "received_at", nullable = false)
    private Long receivedAt;

    @Column(name = "decided_at")
    private Long decidedAt;

    @Column(name = "materialized_at")
    private Long materializedAt;

    @Column(name = "registration_id")
    private Long registrationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private RegistrationAdmissionJournal(
            Long eventId,
            Long sectorId,
            Long userId,
            String email,
            Long admissionEpoch,
            String registrationPayload,
            Long receivedAt) {
        this.eventId = eventId;
        this.sectorId = sectorId;
        this.userId = userId;
        this.email = email;
        this.admissionEpoch = admissionEpoch;
        this.state = RegistrationAdmissionState.RECEIVED;
        this.registrationPayload = registrationPayload;
        this.payloadVersion = 1;
        this.receivedAt = receivedAt;
        this.version = 0L;
    }

    public static RegistrationAdmissionJournal received(
            Long eventId,
            Long sectorId,
            Long userId,
            String email,
            Long admissionEpoch,
            String registrationPayload,
            Long receivedAt) {
        if (eventId == null
                || sectorId == null
                || userId == null
                || email == null
                || admissionEpoch == null
                || registrationPayload == null
                || receivedAt == null) {
            throw new IllegalArgumentException("신청 접수 식별자와 복구 payload가 필요합니다.");
        }
        return new RegistrationAdmissionJournal(
                eventId, sectorId, userId, email, admissionEpoch, registrationPayload, receivedAt);
    }

    public void confirm(
            RegistrationDecisionSource source,
            Integer position,
            UserStatus resultStatus,
            Integer sequence,
            Integer remainingAmount,
            Long decidedAt) {
        if (isDecided()) {
            validateDecision(source, position, resultStatus, sequence);
            return;
        }
        if (state != RegistrationAdmissionState.RECEIVED
                || source == null
                || position == null
                || resultStatus == null
                || sequence == null
                || decidedAt == null) {
            throw new IllegalStateException("접수 상태의 신청에만 확정 결과를 기록할 수 있습니다.");
        }
        this.state = RegistrationAdmissionState.DECIDED;
        this.decisionSource = source;
        this.decisionReason = "RESERVED";
        this.position = position;
        this.resultStatus = resultStatus;
        this.sequence = sequence;
        this.remainingAmount = remainingAmount;
        this.decidedAt = decidedAt;
    }

    public void reject(String reason, Integer remainingAmount, Long decidedAt) {
        if (state == RegistrationAdmissionState.REJECTED) {
            return;
        }
        if (state != RegistrationAdmissionState.RECEIVED || reason == null || decidedAt == null) {
            throw new IllegalStateException("접수 상태의 신청에만 거절 결과를 기록할 수 있습니다.");
        }
        this.state = RegistrationAdmissionState.REJECTED;
        this.decisionReason = reason;
        this.remainingAmount = remainingAmount;
        this.decidedAt = decidedAt;
    }

    public void markMaterialized(Long registrationId, Long materializedAt) {
        if (state == RegistrationAdmissionState.MATERIALIZED) {
            return;
        }
        if (state != RegistrationAdmissionState.DECIDED
                || registrationId == null
                || materializedAt == null) {
            throw new IllegalStateException("확정된 신청만 본 저장 완료로 전환할 수 있습니다.");
        }
        this.registrationId = registrationId;
        this.materializedAt = materializedAt;
        this.state = RegistrationAdmissionState.MATERIALIZED;
    }

    public boolean isReceived() {
        return state == RegistrationAdmissionState.RECEIVED;
    }

    public boolean isDecided() {
        return state == RegistrationAdmissionState.DECIDED
                || state == RegistrationAdmissionState.MATERIALIZED;
    }

    public boolean isMaterialized() {
        return state == RegistrationAdmissionState.MATERIALIZED;
    }

    public boolean isRejected() {
        return state == RegistrationAdmissionState.REJECTED;
    }

    public boolean matchesDecision(Integer position, UserStatus resultStatus, Integer sequence) {
        return Objects.equals(this.position, position)
                && this.resultStatus == resultStatus
                && Objects.equals(this.sequence, sequence);
    }

    private void validateDecision(
            RegistrationDecisionSource source,
            Integer position,
            UserStatus resultStatus,
            Integer sequence) {
        if (this.decisionSource != source || !matchesDecision(position, resultStatus, sequence)) {
            throw new IllegalStateException("이미 기록된 신청 결정과 다른 결과입니다. journalId=" + id);
        }
    }
}
