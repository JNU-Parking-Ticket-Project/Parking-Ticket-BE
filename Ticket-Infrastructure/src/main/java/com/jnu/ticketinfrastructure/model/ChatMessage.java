package com.jnu.ticketinfrastructure.model;


import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class ChatMessage {
    private String registration;
    private Long userId;
    private Long sectorId;
    private Long eventId;
    private Integer position;
    private UserStatus resultStatus;
    private Integer sequence;
    private Integer remainingAmount;
    private Long journalId;
    private Long admissionEpoch;
    private Integer messageVersion;

    public ChatMessage(String registration, Long userId, Long sectorId, Long eventId) {
        this.registration = registration;
        this.userId = userId;
        this.sectorId = sectorId;
        this.eventId = eventId;
    }

    public ChatMessage(
            String registration,
            Long userId,
            Long sectorId,
            Long eventId,
            Integer position,
            UserStatus resultStatus,
            Integer sequence) {
        this.registration = registration;
        this.userId = userId;
        this.sectorId = sectorId;
        this.eventId = eventId;
        this.position = position;
        this.resultStatus = resultStatus;
        this.sequence = sequence;
    }

    public ChatMessage(
            String registration,
            Long userId,
            Long sectorId,
            Long eventId,
            Integer position,
            UserStatus resultStatus,
            Integer sequence,
            Integer remainingAmount,
            Long journalId,
            Long admissionEpoch) {
        this(
                registration,
                userId,
                sectorId,
                eventId,
                position,
                resultStatus,
                sequence,
                remainingAmount,
                journalId,
                admissionEpoch,
                2);
    }

    public ChatMessage(
            String registration,
            Long userId,
            Long sectorId,
            Long eventId,
            Integer position,
            UserStatus resultStatus,
            Integer sequence,
            Integer remainingAmount,
            Long journalId,
            Long admissionEpoch,
            Integer messageVersion) {
        this.registration = registration;
        this.userId = userId;
        this.sectorId = sectorId;
        this.eventId = eventId;
        this.position = position;
        this.resultStatus = resultStatus;
        this.sequence = sequence;
        this.remainingAmount = remainingAmount;
        this.journalId = journalId;
        this.admissionEpoch = admissionEpoch;
        this.messageVersion = messageVersion;
    }

    public boolean hasDecision() {
        return position != null && resultStatus != null && sequence != null;
    }

    public boolean hasJournalDecision() {
        return Integer.valueOf(2).equals(messageVersion)
                && hasDecision()
                && remainingAmount != null
                && journalId != null
                && admissionEpoch != null;
    }

    public boolean hasJournalMetadata() {
        return messageVersion != null || journalId != null || admissionEpoch != null;
    }

    @Override
    public String toString() {
        return "ChatMessage{"
                + "registration='"
                + registration
                + '\''
                + ", userId="
                + userId
                + ", sectorId="
                + sectorId
                + ", eventId="
                + eventId
                + ", position="
                + position
                + ", resultStatus="
                + resultStatus
                + ", sequence="
                + sequence
                + ", remainingAmount="
                + remainingAmount
                + ", journalId="
                + journalId
                + ", admissionEpoch="
                + admissionEpoch
                + ", messageVersion="
                + messageVersion
                + '}';
    }
}
