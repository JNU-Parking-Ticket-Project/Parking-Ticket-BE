package com.jnu.ticketdomain.domains.council.domain;


import com.jnu.ticketdomain.domains.user.domain.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

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

    @Column(name = "phone_num", nullable = false)
    private String phoneNum;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public Council(String name, String phoneNum, User user) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.user = user;
    }
}
