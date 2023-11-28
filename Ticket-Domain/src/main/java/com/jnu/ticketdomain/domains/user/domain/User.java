package com.jnu.ticketdomain.domain.user;


import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Table(name = "user_tb")
@DynamicInsert
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pwd", nullable = false)
    private String pwd;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @ColumnDefault("'USER'")
    private UserRole userRole;
    //TODO: 토의 한 것 기반으로 예비 등수 필드 추가
    @Column(name = "status", nullable = false)
    @ColumnDefault("'불합격'")
    private String status;

    @Column(name = "sequence", nullable = false)
    @ColumnDefault("-2")
    private int sequence;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_confirmed", nullable = false)
    @ColumnDefault("false")
    private boolean emailConfirmed;

    @Builder
    public User(String pwd, String email, UserRole userRole) {
        this.pwd = pwd;
        this.email = email;
        this.userRole = userRole;
    }
}
