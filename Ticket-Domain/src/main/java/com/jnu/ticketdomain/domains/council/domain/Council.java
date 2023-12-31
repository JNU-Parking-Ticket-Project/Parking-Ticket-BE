package com.jnu.ticketdomain.domains.council.domain;


import com.jnu.ticketdomain.domains.user.domain.User;
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

    @Column(name = "phone_num", nullable = false)
    private String phoneNum;

    @Column(name = "student_num", nullable = false)
    private String studentNum;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public Council(String name, String phoneNum, User user, String studentNum) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.user = user;
        this.studentNum = studentNum;
    }
}
