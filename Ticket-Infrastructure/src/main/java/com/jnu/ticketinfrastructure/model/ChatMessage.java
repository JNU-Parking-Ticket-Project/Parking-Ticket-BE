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

    public boolean hasDecision() {
        return position != null && resultStatus != null && sequence != null;
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
                + '}';
    }
}
