package com.jnu.ticketinfrastructure.service;


import com.jnu.ticketcommon.consts.MailTemplate;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import software.amazon.awssdk.services.ses.SesAsyncClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * 비동기 메일 전송을 할 수 있다.
 *
 * @author cookie
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    @Value("${aws.ses.mail-address:centerofjnu@jnu-parking.com}")
    private String mailAddress;

    @Value("${mail.announcement-url}")
    private String announcementUrl;

    private final SesAsyncClient sesAsyncClient;
    private final SpringTemplateEngine htmlTemplateEngine;

    /**
     * 메일을 비동기로 전송한다.
     *
     * @param to - 보낼사람메일 주소. subject - 제목. template - Thyme leaf 로 작성된 html, Context - 내용
     */
    public boolean sendMail(String to, String subject, String template, Context context)
            throws Exception {

        String html = htmlTemplateEngine.process(template, context);

        SendEmailRequest sendEmailRequest =
                SendEmailRequest.builder()
                        .destination(Destination.builder().toAddresses(to).build())
                        .message(newMessage(subject, html))
                        .source(mailAddress)
                        .build();

        CompletableFuture<SendEmailResponse> sendEmailFuture =
                sesAsyncClient.sendEmail(sendEmailRequest);

        SendEmailResponse sendEmailResponse = sendEmailFuture.get(); // 결과를 기다림
        return sendEmailResponse.sdkHttpResponse().isSuccessful();
    }

    private Message newMessage(String subject, String html) {
        Content content = Content.builder().data(subject).build();
        return Message.builder()
                .subject(content)
                .body(Body.builder().html(builder -> builder.data(html)).build())
                .build();
    }

    public boolean sendRegistrationResultMail(
            String email, String name, String status, Integer sequence) {
        try {
            Context context = newRegistrationContext(name, createMailStatus(status, sequence), announcementUrl);
            boolean result =
                    sendMail(
                            email,
                            MailTemplate.REGISTRATION_SUBJECT,
                            MailTemplate.REGISTRATION_TEMPLATE,
                            context);
            log.info("신청 결과 메일 발송. 사용자 이메일 : {}, 결과 : {}", email, result);
            return result;
        } catch (Exception e) {
            log.info("신청 결과 메일 발송 실패. 사용자 이메일 : {}, 에러 로그 : {} ", email, e.getMessage());
            return false;
        }
    }

    public static Context newRegistrationContext(String userName, String pass, String announcementUrl) {
        Context context = new Context();
        context.setVariable(MailTemplate.REGISTRATION_NAME_CONTEXT, userName);
        context.setVariable(MailTemplate.REGISTRATION_PASS_CONTEXT, pass);
        context.setVariable(MailTemplate.REGISTRATION_ANNOUNCEMENT_CONTEXT, announcementUrl);
        return context;
    }

    /**
     * Sequence 의 값을 보고 판단해, 예비인 경우 "예비 1번"과 같이 문자열을 출력한다. 기존과 같이 불합격, 합격인 경우는 동일하게 "불합격"/"합격"으로
     * 출력한다. Sequence == -1 : 합격 Sequence == -2 : 불합격 그 외 : 예비 n번
     *
     * @param status 유저 합/불/예비 여부
     * @param sequence 합/불/예비에 따른 정수
     * @return String type 의 결과 상태.
     */
    private String createMailStatus(String status, Integer sequence) {
        if (sequence >= 0) {
            return status + " " + sequence + "번";
        }
        return status;
    }
}
