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

/**
 * 비밀번호 재설정을 하기 위한 링크를 생성하고, 메일 전송한다.
 * 비밀번호 재설정을 하기 위한 링크는 UUID 클래스를 통해 랜덤 생성한다.
 *
 * @author cookie
 * @version 1.0
 */
@RequiredArgsConstructor
@UseCase
@Slf4j
public class CredentialCodeUseCase {

    private final CredentialCodeAdaptor credentialCodeAdaptor;
    private final MailService mailService;

    /**
     * 비밀번호 재설정 링크 생성하고 이를 포함한 메일을 사용자가 입력한 이메일 주소로 전송한다.
     *
     * @param findPasswordRequest : 비밀번호 재설정을 위한 이메일 문자열이 들어있다.
     * @return FindPasswordResponse : boolean 값을 통해 메일이 전송되었는지 유무를 문자열로 반환한다.
     */
    @Transactional
    public FindPasswordResponse sendMail(FindPasswordRequest findPasswordRequest){
        CredentialCode credentialCode = credentialCodeAdaptor.saveCode(findPasswordRequest
                .toEntity(UUID.randomUUID().toString()));

        Context context = new Context();
        context.setVariable(MailTemplate.FIND_PASSWORD_CONTEXT, MailTemplate.URL + credentialCode.getCode());

        try{
            boolean result = mailService.sendMail(credentialCode.getEmail(), MailTemplate.FIND_PASSWORD_SUBJECT, MailTemplate.FIND_PASSWORD_TEMPLATE, context);
            return FindPasswordResponse.of(result);
        }catch (Exception e){
            log.info("발송 실패 : {}", e.getMessage());
            return FindPasswordResponse.of(false);
        }

    }
}
