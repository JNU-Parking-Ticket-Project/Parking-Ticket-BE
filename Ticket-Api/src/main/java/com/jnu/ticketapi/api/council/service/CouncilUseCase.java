package com.jnu.ticketapi.api.council.service;


import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketapi.api.council.model.response.SignUpCouncilResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.council.adaptor.CouncilAdaptor;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.council.exception.AlreadyExistEmailException;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.exception.NotFoundUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CouncilUseCase {
    private final CouncilAdaptor councilAdaptor;
    private final UserAdaptor userAdaptor;

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
}
