package com.jnu.ticketinfrastructure.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class ChatMessage {
    private String registration;
    private Long userId;
    private Long sectorId;
    private Long eventId;

    @Override
    public String toString() {
        return "ChatMessage{" +
                "registration='" + registration + '\'' +
                ", userId=" + userId +
                ", sectorId=" + sectorId +
                ", eventId=" + eventId +
                '}';
    }
}
