package com.jnu.ticketapi.api.user.service;


import com.jnu.ticketapi.api.user.model.request.UpdatePasswordRequest;
import com.jnu.ticketapi.api.user.model.response.UpdatePasswordResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketdomain.domains.CredentialCode.adaptor.CredentialCodeAdaptor;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdatePasswordUseCase {

    private final UserAdaptor userAdaptor;
    private final CredentialCodeAdaptor credentialCodeAdaptor;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public UpdatePasswordResponse execute(
            String code, UpdatePasswordRequest updatePasswordRequest) {

        return UpdatePasswordResponse.from(
                userAdaptor.updatePassword(
                        credentialCodeAdaptor.getEmail(code),
                        bCryptPasswordEncoder.encode(updatePasswordRequest.password())));
    }
}
