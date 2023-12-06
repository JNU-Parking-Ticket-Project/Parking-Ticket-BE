package com.jnu.ticketinfrastructure.service;


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

    @Value("${aws.ses.mail-address}")
    private String mailAddress;

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
}
