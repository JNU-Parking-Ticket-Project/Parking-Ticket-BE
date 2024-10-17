package com.jnu.ticketdomain.domains.registration.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "registration_result_email_outbox")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@Getter
public class RegistrationResultEmailOutbox {

    @Id
    private String id;

    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "registration_result", nullable = false)
    private String registrationResult;

    @Column(name = "registration_sequence", nullable = false)
    private Integer registrationSequence;

    @Column(name = "transfer_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferStatus transferStatus;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Builder
    public RegistrationResultEmailOutbox(String id, String receiverEmail, String receiverName, String registrationResult, Integer registrationSequence) {
        this.id = id;
        this.receiverEmail = receiverEmail;
        this.receiverName = receiverName;
        this.registrationResult = registrationResult;
        this.registrationSequence = registrationSequence;
    }

    public void setTransferStatus(TransferStatus transferStatus) {
        this.transferStatus = transferStatus;
    }
}
