package com.jnu.ticketapi.application.port;

import javax.mail.MessagingException;
import java.io.IOException;

/**
 * 메일을 전송하기 위한 추상 메서드
 *
 * @author cookie
 * @version 1.0
 */
public interface MailUseCase {

    void sendMail(String sender,
                  String recipient,
                  String subject,
                  String bodyText,
                  String bodyHTML) throws MessagingException, IOException;
}
