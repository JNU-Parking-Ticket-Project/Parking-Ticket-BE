package com.jnu.ticketdomain.domains.registration.domain;


import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "registration_result_email_outbox")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Getter
public class RegistrationResultEmail {

    @Id
    @Column(name = "email_id", columnDefinition = "CHAR(26)", nullable = false)
    private String emailId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "registration_result", nullable = false)
    private String registrationResult;

    @Column(name = "registration_sequence", nullable = false)
    private Integer registrationSequence;

    @Column(name = "transfer_status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private TransferStatus transferStatus;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public RegistrationResultEmail(
            String emailId,
            Long eventId,
            String receiverEmail,
            String receiverName,
            String registrationResult,
            Integer registrationSequence) {
        this.emailId = emailId;
        this.eventId = eventId;
        this.receiverEmail = receiverEmail;
        this.receiverName = receiverName;
        this.registrationResult = registrationResult;
        this.registrationSequence = registrationSequence;
        this.transferStatus = TransferStatus.PENDING;
    }

    public void updateEmailTransferResult(boolean transferResult) {
        if (transferResult) {
            this.transferStatus = TransferStatus.SUCCESS;

        } else {
            switch (this.transferStatus) {
                case PENDING:
                    this.transferStatus = TransferStatus.FAILED_1;
                    break;
                case FAILED_1:
                    this.transferStatus = TransferStatus.FAILED_2;
                    break;
                case FAILED_2:
                    this.transferStatus = TransferStatus.FAILED_3;
                    break;
                case FAILED_3:
                default:
                    this.transferStatus = TransferStatus.EXCLUDED;
                    break;
            }
        }
    }
}
