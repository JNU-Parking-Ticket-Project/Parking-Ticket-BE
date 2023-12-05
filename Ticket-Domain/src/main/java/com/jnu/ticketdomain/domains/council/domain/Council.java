package com.jnu.ticketdomain.domains.council.domain;


import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Table(name = "council_tb")
public class Council {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "pwd", nullable = false)
    private String pwd;

    @Column(name = "phone_num", nullable = false)
    private String phoneNum;

    @Builder
    public Council(String name, String email, String pwd, String phoneNum) {
        this.name = name;
        this.email = email;
        this.pwd = pwd;
        this.phoneNum = phoneNum;
    }
}
