package com.jnu.ticketapi.api.user.model.response;

import com.jnu.ticketcommon.message.MailMessage;
import lombok.Builder;

public record FindPasswordResponse(
        String message
) {
    @Builder
    public FindPasswordResponse{}

    public static FindPasswordResponse of(boolean isSend){
        if (isSend){
            return FindPasswordResponse.builder()
                    .message(MailMessage.SUCCESS_SEND_MAIL)
                    .build();
        }
        return FindPasswordResponse.builder()
                .message(MailMessage.FAIL_SEND_MAIL)
                .build();
    }
}
