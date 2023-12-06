package com.jnu.ticketdomain.domains.CredentialCode.repository;

import com.jnu.ticketdomain.domains.CredentialCode.domain.CredentialCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialCodeRepository extends JpaRepository<CredentialCode, Long> {

    CredentialCode findByEmail(String email);
    CredentialCode findByCode(String code);
    boolean existsByEmail(String email);
    void deleteByEmail(String email);
}
