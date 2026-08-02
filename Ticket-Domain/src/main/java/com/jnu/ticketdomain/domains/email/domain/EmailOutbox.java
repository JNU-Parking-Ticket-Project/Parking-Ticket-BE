package com.jnu.ticketdomain.domains.email.domain;


import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketdomain.domains.user.domain.UserStatusConverter;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "email_outbox",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_email_outbox_registration_id",
                    columnNames = "registration_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class EmailOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "result_status", nullable = false)
    @Convert(converter = UserStatusConverter.class)
    private UserStatus resultStatus;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    private EmailOutbox(
            Long eventId,
            Long registrationId,
            String email,
            String name,
            UserStatus resultStatus,
            Integer sequence) {
        this.eventId = eventId;
        this.registrationId = registrationId;
        this.email = email;
        this.name = name;
        this.resultStatus = resultStatus;
        this.sequence = sequence;
    }

    public static EmailOutbox from(Registration registration) {
        UserStatus resultStatus =
                registration.getResultStatus() != null
                        ? registration.getResultStatus()
                        : registration.getUser().getStatus();
        Integer sequence =
                registration.getSequence() != null
                        ? registration.getSequence()
                        : registration.getUser().getSequence();
        return new EmailOutbox(
                registration.getEventId(),
                registration.getId(),
                registration.getEmail(),
                registration.getName(),
                resultStatus,
                sequence);
    }
}
