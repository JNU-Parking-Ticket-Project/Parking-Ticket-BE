package com.jnu.ticketapi.api.captcha.model.response;


import com.jnu.ticketdomain.domains.captcha.domain.Captcha;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;

@Schema(description = "캡챠 요청 응답")
public record CaptchaResponse(
        @Schema(description = "캡챠 식별자") String captchaCode,
        @Schema(description = "캡챠 이미지 URL") String captchaImageUrl) {
    @Builder
    public CaptchaResponse {}

    @Value("${aws.cloudfront.url}")
    private static String couldFrontUrl;
    private static final String IMAGE_POSTFIX = ".png";

    public static CaptchaResponse of(String code, Captcha captcha) {
        return CaptchaResponse.builder()
                .captchaCode(code)
                .captchaImageUrl(couldFrontUrl + "/" + captcha.getImageName() + IMAGE_POSTFIX)
                .build();
    }
}
