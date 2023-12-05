package com.jnu.ticketapi.api.council.service;


import com.jnu.ticketapi.api.council.model.request.SignUpCouncilRequest;
import com.jnu.ticketapi.api.council.model.response.SignUpCouncilResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.message.ResponseMessage;
import com.jnu.ticketdomain.domains.council.adaptor.CouncilAdaptor;
import com.jnu.ticketdomain.domains.council.domain.Council;
import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CouncilUseCase {
    private final CouncilAdaptor councilAdaptor;

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return councilAdaptor.findByEmail(email);
    }

    @Transactional
    public SignUpCouncilResponse signUp(SignUpCouncilRequest signUpCouncilRequest) {
        User user = signUpCouncilRequest.toUserEntity(signUpCouncilRequest);
        Council council = signUpCouncilRequest.toCouncilEntity(signUpCouncilRequest, user);
        councilAdaptor.saveUser(user);
        councilAdaptor.saveCouncil(council);
        return SignUpCouncilResponse.of(ResponseMessage.SUCCESS_SIGN_UP);
    }
}
