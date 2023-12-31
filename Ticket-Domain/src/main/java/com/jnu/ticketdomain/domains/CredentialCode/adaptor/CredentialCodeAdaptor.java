package com.jnu.ticketdomain.domains.CredentialCode.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.CredentialCode.domain.CredentialCode;
import com.jnu.ticketdomain.domains.CredentialCode.out.CredentialCodeLoadPort;
import com.jnu.ticketdomain.domains.CredentialCode.out.CredentialCodeRecordPort;
import com.jnu.ticketdomain.domains.CredentialCode.repository.CredentialCodeRepository;
import com.jnu.ticketdomain.domains.user.exception.CredentialCodeNotExistException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CredentialCodeAdaptor implements CredentialCodeRecordPort, CredentialCodeLoadPort {

    private final CredentialCodeRepository credentialCodeRepository;

    @Override
    public CredentialCode saveCode(CredentialCode credentialCode) {
        Optional<CredentialCode> newCode =
                credentialCodeRepository.findByEmail(credentialCode.getEmail());

        if (newCode.isPresent()) {
            newCode.get().updateCode(credentialCode.getCode());
            return newCode.get();
        } else {
            return credentialCodeRepository.save(credentialCode);
        }
    }

    @Override
    public String getEmail(String code) {
        return credentialCodeRepository
                .findByCode(code)
                .orElseThrow(() -> CredentialCodeNotExistException.EXCEPTION)
                .getEmail();
    }
}
