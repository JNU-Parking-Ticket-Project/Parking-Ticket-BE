package com.jnu.ticketapi.api.user.service;

import com.jnu.ticketapi.api.user.model.request.FindPasswordRequest;
import com.jnu.ticketapi.api.user.model.response.FindPasswordResponse;
import com.jnu.ticketcommon.annotation.UseCase;
import com.jnu.ticketcommon.consts.MailTemplate;
import com.jnu.ticketdomain.domains.CredentialCode.adaptor.CredentialCodeAdaptor;
import com.jnu.ticketdomain.domains.CredentialCode.domain.CredentialCode;
import com.jnu.ticketinfrastructure.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.util.UUID;

@RequiredArgsConstructor
@UseCase
@Slf4j
public class CredentialCodeUseCase {

    private final CredentialCodeAdaptor credentialCodeAdaptor;
    private final MailService mailService;

    @Transactional
    public FindPasswordResponse sendMail(FindPasswordRequest findPasswordRequest){
        String code = UUID.randomUUID().toString();
        CredentialCode credentialCode = credentialCodeAdaptor.saveCode(findPasswordRequest.toEntity(code));
        Context context = new Context();
        context.setVariable(MailTemplate.FIND_PASSWORD_CONTEXT, credentialCode.getCode());
        boolean result;
        try{
            result = mailService.sendMail(credentialCode.getEmail(), MailTemplate.FIND_PASSWORD_SUBJECT, MailTemplate.FIND_PASSWORD_TEMPLATE, context);
        }catch (Exception e){
            log.info("발송 실패 : {}", e.getMessage());
            return FindPasswordResponse.of(false);
        }
        return FindPasswordResponse.of(result);
    }
}
