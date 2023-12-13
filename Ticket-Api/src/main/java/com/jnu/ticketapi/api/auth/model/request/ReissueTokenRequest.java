package com.jnu.ticketapi.api.auth.model.request;


import com.jnu.ticketcommon.message.ValidationMessage;
import lombok.Builder;

import javax.validation.constraints.NotBlank;

@Builder
public record ReissueTokenRequest(@NotBlank(message = ValidationMessage.IS_NOT_BLANK) String refreshToken) {}
