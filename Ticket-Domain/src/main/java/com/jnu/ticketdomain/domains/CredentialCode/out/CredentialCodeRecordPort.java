package com.jnu.ticketdomain.domains.CredentialCode.out;


import com.jnu.ticketdomain.domains.CredentialCode.domain.CredentialCode;

public interface CredentialCodeRecordPort {

    CredentialCode saveCode(CredentialCode credentialCode);
}
