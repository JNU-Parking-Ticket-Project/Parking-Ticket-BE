package com.jnu.ticketinfrastructure.service;

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
public class MailService{


    @Value("${mail-address}")
    private final String mailAddress;
    private final SesAsyncClient sesAsyncClient;
    private final SpringTemplateEngine springTemplateEngine;

    /**
     * 메일을 비동기로 전송한다.
     * TODO - 추후 template 작성 후 파라미터에서 template 삭제할 것
     *
     * @param to - 보낼사람메일 주소. subject - 제목. template - Thyme leaf  로 작성된 html, Context - 내용
     */
    public void sendMail(String to, String subject, String template, Context context){

        String html = springTemplateEngine.process(template, context);

        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(to).build())
                .message(newMessage(subject, html))
                .source(mailAddress)
                .build();

        sesAsyncClient.sendEmail(sendEmailRequest);

    }

    private Message newMessage(String subject, String html) {
        Content content = Content.builder().data(subject).build();
        return Message.builder().subject(content).body(Body.builder().html(builder -> builder.data(html)).build()).build();
    }
}

