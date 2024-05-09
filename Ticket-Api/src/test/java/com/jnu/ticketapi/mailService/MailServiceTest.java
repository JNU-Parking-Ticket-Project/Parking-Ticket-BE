//package com.jnu.ticketapi.mailService;
//
//
//import com.jnu.ticketinfrastructure.service.MailService;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//public class MailServiceTest {
//
//    @Autowired private MailService mailService;
//
//    @DisplayName("성공 : 1차신청 결과 메일링 테스트")
//    @Test
//    @WithMockUser(roles = "USER")
//    public void send_email_test() {
//        // given
//        String email = "pon05114@naver.com";
//        String name = "홍길동";
//        String status = "합격";
//        Integer sequence = -1;
//
//        // when
//        boolean result = mailService.sendRegistrationResultMail(email, name, status, sequence);
//
//        // then
//        Assertions.assertTrue(result);
//    }
//}
