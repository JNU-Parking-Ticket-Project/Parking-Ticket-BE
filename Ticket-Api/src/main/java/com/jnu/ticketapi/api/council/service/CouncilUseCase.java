package com.jnu.ticketapi.api.council.service;


import com.jnu.ticketapi.api.council.handler.EmailSendEventHandler;
import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketapi.api.council.model.response.SignUpCouncilResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.council.adaptor.CouncilAdaptor;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.council.exception.AlreadyExistEmailException;
import com.jnu.ticketdomain.domains.events.event.SendEmailEvent;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
import com.jnu.ticketinfrastructure.service.MailService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CouncilUseCase {
    private final CouncilAdaptor councilAdaptor;
    private final UserAdaptor userAdaptor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userAdaptor.findByEmail(email).orElseThrow(() -> NotFoundUserException.EXCEPTION);
    }

    @Transactional
    public SignUpCouncilResponse signUp(SignUpCouncilRequest signUpCouncilRequest) {
        User alreadyExistUser = userAdaptor.findByEmail(signUpCouncilRequest.email()).orElse(null);
        if (alreadyExistUser != null) {
            throw AlreadyExistEmailException.EXCEPTION;
        }
        User user = signUpCouncilRequest.toUserEntity(signUpCouncilRequest);
        User jpaUser = userAdaptor.save(user);
        Council council = signUpCouncilRequest.toCouncilEntity(signUpCouncilRequest, jpaUser);
        councilAdaptor.save(council);
        return SignUpCouncilResponse.of(ResponseMessage.SUCCESS_SIGN_UP);
    }

    @Transactional
    public void sendEmail(Long eventId) {
        try {
            eventPublisher.publishEvent(new SendEmailEvent(eventId)); // 이벤트 객체 발행
            log.info("SendEmailEvent published for eventId: {}", eventId);
        } catch (Exception e) {
            log.error("Failed to publish SendEmailEvent: {}", e.getMessage());
        }
    }
}
