package com.jnu.ticketdomain.domains.CredentialCode.domain;


import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "credential_code_tb")
public class CredentialCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long credentialCodeId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Builder
    public CredentialCode(String code, String email) {
        this.code = code;
        this.email = email;
    }

    public void updateCode(String code) {
        this.code = code;
    }
}
