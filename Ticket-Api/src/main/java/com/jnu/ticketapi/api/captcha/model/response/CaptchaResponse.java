package com.jnu.ticketapi.api.captcha.model.response;


import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "캡챠 요청 응답")
public record CaptchaResponse(
        @Schema(description = "캡챠 식별자") String captchaCode,
        @Schema(description = "캡챠 이미지 URL") String captchaImageUrl) {
    @Builder
    public CaptchaResponse {}

    public static CaptchaResponse of(
            String cloudFrontUrl, String imagePostfix, String code, Captcha captcha) {
        return CaptchaResponse.builder()
                .captchaCode(code)
                .captchaImageUrl(cloudFrontUrl + "/" + captcha.getImageName() + imagePostfix)
                .build();
    }
}
